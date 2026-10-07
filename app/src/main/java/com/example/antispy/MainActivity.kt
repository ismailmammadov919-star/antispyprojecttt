package com.example.antispy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import java.util.Random

class MatrixRainView(context: android.content.Context) : View(context) {
    private val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ@#$%&*<>\\/|?".toCharArray()
    private val rand = Random()
    private var drops = FloatArray(0)
    private val paint = Paint().apply {
        color = Color.parseColor("#22c55e")
        textSize = 28f
        isAntiAlias = true
        typeface = android.graphics.Typeface.MONOSPACE
    }
    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#121212")
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cols = (w / 30).coerceAtLeast(1)
        drops = FloatArray(cols) { rand.nextFloat() * -50 }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        for (i in drops.indices) {
            val text = chars[rand.nextInt(chars.size)].toString()
            val x = i * 30f
            val y = drops[i] * 35f

            paint.alpha = 255
            canvas.drawText(text, x, y, paint)

            drops[i] += 0.5f
            if (drops[i] * 35f > height && rand.nextFloat() > 0.975f) {
                drops[i] = 0f
            }
        }
        invalidate()
    }
}

class MainActivity : Activity() {

    private lateinit var frameContent: FrameLayout
    private lateinit var vpnStatusText: TextView
    private lateinit var vpnToggle: Button
    private lateinit var blockedCountText: TextView
    private lateinit var recentListContainer: LinearLayout
    private lateinit var appsList: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private var currentTab = 0

    private val uiUpdater = object : Runnable {
        override fun run() {
            if (currentTab == 0 && ::blockedCountText.isInitialized) {
                updateDynamicVpnUi()
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MusicManager.playBackgroundMusic()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }

        root.addView(createHeader())
        frameContent = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        root.addView(frameContent)
        root.addView(createBottomNav())

        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        ScanJobService.schedule(this)
        showTab(0)
    }

    override fun onResume() {
        super.onResume()
        handler.post(uiUpdater)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(uiUpdater)
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicManager.stopBackgroundMusic()
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

    private fun createBottomNav(): View {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1a1a1a"))
            setPadding(0, 12, 0, 12)
        }

        val tabs = listOf("🛡️ Защита", "📦 Приложения", "⚙️ Настройки", "🤖 Задачи", "🎮 Игры")
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
            3 -> frameContent.addView(TaskSolver.createSolverTab(this))
            4 -> frameContent.addView(createGamesTab())
        }
    }

    private fun createGamesTab(): View {
        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 20, 16, 20)
        }
        container.addView(GamesManager.createRockPaperScissors(this))
        container.addView(GamesManager.createNumberGuessingGame(this))
        scroll.addView(container)
        return scroll
    }

    private fun createVpnTab(): View {
        val outerContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val matrixView = MatrixRainView(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            alpha = 0.25f
        }
        outerContainer.addView(matrixView)

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
        vpnStatusText = TextView(this).apply {
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val desc = TextView(this).apply {
            text = "Рекламу и трекеры не пропускаем"
            textSize = 13f
            setTextColor(Color.parseColor("#888888"))
        }
        vpnToggle = Button(this).apply {
            setPadding(12, 12, 12, 12)
            setOnClickListener { toggleVpn() }
        }

        statusCard.addView(icon)
        statusCard.addView(vpnStatusText)
        statusCard.addView(desc)
        statusCard.addView(vpnToggle)
        container.addView(statusCard)

        val statsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val stat = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a1a"))
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val l = TextView(this).apply { text = "Заблокировано"; textSize = 12f; setTextColor(Color.parseColor("#666666")) }
        blockedCountText = TextView(this).apply {
            textSize = 24f
            setTextColor(Color.parseColor("#4ade80"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        stat.addView(l)
        stat.addView(blockedCountText)
        statsContainer.addView(stat)
        container.addView(statsContainer)

        val title = TextView(this).apply {
            text = "Недавно заблокированные"
            textSize = 14f
            setTextColor(Color.parseColor("#999999"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 20, 0, 12)
        }
        container.addView(title)

        recentListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        container.addView(recentListContainer)

        scroll.addView(container)
        outerContainer.addView(scroll)

        updateDynamicVpnUi()
        return outerContainer
    }

    private fun updateDynamicVpnUi() {
        if (DnsVpnService.isRunning) {
            vpnStatusText.text = "Защита включена"
            vpnStatusText.setTextColor(Color.parseColor("#4ade80"))
            vpnToggle.text = "Выключить"
            vpnToggle.setBackgroundColor(Color.parseColor("#2d5a3d"))
            vpnToggle.setTextColor(Color.parseColor("#4ade80"))
        } else {
            vpnStatusText.text = "Защита отключена"
            vpnStatusText.setTextColor(Color.parseColor("#ef4444"))
            vpnToggle.text = "Включить"
            vpnToggle.setBackgroundColor(Color.parseColor("#4a2d2d"))
            vpnToggle.setTextColor(Color.parseColor("#ef4444"))
        }

        blockedCountText.text = DnsVpnService.blockedCount.get().toString()

        if (::recentListContainer.isInitialized) {
            recentListContainer.removeAllViews()
            val recent = DnsVpnService.recentBlocked()
            if (recent.isEmpty()) {
                val emptyTv = TextView(this).apply {
                    text = "Пока нет заблокированных запросов"
                    textSize = 12f
                    setTextColor(Color.parseColor("#666666"))
                    setPadding(0, 8, 0, 8)
                }
                recentListContainer.addView(emptyTv)
            } else {
                for (domain in recent) {
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
                    val d = TextView(this).apply { text = domain; textSize = 13f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.MONOSPACE }
                    info.addView(d)

                    val badge = TextView(this).apply {
                        text = "реклама"
                        textSize = 11f
                        setTextColor(Color.parseColor("#4ade80"))
                        setBackgroundColor(Color.parseColor("#2a3f2a"))
                        setPadding(8, 4, 8, 4)
                    }

                    item.addView(info)
                    item.addView(badge)
                    recentListContainer.addView(item)
                }
            }
        }
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
            text = "Настройки пока пустые.\nВсе уже включено."
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
        updateDynamicVpnUi()
    }

    private fun startVpn() {
        startService(Intent(this, DnsVpnService::class.java))
        updateDynamicVpnUi()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            startVpn()
        }
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
                text = "📦 ${f.label} (Риск: ${f.score})\n📋 ${f.pkg}\n\n⚠️ Выявленные доступы и угрозы:\n${f.reasons.joinToString("\n")}"
                textSize = 13f
                setTextColor(color)
                setPadding(16, 16, 16, 16)
                setBackgroundColor(Color.parseColor("#1a1a1a"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                val lp = layoutParams as LinearLayout.LayoutParams
                lp.bottomMargin = 12
                layoutParams = lp
                setOnClickListener {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${f.pkg}")))
                }
            }
            appsList.addView(item)
        }
    }
}
