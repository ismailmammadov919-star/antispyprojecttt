package com.example.antispy

import android.app.Activity
import android.graphics.Color
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

object TaskSolver {

    fun createSolverTab(activity: Activity): LinearLayout {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 20, 16, 20)
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val title = TextView(activity).apply {
            text = "🤖 Офлайн-решатель задач"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }
        root.addView(title)

        val inputField = EditText(activity).apply {
            hint = "Введите пример или задачу (например: 2 + 2 * 2)"
            setHintTextColor(Color.parseColor("#666666"))
            setTextColor(Color.WHITE)
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.parseColor("#1a1a1a"))
        }
        root.addView(inputField)

        val resultView = TextView(activity).apply {
            text = "Результат появится здесь"
            textSize = 16f
            setTextColor(Color.parseColor("#888888"))
            setPadding(0, 16, 0, 16)
        }

        val solveButton = Button(activity).apply {
            text = "Решить"
            setBackgroundColor(Color.parseColor("#2d5a3d"))
            setTextColor(Color.parseColor("#4ade80"))
            setOnClickListener {
                val text = inputField.text.toString().trim()
                val resultText = if (text.isEmpty()) {
                    "Введите текст задачи!"
                } else {
                    solveTaskLocally(text)
                }
                resultView.text = resultText
                resultView.setTextColor(Color.parseColor("#4ade80"))
            }
        }
        root.addView(solveButton)
        root.addView(resultView)

        return root
    }

    private fun solveTaskLocally(input: String): String {
        return try {
            if (input.contains("+")) {
                val parts = input.split("+")
                val sum = parts.sumOf { it.trim().toDoubleOrNull() ?: 0.0 }
                "Результат сложения: $sum"
            } else if (input.contains("-")) {
                val parts = input.split("-")
                if (parts.size >= 2) {
                    val first = parts[0].trim().toDoubleOrNull() ?: 0.0
                    val second = parts[1].trim().toDoubleOrNull() ?: 0.0
                    "Результат вычитания: ${first - second}"
                } else {
                    "Неверный формат вычитания"
                }
            } else {
                "Анализ текста: задача принята в обработку. Длина: ${input.length} симв."
            }
        } catch (e: Exception) {
            "Ошибка при вычислении: ${e.localizedMessage}"
        }
    }
}
