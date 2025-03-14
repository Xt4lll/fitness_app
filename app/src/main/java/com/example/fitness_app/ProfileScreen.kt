package com.example.fitness_app

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@Composable
fun ProfileScreen(userId: String, onLogout: () -> Unit) {
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var goalWeight by remember { mutableStateOf("") }
    var dailyStepGoal by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    var heightError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    var goalWeightError by remember { mutableStateOf<String?>(null) }
    var dailyStepGoalError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(userId) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    height = document.getDouble("height")?.toString() ?: ""
                    weight = document.getDouble("weight")?.toString() ?: ""
                    goalWeight = document.getDouble("goal_weight")?.toString() ?: ""
                    dailyStepGoal = document.getDouble("daily_step_goal")?.toInt()?.toString() ?: ""
                    nickname = document.getString("nickname") ?: ""
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = nickname,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Поле "Рост"
        OutlinedTextField(
            value = height,
            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*\$"))) height = it },
            label = { Text("Рост (см)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = heightError != null
        )
        heightError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(modifier = Modifier.height(16.dp))

        // Поле "Вес"
        OutlinedTextField(
            value = weight,
            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*\$"))) weight = it },
            label = { Text("Вес (кг)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = weightError != null
        )
        weightError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(modifier = Modifier.height(16.dp))

        // Поле "Цель по весу"
        OutlinedTextField(
            value = goalWeight,
            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*\$"))) goalWeight = it },
            label = { Text("Цель по весу (кг)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = goalWeightError != null
        )
        goalWeightError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(modifier = Modifier.height(16.dp))

        // Поле "Цель по шагам"
        OutlinedTextField(
            value = dailyStepGoal,
            onValueChange = { if (it.matches(Regex("^\\d*\$"))) dailyStepGoal = it },
            label = { Text("Цель по шагам") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = dailyStepGoalError != null
        )
        dailyStepGoalError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(modifier = Modifier.height(32.dp))

        // Кнопка сохранения
        Button(
            onClick = {
                heightError = if (height.isEmpty() || height.toDoubleOrNull() == null || height.toDouble() <= 0) {
                    "Введите корректный рост (больше 0)"
                } else null

                weightError = if (weight.isEmpty() || weight.toDoubleOrNull() == null || weight.toDouble() <= 0) {
                    "Введите корректный вес (больше 0)"
                } else null

                goalWeightError = if (goalWeight.isEmpty() || goalWeight.toDoubleOrNull() == null || goalWeight.toDouble() <= 0) {
                    "Введите корректную цель по весу (больше 0)"
                } else null

                dailyStepGoalError = if (dailyStepGoal.isEmpty() || dailyStepGoal.toIntOrNull() == null || dailyStepGoal.toInt() <= 0) {
                    "Введите корректное количество шагов (целое число больше 0)"
                } else null

                if (heightError != null || weightError != null || goalWeightError != null || dailyStepGoalError != null) {
                    Toast.makeText(context, "Исправьте ошибки перед сохранением", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val userData = hashMapOf(
                    "height" to height.toDouble(),
                    "weight" to weight.toDouble(),
                    "goal_weight" to goalWeight.toDouble(),
                    "daily_step_goal" to dailyStepGoal.toInt()
                )

                db.collection("users").document(userId).set(userData, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(context, "Данные обновлены!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Ошибка обновления данных: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить изменения")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка выхода
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Выйти из аккаунта")
        }
    }
}