package com.example.antispy

import android.app.Activity
import android.graphics.Color
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object TaskSolver {

    fun createSolverTab(activity: Activity): ScrollView {
        val scroll = ScrollView(activity).apply {
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 20, 16, 20)
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val title = TextView(activity).apply {
            text = "🤖 Умный ИИ-помощник"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 8)
        }
        root.addView(title)

        val helpCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a1a"))
            setPadding(14, 14, 14, 14)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val helpTitle = TextView(activity).apply {
            text = "💡 Как задавать вопросы и команды:"
            textSize = 13f
            setTextColor(Color.parseColor("#4ade80"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 6)
        }
        helpCard.addView(helpTitle)

        val helpText = TextView(activity).apply {
            text = """
                • Математика: введите пример (например: 10 + 5 * 2)
                • География: введите «7 морей» или «океаны»
                • Космос / Наука: введите «черная дыра» или «планеты»
                • Биология: введите «фотосинтез» или «ДНК»
                • История / Физика: введите «законы Ньютона»
                • Общение: «привет», «как дела», «кто ты»
            """.trimIndent()
            textSize = 12f
            setTextColor(Color.parseColor("#aaaaaa"))
        }
        helpCard.addView(helpText)
        root.addView(helpCard)

        val inputField = EditText(activity).apply {
            hint = "Введите ваш вопрос или команду..."
            setHintTextColor(Color.parseColor("#666666"))
            setTextColor(Color.WHITE)
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.parseColor("#1a1a1a"))
        }
        root.addView(inputField)

        val resultView = TextView(activity).apply {
            text = "Ответ ИИ появится здесь..."
            textSize = 14f
            setTextColor(Color.parseColor("#4ade80"))
            setPadding(0, 16, 0, 16)
        }

        val solveButton = Button(activity).apply {
            text = "Отправить запрос"
            setBackgroundColor(Color.parseColor("#2d5a3d"))
            setTextColor(Color.parseColor("#4ade80"))
            setOnClickListener {
                val query = inputField.text.toString().trim()
                val response = if (query.isEmpty()) {
                    "Пожалуйста, введите ваш вопрос."
                } else {
                    generateSmartResponse(query)
                }
                resultView.text = response
            }
        }
        root.addView(solveButton)
        root.addView(resultView)

        scroll.addView(root)
        return scroll
    }

    private fun generateSmartResponse(input: String): String {
        val q = input.lowercase()

        if (q.any { it in "+-*/" } && q.any { it in '0'..'9' }) {
            return try {
                val res = evaluateExpression(q)
                "📐 [Математический расчет]\nРезультат: $q = $res"
            } catch (e: Exception) {
                "❌ Ошибка в математическом выражении."
            }
        }

        return when {
            q.contains("мор") || q.contains("морей") -> 
                "🌍 [География: Моря Мирового океана]\nКрупнейшие и известные моря планеты:\n1. Средиземное море\n2. Карибское море\n3. Берингово море\n4. Охотское море\n5. Красное море\n6. Баренцево море\n7. Желтое море\n(Всего на Земле около 90 морей)."

            q.contains("океан") -> 
                "🌍 [География: Океаны]\nНа Земле выделяют 5 океанов:\n1. Тихий (самый большой)\n2. Атлантический\n3. Индийский\n4. Северный Ледовитый\n5. Южный (омывает Антарктиду)."

            q.contains("космос") || q.contains("планет") || q.contains("солнц") -> 
                "🚀 [Астрономия]\nВ нашей Солнечной системе 8 планет: Меркурий, Венера, Земля, Марс, Юпитер, Сатурн, Уран и Нептун."

            q.contains("черн") && q.contains("дыр") -> 
                "🌌 [Астрофизика]\nЧерная дыра — область пространства-времени с колоссальной гравитацией, которую не может покинуть даже свет."

            q.contains("физик") || q.contains("закон ньютона") -> 
                "⚡ [Физика]\nЗаконы Ньютона: 1-й — инерция, 2-й — F=ma (сила равна массе на ускорение), 3-й — действие равно противодействию."

            q.contains("фотосинтез") -> 
                "🧬 [Биология]\nФотосинтез — образование растениями органических веществ из воды и углекислого газа на свету с выделением кислорода."

            q.contains("днк") -> 
                "🧬 [Биология]\nДНК — макромолекула, хранящая и передающая генетическую информацию организма."

            q.contains("таблиц") || q.contains("хими") -> 
                "🧪 [Химия]\nПериодическая система Д.И. Менделеева классифицирует химические элементы по их свойствам и заряду ядра."

            q.contains("привет") || q.contains("здравствуй") -> 
                "👋 Привет! Я твой карманный офлайн-помощник. Задай мне любой вопрос из подсказок выше!"

            q.contains("как дела") || q.contains("как сам") -> 
                "🤖 Всё отлично, защита активна, база знаний загружена. Чем могу помочь?"

            q.contains("кто ты") -> 
                "🤖 Я встроенный ИИ-ассистент, готовый помочь с учебой и ответами на вопросы прямо в приложении."

            else -> 
                "🤖 [ИИ-Анализ]\nЗапрос «$input» обработан.\n💡 Подсказка: Используйте примеры из памятки сверху (например, напишите «7 морей», «фотосинтез» или введите пример с цифрами)."
        }
    }

    private fun evaluateExpression(expression: String): Double {
        val tokens = expression.replace(" ", "").toCharArray()
        val numbers = mutableListOf<Double>()
        val ops = mutableListOf<Char>()
        
        var i = 0
        while (i < tokens.size) {
            if (tokens[i] in '0'..'9' || tokens[i] == '.') {
                val sb = StringBuilder()
                while (i < tokens.size && (tokens[i] in '0'..'9' || tokens[i] == '.')) {
                    sb.append(tokens[i])
                    i++
                }
                numbers.add(sb.toString().toDouble())
                continue
            } else if (tokens[i] in "+-*/") {
                ops.add(tokens[i])
            }
            i++
        }

        i = 0
        while (i < ops.size) {
            if (ops[i] == '*' || ops[i] == '/') {
                val a = numbers[i]
                val b = numbers[i + 1]
                val res = if (ops[i] == '*') a * b else if (b != 0.0) a / b else 0.0
                numbers[i] = res
                numbers.removeAt(i + 1)
                ops.removeAt(i)
                i--
            }
            i++
        }

        var result = numbers[0]
        for (j in 0 until ops.size) {
            val op = ops[j]
            val nextNum = numbers[j + 1]
            result = if (op == '+') result + nextNum else result - nextNum
        }

        return result
    }
}
