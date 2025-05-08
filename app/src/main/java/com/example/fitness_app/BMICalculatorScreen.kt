package com.example.fitness_app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight


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
            BMITitle()
            BMIResultCard(bmi)
            BMIModernChart(bmi, BMICategories.categories)
            BMIInputForm(
                height = height,
                onHeightChange = { height = it },
                weight = weight,
                onWeightChange = { weight = it },
                gender = gender,
                onGenderSelected = { gender = it },
                age = age,
                onAgeChange = { age = it },
                isValid = isValid.value,
                onCalculate = {
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
                }
            )
            BMILegend()
            BMIInfoCard()
            BackButton(onBack)
        }
    }
}

@Composable
private fun BMITitle() {
    Text(
        "Расчет ИМТ",
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun BMIResultCard(bmi: Float) {
    val currentCategory = BMICategories.categories.firstOrNull {
        bmi >= it.rangeStart && bmi < it.rangeEnd
    }
    val animatedBmi by animateFloatAsState(
        targetValue = bmi,
        animationSpec = tween(durationMillis = 1000)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (bmi > 0f) "%.1f".format(animatedBmi) else "-",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                color = currentCategory?.color ?: colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentCategory?.label ?: "Введите данные",
                style = MaterialTheme.typography.titleMedium,
                color = currentCategory?.color ?: colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BMIInputForm(
    height: String,
    onHeightChange: (String) -> Unit,
    weight: String,
    onWeightChange: (String) -> Unit,
    gender: String,
    onGenderSelected: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    isValid: Boolean,
    onCalculate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = height,
                    onValueChange = onHeightChange,
                    label = { Text("Рост (см)", color = colorScheme.onSurface) },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    isError = height.isNotEmpty() && (height.toFloatOrNull()?.let { it > 0 } == false),
                    singleLine = true
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = onWeightChange,
                    label = { Text("Вес (кг)", color = colorScheme.onSurface) },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    isError = weight.isNotEmpty() && (weight.toFloatOrNull()?.let { it > 0 } == false),
                    singleLine = true
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GenderDropdown(
                    gender = gender,
                    onGenderSelected = onGenderSelected,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = onAgeChange,
                    label = { Text("Возраст", color = colorScheme.onSurface) },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    isError = age.isNotEmpty() && (age.toIntOrNull()?.let { it > 0 } == false),
                    singleLine = true
                )
            }
            Button(
                onClick = onCalculate,
                modifier = Modifier.fillMaxWidth(),
                enabled = isValid,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Рассчитать", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun BMILegend() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Категории ИМТ:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            BMICategories.categories.forEach {
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
}

@Composable
private fun BMIInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
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
}

@Composable
private fun BackButton(onBack: () -> Unit) {
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