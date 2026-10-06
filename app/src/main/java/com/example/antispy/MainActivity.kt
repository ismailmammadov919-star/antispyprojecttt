package com.example.antispy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var list: LinearLayout
    private lateinit var vpnBtn: Button
    private lateinit var vpnInfo: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            refreshVpn()
            handler.postDelayed(this, 1500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }

        vpnBtn = Button(this).apply { setOnClickListener { toggleVpn() } }
        vpnInfo = TextView(this).apply { setPadding(0, 8, 0, 24) }
        root.addView(vpnBtn)
        root.addView(vpnInfo)

        root.addView(Button(this).apply {
            text = "Сканировать приложения"
            setOnClickListener { scan() }
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) })
        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        ScanJobService.schedule(this)
        scan()
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    private fun toggleVpn() {
        if (DnsVpnService.isRunning) {
            startService(Intent(this, DnsVpnService::class.java).setAction(DnsVpnService.ACTION_STOP))
        } else {
            val consent = VpnService.prepare(this)
            if (consent != null) startActivityForResult(consent, 100) else startVpn()
        }
    }

    private fun startVpn() {
        startService(Intent(this, DnsVpnService::class.java))
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) startVpn()
    }

    private fun refreshVpn() {
        if (DnsVpnService.isRunning) {
            vpnBtn.text = "Выключить защиту сети"
            val recent = DnsVpnService.recentBlocked()
            vpnInfo.text = "🛡 Защита сети включена\nЗаблокировано запросов: ${DnsVpnService.blockedCount.get()}" +
                    (if (recent.isNotEmpty()) "\nПоследние: " + recent.joinToString(", ") else "")
            vpnInfo.setTextColor(Color.rgb(34, 139, 34))
        } else {
            vpnBtn.text = "Включить защиту сети"
            vpnInfo.text = "Защита сети выключена. Блокирует рекламу, трекеры и серверы известных программ слежки.\n" +
                    "Если не работает, отключите «Частный DNS» в настройках телефона."
            vpnInfo.setTextColor(Color.GRAY)
        }
    }

    private fun scan() {
        list.removeAllViews()
        val findings = Scanner.scan(this)

        if (findings.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "✅ Подозрительных приложений не найдено\nФоновая проверка включена"
                textSize = 18f
                setPadding(0, 32, 0, 0)
            })
            return
        }

        for (f in findings) {
            val color = when {
                f.score >= 60 -> Color.RED
                f.score >= 40 -> Color.rgb(255, 140, 0)
                else -> Color.rgb(200, 170, 0)
            }
            list.addView(TextView(this).apply {
                text = "${f.label}  (риск ${f.score})\n${f.pkg}\n• " +
                        f.reasons.joinToString("\n• ") +
                        "\n\nНажмите, чтобы открыть настройки приложения"
                setTextColor(color)
                setPadding(0, 24, 0, 24)
                setOnClickListener {
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${f.pkg}"))
                    )
                }
            })
        }
    }
}
