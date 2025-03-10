package com.example.fitness_app

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(onAuthSuccess: (userId: String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") } // Новое поле для никнейма
    var isSignUp by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Анимация для текста заголовка
        AnimatedContent(
            targetState = isSignUp,
            transitionSpec = {
                fadeIn() + slideInVertically { -40 } with fadeOut() + slideOutVertically { 40 }
            },
            label = "titleAnimation"
        ) { signUpState ->
            Text(
                text = if (signUpState) "Регистрация" else "Вход",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Поле для email
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Поле для пароля
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Плавное появление/исчезновение поля для подтверждения пароля и никнейма
        AnimatedVisibility(
            visible = isSignUp,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.animateContentSize()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Повторите пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Никнейм") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка для входа или регистрации
        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (isSignUp) {
                    if (password != confirmPassword) {
                        Toast.makeText(context, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (nickname.isEmpty()) {
                        Toast.makeText(context, "Введите никнейм", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    registerUser(email, password, nickname) { success, message, userId ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        if (success) {
                            onAuthSuccess(userId.toString())
                        }
                    }
                } else {
                    loginUser(email, password) { success, message, userId ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        if (success) {
                            onAuthSuccess(userId.toString())
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSignUp) "Зарегистрироваться" else "Войти")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Текст для переключения между регистрацией и входом
        Text(
            text = if (isSignUp) "Уже есть аккаунт? Войти" else "Нет аккаунта? Зарегистрироваться",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { isSignUp = !isSignUp }
                .padding(8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

fun registerUser(email: String, password: String, nickname: String, callback: (Boolean, String, String?) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                // Создаем данные пользователя
                val user = hashMapOf(
                    "email" to email,
                    "nickname" to nickname, // Добавляем никнейм
                    "height" to 0.0,
                    "weight" to 0.0,
                    "goal_weight" to 0.0,
                    "daily_step_goal" to 10000,
                    "created_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                // Сохраняем данные в Firestore
                db.collection("users").document(userId).set(user)
                    .addOnSuccessListener {
                        callback(true, "Регистрация успешна", userId)
                    }
                    .addOnFailureListener { e ->
                        callback(false, "Ошибка при сохранении данных пользователя: ${e.message}", null)
                    }
            } else {
                callback(false, "Ошибка регистрации: ${task.exception?.message}", null)
            }
        }
}

fun loginUser(email: String, password: String, callback: (Boolean, String, String?) -> Unit) {
    val auth = FirebaseAuth.getInstance()

    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid
                callback(true, "Успешный вход!", userId)
            } else {
                callback(false, "Ошибка входа: ${task.exception?.message}", null)
            }
        }
}
