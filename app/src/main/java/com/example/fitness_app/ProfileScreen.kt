package com.example.fitness_app

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.imagekit.android.ImageKit
import com.imagekit.android.entity.TransformationPosition
import com.imagekit.android.entity.UploadPolicy
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import java.io.File
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@Composable
fun ProfileScreen(userId: String, onLogout: () -> Unit, navController: NavController) {
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var goalWeight by remember { mutableStateOf("") }
    var dailyStepGoal by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var userEmail by remember { mutableStateOf("") }

    val context = LocalContext.current.applicationContext
    val db = FirebaseFirestore.getInstance()
    val authViewModel: AuthViewModel = viewModel()
    val scrollState = rememberScrollState()
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Get user email
    LaunchedEffect(Unit) {
        userEmail = currentUser?.email ?: ""
    }

    // Инициализация ImageKit
    LaunchedEffect(Unit) {
        ImageKit.init(
            context = context,
            publicKey = "public_h842XCc32GFUkOupuWPd6WGOnIA=",
            urlEndpoint = "https://ik.imagekit.io/oi84tpnc3",
            transformationPosition = TransformationPosition.PATH,
            defaultUploadPolicy = UploadPolicy.Builder()
                .requireNetworkType(UploadPolicy.NetworkType.ANY)
                .build()
        )
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isLoading = true
            uploadImageToImageKit(
                context = context,
                userId = userId,
                uri = it,
                onComplete = { success, url ->
                    isLoading = false
                    if (success && url != null) {
                        avatarUrl = url
                        updateUserAvatar(db, userId, url, context)
                    } else {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(context, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    // Загрузка данных пользователя
    LaunchedEffect(userId) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    height = document.getDouble("height")?.toString() ?: ""
                    weight = document.getDouble("weight")?.toString() ?: ""
                    goalWeight = document.getDouble("goal_weight")?.toString() ?: ""
                    dailyStepGoal = document.getLong("daily_step_goal")?.toString() ?: "10000"
                    nickname = document.getString("nickname") ?: ""
                    avatarUrl = document.getString("photoUrl")
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clickable { imagePicker.launch("image/*") }
                    .size(80.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.Center)
                    )
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = avatarUrl,
                            error = painterResource(R.drawable.ic_default_avatar),
                            placeholder = painterResource(R.drawable.ic_default_avatar)
                        ),
                        contentDescription = "Аватар пользователя",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = nickname.ifEmpty { "Без имени" },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = height,
            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*\$"))) height = it },
            label = { Text("Рост (см)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = weight,
            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*\$"))) weight = it },
            label = { Text("Вес (кг)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = goalWeight,
            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*\$"))) goalWeight = it },
            label = { Text("Цель по весу (кг)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = dailyStepGoal,
            onValueChange = { if (it.matches(Regex("^\\d*\$"))) dailyStepGoal = it },
            label = { Text("Цель по шагам") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val userData = hashMapOf(
                    "height" to height.toDoubleOrNull(),
                    "weight" to weight.toDoubleOrNull(),
                    "goal_weight" to goalWeight.toDoubleOrNull(),
                    "daily_step_goal" to dailyStepGoal.toLongOrNull()
                )

                db.collection("users").document(userId)
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(context, "Данные обновлены!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить изменения")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("bmi_calculator") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Рассчитать ИМТ")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("subscriptions") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Мои подписки")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                authViewModel.logout(
                    context = context,
                    onSuccess = {
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    },
                    onFailure = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Выйти из аккаунта")
        }
    }
}

// Функция для загрузки изображения в ImageKit
private fun uploadImageToImageKit(
    context: Context,
    userId: String,
    uri: Uri,
    onComplete: (Boolean, String?) -> Unit
) {
    val publicApiKey = "public_h842XCc32GFUkOupuWPd6WGOnIA="
    val uploadUrl = "https://upload.imagekit.io/api/v1/files/upload"
    val privateApiKey = "private_DQx8pRGWDdYj2K04lEm7kGOzD1M="
    val filename = "avatar_$userId"

    val contentResolver = context.contentResolver
    val inputStream = contentResolver.openInputStream(uri)
    val requestBody = inputStream?.readBytes()?.let { bytes ->
        MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", filename,
                bytes.toRequestBody("image/*".toMediaTypeOrNull())
            )
            .addFormDataPart("fileName", filename)
            .addFormDataPart("useUniqueFilename", "true")
            .addFormDataPart("folder", "/dummy/folder")
            .build()
    } ?: run {
        onComplete(false, null)
        return
    }

    val credential = Credentials.basic(privateApiKey, "")
    val request = Request.Builder()
        .url(uploadUrl)
        .addHeader("Authorization", credential)
        .post(requestBody)
        .build()

    val client = OkHttpClient()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onComplete(false, null)
            }
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                val json = JSONObject(bodyString ?: "")
                val uploadedUrl = json.optString("url", null)

                if (uploadedUrl != null) {
                    // Сохраняем в Firestore
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users").document(userId)
                        .set(mapOf("photoUrl" to uploadedUrl), SetOptions.merge())
                        .addOnSuccessListener {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onComplete(true, uploadedUrl)
                            }
                        }
                        .addOnFailureListener {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onComplete(false, null)
                            }
                        }
                } else {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onComplete(false, null)
                    }
                }
            } else {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onComplete(false, null)
                }
            }
        }
    })
}

// Функция для обновления URL аватара в Firebase
private fun updateUserAvatar(
    db: FirebaseFirestore,
    userId: String,
    imageUrl: String,
    context: Context
) {
    db.collection("users").document(userId)
        .update("photoUrl", imageUrl)
        .addOnSuccessListener {
            Toast.makeText(context, "Аватар обновлен!", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Ошибка обновления: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}