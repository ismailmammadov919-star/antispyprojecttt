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
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var frameContent: FrameLayout
    private lateinit var vpnStatus: TextView
    private lateinit var vpnToggle: Button
    private lateinit var appsList: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }

        root.addView(createHeader())
        frameContent = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        root.addView(frameContent)
        root.addView(createBottomNav(root))

        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        ScanJobService.schedule(this)
        showTab(0)
    }

    private fun createHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a1a"))
            setPadding(16, 24, 16, 16)
        }
        val title = TextView(this).apply {
            text = "Защита"
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val subtitle = TextView(this).apply {
            text = "Сетевой фильтр"
            textSize = 13f
            setTextColor(Color.parseColor("#888888"))
        }
        header.addView(title)
        header.addView(subtitle)
        return header
    }

    private fun createBottomNav(root: View): View {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1a1a1a"))
            setPadding(0, 12, 0, 12)
        }

        val tabs = listOf("🛡️ Защита", "📱 Приложения", "⚙️ Настройки")
        for ((i, tab) in tabs.withIndex()) {
            val btn = Button(this).apply {
                text = tab
                setBackgroundColor(Color.TRANSPARENT)
                setTextColor(if (i == 0) Color.parseColor("#4ade80") else Color.parseColor("#666666"))
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { showTab(i) }
            }
            nav.addView(btn)
        }
        return nav
    }

    private fun showTab(tab: Int) {
        currentTab = tab
        frameContent.removeAllViews()
        when (tab) {
            0 -> frameContent.addView(createVpnTab())
            1 -> frameContent.addView(createAppsTab())
            2 -> frameContent.addView(createSettingsTab())
        }
    }

    private fun createVpnTab(): View {
        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 20, 16, 20)
        }

        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1e3a2e"))
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val lp = layoutParams as LinearLayout.LayoutParams
            lp.bottomMargin = 20
            layoutParams = lp
        }

        val icon = TextView(this).apply { text = "🛡️"; textSize = 40f }
        val statusText = TextView(this).apply {
            text = "Защита включена"
            textSize = 16f
            setTextColor(Color.parseColor("#4ade80"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val desc = TextView(this).apply {
            text = "Рекламу и трекеры не пропускаем"
            textSize = 13f
            setTextColor(Color.parseColor("#888888"))
        }
        vpnToggle = Button(this).apply {
            text = "Выключить"
            setBackgroundColor(Color.parseColor("#2d5a3d"))
            setTextColor(Color.parseColor("#4ade80"))
            setPadding(12, 12, 12, 12)
            setOnClickListener { toggleVpn() }
        }

        statusCard.addView(icon)
        statusCard.addView(statusText)
        statusCard.addView(desc)
        statusCard.addView(vpnToggle)
        container.addView(statusCard)

        val statsContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
        for (label to value in listOf("Блокировано" to "1234", "Сегодня" to "47")) {
            val stat = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#1a1a1a"))
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val lp = layoutParams as LinearLayout.LayoutParams
                lp.rightMargin = 6
                layoutParams = lp
            }
            val l = TextView(this).apply { text = label; textSize = 12f; setTextColor(Color.parseColor("#666666")) }
            val v = TextView(this).apply { text = value; textSize = 24f; setTextColor(Color.parseColor("#4ade80")); typeface = android.graphics.Typeface.DEFAULT_BOLD }
            stat.addView(l)
            stat.addView(v)
            statsContainer.addView(stat)
        }
        container.addView(statsContainer)

        val title = TextView(this).apply {
            text = "Недавно заблокированные"
            textSize = 14f
            setTextColor(Color.parseColor("#999999"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 20, 0, 12)
        }
        container.addView(title)

        for ((domain, time, badge) in listOf(
            Triple("ads.google.com", "2 мин назад", "реклама"),
            Triple("analytics.tiktok.com", "5 мин назад", "трекер"),
            Triple("doubleclick.net", "8 мин назад", "реклама"),
            Triple("mc.yandex.ru", "12 мин назад", "аналитика")
        )) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.parseColor("#1a1a1a"))
                setPadding(12, 12, 12, 12)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                val lp = layoutParams as LinearLayout.LayoutParams
                lp.bottomMargin = 8
                layoutParams = lp
            }

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val d = TextView(this).apply { text = domain; textSize = 13f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.MONOSPACE; setPadding(0, 0, 0, 4) }
            val t = TextView(this).apply { text = time; textSize = 11f; setTextColor(Color.parseColor("#666666")) }
            info.addView(d)
            info.addView(t)

            val badge = TextView(this).apply {
                text = badge
                textSize = 11f
                setTextColor(Color.parseColor("#4ade80"))
                setBackgroundColor(Color.parseColor("#2a3f2a"))
                setPadding(8, 4, 8, 4)
            }

            item.addView(info)
            item.addView(badge)
            container.addView(item)
        }

        scroll.addView(container)
        return scroll
    }

    private fun createAppsTab(): View {
        val scroll = ScrollView(this)
        appsList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 20, 16, 20)
        }
        scroll.addView(appsList)
        scanApps()
        return scroll
    }

    private fun createSettingsTab(): View {
        val settings = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(16, 20, 16, 20)
        }
        val text = TextView(this).apply {
            text = "Настройки пока пусты.\nВсё уже включено."
            textSize = 16f
            setTextColor(Color.parseColor("#888888"))
            setPadding(16, 40, 16, 16)
        }
        settings.addView(text)
        return settings
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

    private fun scanApps() {
        appsList.removeAllViews()
        val findings = Scanner.scan(this)

        if (findings.isEmpty()) {
            val msg = TextView(this).apply {
                text = "✅ Подозрительных\nприложений не найдено"
                textSize = 18f
                setTextColor(Color.parseColor("#4ade80"))
                setPadding(16, 40, 16, 16)
            }
            appsList.addView(msg)
            return
        }

        for (f in findings) {
            val color = when {
                f.score >= 60 -> Color.RED
                f.score >= 40 -> Color.rgb(255, 140, 0)
                else -> Color.rgb(200, 170, 0)
            }
            val item = TextView(this).apply {
                text = "${f.label} (${f.score})\n${f.pkg}\n${f.reasons.joinToString("\n")}"
                textSize = 13f
                setTextColor(color)
                setPadding(16, 16, 16, 16)
                setBackgroundColor(Color.parseColor("#1a1a1a"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                val lp = layoutParams as LinearLayout.LayoutParams
                lp.bottomMargin = 12
                layoutParams = lp
                setOnClickListener { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${f.pkg}"))) }
            }
            appsList.addView(item)
        }
    }
}
