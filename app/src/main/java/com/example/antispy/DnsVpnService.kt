package com.example.antispy

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Локальный VPN-фильтр DNS. В туннель направляется только адрес "виртуального DNS-сервера"
 * (10.0.0.1), весь остальной трафик идёт напрямую и никуда не пересылается.
 * DNS-запрос на домен из чёрного списка получает ответ "такого домена нет",
 * остальные запросы пересылаются на обычный DNS (1.1.1.1).
 */
class DnsVpnService : VpnService() {

    private var tun: ParcelFileDescriptor? = null
    @Volatile private var running = false
    private val pool = Executors.newFixedThreadPool(8)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!running) startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        val pfd = Builder()
            .setSession("fellsafewithilens")
            .addAddress(CLIENT_IP, 32)
            .addDnsServer(DNS_IP)
            .addRoute(DNS_IP, 32)
            .setBlocking(true)
            .establish() ?: return
        tun = pfd
        running = true
        isRunning = true
        Thread { loop(pfd) }.start()
    }

    private fun stopVpn() {
        running = false
        isRunning = false
        try { tun?.close() } catch (_: Exception) {}
        tun = null
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        pool.shutdownNow()
        super.onDestroy()
    }

    private fun loop(pfd: ParcelFileDescriptor) {
        val input = FileInputStream(pfd.fileDescriptor)
        val output = FileOutputStream(pfd.fileDescriptor)
        val buf = ByteArray(32767)
        try {
            while (running) {
                val n = input.read(buf)
                if (n <= 0) continue
                val packet = buf.copyOf(n)
                pool.execute { handle(packet, output) }
            }
        } catch (_: Exception) {
        } finally {
            stopVpn()
        }
    }

    private fun handle(p: ByteArray, out: FileOutputStream) {
        try {
            if (p.size < 28 || (p[0].toInt() shr 4) != 4) return        // только IPv4
            val ihl = (p[0].toInt() and 0x0F) * 4
            if (p[9].toInt() != 17) return                              // только UDP
            if (p.size < ihl + 8) return
            val clientPort = u16(p, ihl)
            if (u16(p, ihl + 2) != 53) return                           // только DNS
            val udpLen = u16(p, ihl + 4)
            val dns = p.copyOfRange(ihl + 8, minOf(p.size, ihl + udpLen))

            val q = parseQuestion(dns)
            val reply: ByteArray? = if (q != null && isBlocked(q.first)) {
                record(q.first)
                blockedReply(dns, q.second)
            } else {
                forward(dns)
            }
            if (reply != null) writeUdp(out, p, clientPort, reply)
        } catch (_: Exception) {
        }
    }

    /** Достаёт доменное имя из DNS-запроса. Возвращает (домен, конец раздела вопроса). */
    private fun parseQuestion(d: ByteArray): Pair<String, Int>? {
        if (d.size < 12 || u16(d, 4) < 1) return null
        var i = 12
        val sb = StringBuilder()
        while (true) {
            if (i >= d.size) return null
            val len = d[i].toInt() and 0xFF
            if (len == 0) { i++; break }
            if ((len and 0xC0) != 0) return null
            if (i + 1 + len > d.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(d, i + 1, len, Charsets.US_ASCII))
            i += 1 + len
        }
        val end = i + 4
        if (end > d.size) return null
        return sb.toString().lowercase() to end
    }

    private fun isBlocked(domain: String): Boolean {
        var d = domain
        while (true) {
            if (d in BLOCKLIST) return true
            val dot = d.indexOf('.')
            if (dot < 0) return false
            d = d.substring(dot + 1)
        }
    }

    /** Ответ "домен не существует" (NXDOMAIN). */
    private fun blockedReply(q: ByteArray, qEnd: Int): ByteArray {
        val r = q.copyOf(qEnd)
        r[2] = 0x81.toByte()
        r[3] = 0x83.toByte()
        r[4] = 0; r[5] = 1
        for (i in 6..11) r[i] = 0
        return r
    }

    private fun forward(dns: ByteArray): ByteArray? = try {
        DatagramSocket().use { s ->
            protect(s)
            s.soTimeout = 4000
            s.send(DatagramPacket(dns, dns.size, InetAddress.getByName(UPSTREAM), 53))
            val rb = ByteArray(4096)
            val rp = DatagramPacket(rb, rb.size)
            s.receive(rp)
            rb.copyOf(rp.length)
        }
    } catch (e: Exception) { null }

    private fun writeUdp(out: FileOutputStream, req: ByteArray, clientPort: Int, payload: ByteArray) {
        val total = 20 + 8 + payload.size
        val r = ByteArray(total)
        r[0] = 0x45
        put16(r, 2, total)
        put16(r, 6, 0x4000)
        r[8] = 64
        r[9] = 17
        System.arraycopy(req, 16, r, 12, 4) // отправитель = бывший получатель
        System.arraycopy(req, 12, r, 16, 4) // получатель = бывший отправитель
        put16(r, 10, checksum(r, 20))
        put16(r, 20, 53)
        put16(r, 22, clientPort)
        put16(r, 24, 8 + payload.size)
        System.arraycopy(payload, 0, r, 28, payload.size)
        synchronized(out) { out.write(r) }
    }

    private fun u16(b: ByteArray, i: Int) = ((b[i].toInt() and 0xFF) shl 8) or (b[i + 1].toInt() and 0xFF)

    private fun put16(b: ByteArray, i: Int, v: Int) {
        b[i] = (v shr 8).toByte()
        b[i + 1] = v.toByte()
    }

    private fun checksum(b: ByteArray, len: Int): Int {
        var sum = 0
        var i = 0
        while (i < len) { sum += u16(b, i); i += 2 }
        while ((sum shr 16) != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }

    companion object {
        const val ACTION_STOP = "com.example.antispy.STOP_VPN"
        private const val DNS_IP = "10.0.0.1"
        private const val CLIENT_IP = "10.0.0.2"
        private const val UPSTREAM = "1.1.1.1"

        @Volatile var isRunning = false
        val blockedCount = AtomicInteger(0)
        private val recent = ArrayDeque<String>()

        fun recentBlocked(): List<String> = synchronized(recent) { recent.toList() }

        private fun record(domain: String) {
            blockedCount.incrementAndGet()
            synchronized(recent) {
                recent.remove(domain)
                recent.addFirst(domain)
                while (recent.size > 8) recent.removeLast()
            }
        }

        /** Чёрный список: блокируется сам домен и все его поддомены. Список неполный. */
        val BLOCKLIST = setOf(
            // реклама
            "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
            "2mdn.net", "adnxs.com", "adsrvr.org", "criteo.com", "criteo.net", "taboola.com",
            "outbrain.com", "amazon-adsystem.com", "moatads.com", "smaato.net", "inmobi.com",
            "applovin.com", "ironsrc.com", "mopub.com", "vungle.com", "chartboost.com", "adcolony.com",
            "unityads.unity3d.com", "ads.yahoo.com", "adfox.ru", "ad.mail.ru",
            // аналитика и трекеры
            "google-analytics.com", "scorecardresearch.com", "quantserve.com", "hotjar.com",
            "mixpanel.com", "appsflyer.com", "adjust.com", "flurry.com", "mc.yandex.ru",
            "mc.yandex.com", "an.yandex.ru", "tns-counter.ru", "analytics.tiktok.com",
            "top-fwz1.mail.ru",
            // сайты и серверы известных программ для слежки (stalkerware), список неполный
            "mspy.com", "flexispy.com", "hoverwatch.com", "thetruthspy.com", "spyic.com",
            "cocospy.com", "xnspy.com", "eyezy.com", "umobix.com", "spyera.com", "spyzie.com"
        )
    }
}
