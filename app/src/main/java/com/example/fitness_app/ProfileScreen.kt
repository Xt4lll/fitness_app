package com.example.fitness_app

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@Composable
fun ProfileScreen(userId: String, onLogout: () -> Unit, navController: NavController) {
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var goalWeight by remember { mutableStateOf("") }
    var dailyStepGoal by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = nickname, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 32.dp))

        OutlinedTextField(
            value = height,
            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*\$"))) height = it },
            label = { Text("Рост (см)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = weight,
            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*\$"))) weight = it },
            label = { Text("Вес (кг)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = goalWeight,
            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*\$"))) goalWeight = it },
            label = { Text("Цель по весу (кг)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = dailyStepGoal,
            onValueChange = { if (it.matches(Regex("^\\d*\$"))) dailyStepGoal = it },
            label = { Text("Цель по шагам") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val userData = hashMapOf(
                    "height" to height.toDoubleOrNull(),
                    "weight" to weight.toDoubleOrNull(),
                    "goal_weight" to goalWeight.toDoubleOrNull(),
                    "daily_step_goal" to dailyStepGoal.toIntOrNull()
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

        Button(
            onClick = {
                navController.navigate("bmi_calculator")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Рассчитать ИМТ")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                logout(context, onLogout)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Выйти из аккаунта")
        }
    }
}

fun logout(context: Context, onLogout: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Очистка локальных данных (пример: сброс шагов)
    //StepService.resetSteps(context)

    auth.signOut()
    onLogout()
}
