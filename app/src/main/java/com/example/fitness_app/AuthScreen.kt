package com.example.fitness_app

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.fitness_app.ui.theme.GreenishCyan
import com.example.fitness_app.ui.theme.Aqua
import com.example.fitness_app.ui.theme.GraanCyan

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(onAuthSuccess: (userId: String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Анимация появления карточки
    val cardScale = remember { Animatable(0.85f) }
    val cardAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        cardScale.animateTo(1f, animationSpec = tween(700))
        cardAlpha.animateTo(1f, animationSpec = tween(700))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .scale(cardScale.value)
                .alpha(cardAlpha.value)
                .shadow(32.dp, RoundedCornerShape(32.dp))
                .background(Color.White, RoundedCornerShape(32.dp))
                .padding(32.dp)
                .widthIn(max = 310.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = isSignUp,
                    transitionSpec = {
                        (fadeIn(tween(400)) + slideInVertically { it }) with
                        (fadeOut(tween(400)) + slideOutVertically { -it })
                    },
                    label = "titleAnimation"
                ) { signUpState ->
                    Text(
                        text = if (signUpState) "Регистрация" else "Вход",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = GreenishCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenishCyan,
                        unfocusedBorderColor = GraanCyan,
                        focusedLabelColor = GreenishCyan,
                        cursorColor = GreenishCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenishCyan,
                        unfocusedBorderColor = GraanCyan,
                        focusedLabelColor = GreenishCyan,
                        cursorColor = GreenishCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(
                    visible = isSignUp,
                    enter = fadeIn(tween(400)) + expandVertically(tween(400)),
                    exit = fadeOut(tween(400)) + shrinkVertically(tween(400)),
                    modifier = Modifier.animateContentSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Повторите пароль") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenishCyan,
                                unfocusedBorderColor = GraanCyan,
                                focusedLabelColor = GreenishCyan,
                                cursorColor = GreenishCyan
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            label = { Text("Никнейм") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenishCyan,
                                unfocusedBorderColor = GraanCyan,
                                focusedLabelColor = GreenishCyan,
                                cursorColor = GreenishCyan
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (email.isEmpty() || password.isEmpty()) {
                            Toast.makeText(context, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password.length < 8) {
                            Toast.makeText(context, "Пароль должен быть не менее 8 символов", Toast.LENGTH_SHORT).show()
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenishCyan,
                        contentColor = Color.White,
                        disabledContainerColor = GraanCyan.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Text(
                        if (isSignUp) "Зарегистрироваться" else "Войти",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isSignUp) "Уже есть аккаунт? Войти" else "Нет аккаунта? Зарегистрироваться",
                    color = Aqua,
                    modifier = Modifier
                        .clickable { isSignUp = !isSignUp }
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

fun registerUser(email: String, password: String, nickname: String, callback: (Boolean, String, String?) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid ?: run {
                    callback(false, "Ошибка: пользователь не найден", null)
                    return@addOnCompleteListener
                }

                val userData = hashMapOf(
                    "email" to email,
                    "nickname" to nickname,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                db.collection("users").document(userId)
                    .set(userData)
                    .addOnSuccessListener {
                        callback(true, "Регистрация успешна", userId)
                    }
                    .addOnFailureListener { e ->
                        callback(false, "Ошибка Firestore: ${e.message}", null)
                    }
            } else {
                callback(false, "Ошибка аутентификации: ${task.exception?.message}", null)
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