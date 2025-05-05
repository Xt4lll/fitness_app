package com.example.fitness_app

import android.widget.ScrollView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape

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
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.times
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.times
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

    Box(modifier = Modifier
        .fillMaxSize()
        .background(colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Расчет ИМТ",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            // Карточка результата BMI
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                val currentCategory = bmiCategories.firstOrNull {
                    bmi >= it.rangeStart && bmi < it.rangeEnd
                }
                val animatedBmi by animateFloatAsState(
                    targetValue = bmi,
                    animationSpec = tween(durationMillis = 1000)
                )
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (bmi > 0f) "%.1f".format(animatedBmi) else "-",
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        color = currentCategory?.color ?: colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentCategory?.label ?: "Введите данные",
                        style = MaterialTheme.typography.titleMedium,
                        color = currentCategory?.color ?: colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            // График BMI
            BMIModernChart(bmi, bmiCategories)

            // Ввод данных
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it },
                            label = { Text("Рост (см)", color = colorScheme.onSurface) },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            isError = height.isNotEmpty() && !heightValid.value,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Вес (кг)", color = colorScheme.onSurface) },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            isError = weight.isNotEmpty() && !weightValid.value,
                            singleLine = true
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GenderDropdown(
                            gender = gender,
                            onGenderSelected = { gender = it },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("Возраст", color = colorScheme.onSurface) },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            isError = age.isNotEmpty() && !ageValid.value,
                            singleLine = true
                        )
                    }
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
                        enabled = isValid.value,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Рассчитать", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // Легенда
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Категории ИМТ:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    bmiCategories.forEach {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp, 12.dp)
                                    .background(it.color, shape = RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${it.rangeStart} - ${it.rangeEnd}: ", fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                            Text(it.label, color = it.color)
                        }
                    }
                }
            }

            // Информационный блок
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Что такое Индекс Массы Тела (ИМТ)?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "ИМТ - показатель соотношения роста и веса человека.",
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "ИМТ = вес (кг) / (рост (м)²)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Примечание: ИМТ не учитывает мышечную массу и может быть неточным для спортсменов и пожилых людей.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Назад", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun BMIModernChart(bmiValue: Float, categories: List<BMISection>) {
    val animatedBmi by animateFloatAsState(
        targetValue = bmiValue,
        animationSpec = tween(durationMillis = 1000)
    )
    // Сохраняем позицию стрелки в пикселях
    var arrowX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val yCenter = size.height / 2
            var startX = 0f
            categories.forEachIndexed { idx, section ->
                val sectionWidth = (section.rangeEnd - section.rangeStart) / 60f * totalWidth
                val isFirst = idx == 0
                val isLast = idx == categories.lastIndex
                val cornerRadius = when {
                    isFirst -> androidx.compose.ui.geometry.CornerRadius(12f, 0f)
                    isLast -> androidx.compose.ui.geometry.CornerRadius(0f, 12f)
                    else -> androidx.compose.ui.geometry.CornerRadius(0f, 0f)
                }
                val topLeft = Offset(startX, yCenter - 18f)
                val sizeRect = androidx.compose.ui.geometry.Size(sectionWidth, 36f)
                drawRoundRect(
                    color = section.color,
                    topLeft = topLeft,
                    size = sizeRect,
                    cornerRadius = cornerRadius,
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )
                startX += sectionWidth
            }
            // Сохраняем позицию стрелки
            arrowX = (animatedBmi.coerceIn(0f, 60f) / 60f) * totalWidth
        }
        // Переводим пиксели в dp для Modifier.offset
        val arrowXdp = with(density) { arrowX.toDp() }
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Текущий ИМТ",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .offset(x = arrowXdp - 12.dp, y = 40.dp)
                .size(24.dp)
                .rotate(180f)
        )
    }
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