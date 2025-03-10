package com.example.fitness_app

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    val db = FirebaseFirestore.getInstance()

    // Получаем текущие данные пользователя из Firestore
    LaunchedEffect(userId) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    height = document.getDouble("height")?.toString() ?: ""
                    weight = document.getDouble("weight")?.toString() ?: ""
                    goalWeight = document.getDouble("goal_weight")?.toString() ?: ""
                    dailyStepGoal = document.getDouble("daily_step_goal")?.toString() ?: ""
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
        // Никнейм
        Text(
            text = nickname,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Поля для изменения параметров
        TextField(
            value = height,
            onValueChange = { height = it },
            label = { Text("Рост (см)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Вес (кг)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = goalWeight,
            onValueChange = { goalWeight = it },
            label = { Text("Цель по весу (кг)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = dailyStepGoal,
            onValueChange = { dailyStepGoal = it },
            label = { Text("Цель по шагам") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Кнопка сохранения изменений
        Button(
            onClick = {
                try {
                    val heightValue = height.toDoubleOrNull()
                    val weightValue = weight.toDoubleOrNull()
                    val goalWeightValue = goalWeight.toDoubleOrNull()
                    val dailyStepGoalValue = dailyStepGoal.toIntOrNull()

                    if (heightValue == null || weightValue == null || goalWeightValue == null || dailyStepGoalValue == null) {
                        Toast.makeText(context, "Пожалуйста, введите корректные данные", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val userData = hashMapOf(
                        "height" to heightValue,
                        "weight" to weightValue,
                        "goal_weight" to goalWeightValue,
                        "daily_step_goal" to dailyStepGoalValue
                    )

                    db.collection("users").document(userId).set(userData, SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(context, "Данные обновлены!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Ошибка обновления данных: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить изменения")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка выхода из аккаунта
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onLogout() // Перенаправляем на экран авторизации
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Выйти из аккаунта")
        }
    }
}