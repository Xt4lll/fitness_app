package com.example.fitness_app

import androidx.compose.ui.graphics.Color

object BMICategories {
    val categories = listOf(
        BMISection(0f, 16f, "Выраженный дефицит массы", Color(0xFF4FC3F7)),
        BMISection(16f, 17f, "Недостаточная масса", Color(0xFF81D4FA)),
        BMISection(17f, 18.5f, "Легкий дефицит массы", Color(0xFF4DB6AC)),
        BMISection(18.5f, 25f, "Норма", Color(0xFF66BB6A)),
        BMISection(25f, 30f, "Избыточная масса", Color(0xFFFFA726)),
        BMISection(30f, 35f, "Ожирение I степени", Color(0xFFFB8C00)),
        BMISection(35f, 40f, "Ожирение II степени", Color(0xFFF4511E)),
        BMISection(40f, 60f, "Ожирение III степени", Color(0xFFD32F2F)),
    )
} 