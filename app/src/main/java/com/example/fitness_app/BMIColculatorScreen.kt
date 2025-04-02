package com.example.fitness_app

import android.widget.ScrollView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan



data class BMISection(val rangeStart: Float, val rangeEnd: Float, val label: String, val color: Color)

@Composable
fun BMICalculatorScreen(onBack: () -> Unit) {
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var bmi by remember { mutableStateOf(0f) }

    val heightValid = remember {
        derivedStateOf { height.toFloatOrNull()?.let { it > 0 } ?: false }
    }
    val weightValid = remember {
        derivedStateOf { weight.toFloatOrNull()?.let { it > 0 } ?: false }
    }
    val ageValid = remember {
        derivedStateOf { age.toIntOrNull()?.let { it > 0 } ?: false }
    }
    val genderValid = remember {
        derivedStateOf { gender.isNotEmpty() }
    }

    val isValid = remember {
        derivedStateOf {
            heightValid.value && weightValid.value && ageValid.value && genderValid.value
        }
    }

    val bmiCategories = listOf(
        BMISection(0f, 16f, "Выраженный дефицит массы", Color(0xFF4FC3F7)),
        BMISection(16f, 17f, "Недостаточная масса", Color(0xFF81D4FA)),
        BMISection(17f, 18.5f, "Легкий дефицит массы", Color(0xFF4DB6AC)),
        BMISection(18.5f, 25f, "Норма", Color(0xFF66BB6A)),
        BMISection(25f, 30f, "Избыточная масса", Color(0xFFFFA726)),
        BMISection(30f, 35f, "Ожирение I степени", Color(0xFFFB8C00)),
        BMISection(35f, 40f, "Ожирение II степени", Color(0xFFF4511E)),
        BMISection(40f, 60f, "Ожирение III степени", Color(0xFFD32F2F)),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp)
                .verticalScroll(rememberScrollState()) // ScrollView для прокрутки
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Расчет ИМТ", style = MaterialTheme.typography.headlineMedium)

                Spacer(modifier = Modifier.height(16.dp))

                // Поля ввода
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Рост (см)") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = height.isNotEmpty() && !heightValid.value
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Вес (кг)") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = weight.isNotEmpty() && !weightValid.value
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GenderDropdown(
                        gender = gender,
                        onGenderSelected = { gender = it },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Возраст") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = age.isNotEmpty() && !ageValid.value
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val h = height.toFloat()
                        val w = weight.toFloat()
                        val a = age.toInt()

                        var calculatedBMI = w / ((h / 100) * (h / 100))

                        calculatedBMI += when (gender) {
                            "Мужской" -> 0.5f
                            "Женский" -> -0.5f
                            else -> 0f
                        }

                        calculatedBMI += a * 0.01f
                        bmi = calculatedBMI.coerceIn(0f, 60f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isValid.value
                ) {
                    Text("Рассчитать")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Результат
                val currentCategory = bmiCategories.firstOrNull {
                    bmi >= it.rangeStart && bmi < it.rangeEnd
                }
                Text(
                    text = "%.1f: %s".format(bmi, currentCategory?.label ?: ""),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                BMIChart(bmi, bmiCategories)

                Spacer(modifier = Modifier.height(24.dp))

                // Легенда
                Column {
                    bmiCategories.forEach {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(it.color, shape = MaterialTheme.shapes.small)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${it.rangeStart} - ${it.rangeEnd}: ${it.label}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Информационный блок
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Что такое Индекс Массы Тела (ИМТ)?",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "ИМТ - показатель соотношения роста и веса человека.",
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "ИМТ = вес (кг) / (рост (м)²)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Категории ИМТ:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        listOf(
                            "Менее 16.0" to "Выраженный дефицит массы",
                            "16.0-17.0" to "Недостаточная масса",
                            "17.0-18.5" to "Легкий дефицит массы",
                            "18.5-25.0" to "Нормальный вес",
                            "25.0-30.0" to "Избыточная масса",
                            "30.0-35.0" to "Ожирение I степени",
                            "35.0-40.0" to "Ожирение II степени",
                            "Более 40.0" to "Ожирение III степени"
                        ).forEach { (range, description) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "• $range:",
                                    modifier = Modifier.width(100.dp)
                                )
                                Text(text = description)
                            }
                        }
                        Text(
                            text = "Примечание: ИМТ не учитывает мышечную массу и " +
                                    "может быть неточным для спортсменов и пожилых людей.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Назад")
                }
            }
        }
    }
}


@Composable
fun BMIChart(bmiValue: Float, categories: List<BMISection>) {
    val animatedBmi by animateFloatAsState(
        targetValue = bmiValue,
        animationSpec = tween(durationMillis = 1000)
    )

    Box(modifier = Modifier.height(100.dp).fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val yCenter = size.height / 2

            // Рисуем зоны
            var startX = 0f
            categories.forEach { section ->
                val sectionWidth = (section.rangeEnd - section.rangeStart) / 60f * totalWidth
                drawRect(
                    color = section.color,
                    topLeft = Offset(startX, yCenter - 20f),
                    size = androidx.compose.ui.geometry.Size(sectionWidth, 40f)
                )
                startX += sectionWidth
            }

            // Рисуем стрелку
            val bmiX = (animatedBmi.coerceIn(0f, 60f) / 60f) * totalWidth
            drawLine(
                color = Color.Black,
                start = Offset(bmiX, yCenter + 25f),
                end = Offset(bmiX, yCenter + 45f),
                strokeWidth = 6f
            )
            drawTriangle(bmiX, yCenter + 25f)
        }
    }
}


fun DrawScope.drawTriangle(x: Float, y: Float) {
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(x, y)
            lineTo(x - 10, y + 20)
            lineTo(x + 10, y + 20)
            close()
        },
        color = Color.Black
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderDropdown(
    gender: String,
    onGenderSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = gender,
            onValueChange = {},
            label = { Text("Пол") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Мужской") },
                onClick = {
                    onGenderSelected("Мужской")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Женский") },
                onClick = {
                    onGenderSelected("Женский")
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun BMIGauge(bmi: Float) {
    Box(modifier = Modifier.size(200.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Цветовые зоны
            drawArc(color = Color.Blue, startAngle = 180f, sweepAngle = 30f, useCenter = false, style = Stroke(40f))
            drawArc(color = Color.Green, startAngle = 210f, sweepAngle = 30f, useCenter = false, style = Stroke(40f))
            drawArc(color = Color.Yellow, startAngle = 240f, sweepAngle = 30f, useCenter = false, style = Stroke(40f))
            drawArc(color = Color.Red, startAngle = 270f, sweepAngle = 30f, useCenter = false, style = Stroke(40f))

            // Стрелка
            val angle = 180f + ((bmi.coerceIn(16f, 40f) - 16f) / (40f - 16f)) * 120f
            val radians = Math.toRadians(angle.toDouble())
            val centerX = size.width / 2
            val centerY = size.height
            val needleLength = size.minDimension / 2.2f

            drawLine(
                color = Color.Black,
                start = Offset(centerX, centerY),
                end = Offset(
                    centerX + needleLength * kotlin.math.cos(radians).toFloat(),
                    centerY + needleLength * kotlin.math.sin(radians).toFloat()
                ),
                strokeWidth = 8f
            )
        }
    }
}

