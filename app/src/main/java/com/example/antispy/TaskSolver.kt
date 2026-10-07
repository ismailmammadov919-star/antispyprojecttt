package com.example.antispy

import android.app.Activity
import android.graphics.Color
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object TaskSolver {

    fun createSolverTab(activity: Activity): LinearLayout {
        val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 20, 16, 20) }

        val title = TextView(activity).apply { text = "🤖 Решатель задач"; textSize = 20f; setTextColor(Color.WHITE); setPadding(0, 0, 0, 12) }
        root.addView(title)

        val subjects = listOf("Математика", "Биология", "Русский язык", "География")
        val subjectButtons = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        var selectedSubject = 0
        for ((i, subject) in subjects.withIndex()) {
            val btn = Button(activity).apply {
                text = subject
                setBackgroundColor(if (i == 0) Color.parseColor("#2d5a3d") else Color.parseColor("#1a1a1a"))
                setTextColor(if (i == 0) Color.parseColor("#4ade80") else Color.parseColor("#666666"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val lp = layoutParams as LinearLayout.LayoutParams
                lp.rightMargin = 4
                layoutParams = lp
                setOnClickListener {
                    selectedSubject = i
                    (subjectButtons.getChildAt(selectedSubject) as Button).setBackgroundColor(Color.parseColor("#2d5a3d"))
                    (subjectButtons.getChildAt(selectedSubject) as Button).setTextColor(Color.parseColor("#4ade80"))
                    for (j in subjects.indices) {
                        if (j != i) {
                            (subjectButtons.getChildAt(j) as Button).setBackgroundColor(Color.parseColor("#1a1a1a"))
                            (subjectButtons.getChildAt(j) as Button).setTextColor(Color.parseColor("#666666"))
                        }
                    }
                }
            }
            subjectButtons.addView(btn)
        }
        root.addView(subjectButtons)

        val inputLabel = TextView(activity).apply { text = "Введите задачу:"; textSize = 12f; setTextColor(Color.parseColor("#888888")); setPadding(0, 16, 0, 8) }
        root.addView(inputLabel)

        val input = EditText(activity).apply {
            hint = "Ваша задача..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#666666"))
            setBackgroundColor(Color.parseColor("#1a1a1a"))
            setPadding(12, 12, 12, 12)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 120)
        }
        root.addView(input)

        val solve = Button(activity).apply {
            text = "Решить"
            setBackgroundColor(Color.parseColor("#2d5a3d"))
            setTextColor(Color.parseColor("#4ade80"))
            setPadding(16, 12, 16, 12)
            setOnClickListener {
                val task = input.text.toString()
                if (task.isNotBlank()) {
                    val solution = solveProblem(task, selectedSubject)
                    result.text = solution
                }
            }
        }
        root.addView(solve)

        val result = TextView(activity).apply {
            text = "Ответ появится здесь"
            textSize = 13f
            setTextColor(Color.parseColor("#e0e0e0"))
            setBackgroundColor(Color.parseColor("#1a1a1a"))
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val lp = layoutParams as LinearLayout.LayoutParams
            lp.topMargin = 16
            layoutParams = lp
        }
        root.addView(result)
        return root
    }

    private fun solveProblem(task: String, subject: Int): String {
        val t = task.lowercase()
        return when (subject) {
            0 -> solveMath(t)
            1 -> solveBiology(t)
            2 -> solveRussian(t)
            3 -> solveGeography(t)
            else -> "Неизвестный предмет"
        }
    }

    private fun solveMath(task: String): String = when {
        "2+2" in t -> "Ответ: 4"
        "пифагор" in task -> "Теорема Пифагора: в прямоугольном треугольнике квадрат гипотенузы равен сумме квадратов катетов. c² = a² + b²"
        "окружность" in task -> "Длина окружности: L = 2πr\nПлощадь круга: S = πr²"
        "производн" in task -> "Производная — это скорость изменения функции.\nПримеры: (x²)' = 2x, (sin x)' = cos x"
        "логарифм" in task -> "Логарифм: log_a(b) = c означает a^c = b\nОсновное свойство: log_a(x*y) = log_a(x) + log_a(y)"
        else -> "Попробуйте спросить про: пифагора, окружность, производные, логарифмы"
    }

    private fun solveBiology(task: String): String = when {
        "фотосинтез" in task -> "Фотосинтез: 6CO₂ + 6H₂O + свет → C₆H₁₂O₆ + 6O₂\nПроцесс, где растения преобразуют свет в химическую энергию"
        "днк" in task || "ДНК" in task -> "ДНК состоит из двух цепей нуклеотидов с основаниями: аденин (А), тимин (Т), гуанин (Г), цитозин (Ц)"
        "клетка" in task -> "Клетка — базовая единица жизни. Содержит: ядро (у эукариотов), цитоплазму, митохондрии, рибосомы"
        "мейоз" in task || "митоз" in task -> "Митоз — деление для роста (2n → 2n)\nМейоз — деление для размножения (2n → n)"
        "эволюция" in task -> "Эволюция — процесс изменения видов во времени через естественный отбор"
        else -> "Попробуйте спросить про: фотосинтез, ДНК, клетку, мейоз, эволюцию"
    }

    private fun solveRussian(task: String): String = when {
        "прилаг" in task -> "Прилагательное — часть речи, обозначающая признак предмета. Отвечает на вопросы: какой? какая? какое?"
        "существ" in task -> "Существительное — часть речи, обозначающая предмет. Бывает одушевленное и неодушевленное"
        "глагол" in task -> "Глагол — часть речи, обозначающая действие. Может быть совершенного или несовершенного вида"
        "пунктуация" in task -> "Знаки пунктуации: . , ; : ! ? - \"\" ( )\nЗапятая разделяет однородные члены предложения"
        "ударение" in task -> "Ударение — выделение слога голосом. В русском языке может быть на любом слоге"
        else -> "Попробуйте спросить про: прилагательное, существительное, глагол, пунктуацию, ударение"
    }

    private fun solveGeography(task: String): String = when {
        "столица" in task && "россии" in task -> "Столица России: Москва"
        "столица" in task && "украин" in task -> "Столица Украины: Киев (Kyiv)"
        "столица" in task && "франц" in task -> "Столица Франции: Париж"
        "экватор" in task -> "Экватор — воображаемая линия на Земле, делящая её на Северное и Южное полушария. Длина: ~40 075 км"
        "климат" in task -> "Климат — долгосрочные характеристики погоды региона (температура, осадки, влажность)"
        "материк" in task || "континент" in task -> "На Земле 6 материков: Африка, Америка (С и Ю), Евразия, Океания, Антарктида"
        else -> "Попробуйте спросить про: столицы, экватор, климат, материки"
    }

    private val t: String = ""
}
