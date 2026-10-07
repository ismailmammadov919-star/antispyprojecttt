package com.example.antispy

import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.random.Random

object GamesManager {

    fun createRockPaperScissors(activity: Activity): LinearLayout {
        val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 20, 16, 20) }
        val title = TextView(activity).apply { text = "✂️ Камень-Ножницы-Бумага"; textSize = 20f; setTextColor(Color.WHITE); setPadding(0, 0, 0, 16) }
        val result = TextView(activity).apply { text = "Выберите свой ход"; textSize = 14f; setTextColor(Color.parseColor("#888888")); setPadding(0, 16, 0, 16) }
        root.addView(title)

        val info = TextView(activity).apply { text = "Компьютер сыграет сам\nПобеда = +1 очко"; textSize = 12f; setTextColor(Color.parseColor("#666666")); setPadding(16, 8, 16, 8); setBackgroundColor(Color.parseColor("#1a1a1a")) }
        root.addView(info)
        root.addView(result)

        var playerWins = 0
        val moves = listOf("✊ Камень", "✌️ Ножницы", "✋ Бумага")
        val buttons = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }

        for ((i, move) in moves.withIndex()) {
            val btn = Button(activity).apply {
                text = move
                setBackgroundColor(Color.parseColor("#2d5a3d"))
                setTextColor(Color.parseColor("#4ade80"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val lp = layoutParams as LinearLayout.LayoutParams
                lp.rightMargin = 8
                layoutParams = lp
                setOnClickListener {
                    val computerMove = Random.nextInt(3)
                    val winner = when {
                        i == computerMove -> "Ничья!"
                        (i == 0 && computerMove == 1) || (i == 1 && computerMove == 2) || (i == 2 && computerMove == 0) -> {
                            playerWins++
                            "Вы победили! 🎉"
                        }
                        else -> "Компьютер победил 🤖"
                    }
                    result.text = "Вы: $move\nКомпьютер: ${moves[computerMove]}\n$winner\nВсего побед: $playerWins"
                }
            }
            buttons.addView(btn)
        }
        root.addView(buttons)
        return root
    }

    fun createNumberGuessingGame(activity: Activity): LinearLayout {
        val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 20, 16, 20) }
        val title = TextView(activity).apply { text = "🎯 Угадай число"; textSize = 20f; setTextColor(Color.WHITE); setPadding(0, 0, 0, 16) }
        root.addView(title)

        val info = TextView(activity).apply {
            text = "Компьютер загадал число от 1 до 100.\nВы должны угадать."
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            setPadding(16, 8, 16, 8)
            setBackgroundColor(Color.parseColor("#1a1a1a"))
        }
        root.addView(info)

        var secret = Random.nextInt(1, 101)
        var attempts = 0
        val feedback = TextView(activity).apply { text = "Введите число и нажмите кнопку"; textSize = 14f; setTextColor(Color.parseColor("#888888")); setPadding(0, 16, 0, 16) }
        root.addView(feedback)

        val inputContainer = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
        val input = android.widget.EditText(activity).apply {
            hint = "Число (1-100)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btn = Button(activity).apply {
            text = "Проверить"
            setBackgroundColor(Color.parseColor("#2d5a3d"))
            setTextColor(Color.parseColor("#4ade80"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val lp = layoutParams as LinearLayout.LayoutParams
            lp.leftMargin = 8
            layoutParams = lp
            setOnClickListener {
                val guess = input.text.toString().toIntOrNull() ?: return@setOnClickListener
                attempts++
                feedback.text = when {
                    guess == secret -> {
                        input.isEnabled = false
                        isEnabled = false
                        "🎉 Правильно за $attempts попыток! Число: $secret"
                    }
                    guess < secret -> "Загаданное число БОЛЬШЕ ⬆️"
                    else -> "Загаданное число МЕНЬШЕ ⬇️"
                }
                input.text.clear()
            }
        }
        val resetBtn = Button(activity).apply {
            text = "Заново"
            setBackgroundColor(Color.parseColor("#5a3d2d"))
            setTextColor(Color.parseColor("#e0a080"))
            setOnClickListener {
                secret = Random.nextInt(1, 101)
                attempts = 0
                input.isEnabled = true
                btn.isEnabled = true
                feedback.text = "Введите число и нажмите кнопку"
                input.text.clear()
            }
        }
        inputContainer.addView(input)
        inputContainer.addView(btn)
        root.addView(inputContainer)
        root.addView(resetBtn)
        return root
    }
}
