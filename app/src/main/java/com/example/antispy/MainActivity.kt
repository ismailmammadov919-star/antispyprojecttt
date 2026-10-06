package com.example.antispy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        root.addView(Button(this).apply {
            text = "Сканировать"
            setOnClickListener { scan() }
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) })
        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        ScanJobService.schedule(this) // фоновая проверка каждые 15 минут
        scan()
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
