package com.example.fitness_app

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class AddVideoViewModel : ViewModel() {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var selectedTags by mutableStateOf(setOf<String>())
    var isLoading by mutableStateOf(false)
    var selectedVideoUri by mutableStateOf<Uri?>(null)
    var uploadSuccess by mutableStateOf<Boolean?>(null)

    fun reset() {
        title = ""
        description = ""
        selectedTags = emptySet()
        isLoading = false
        selectedVideoUri = null
        uploadSuccess = null
    }

    fun uploadVideo(
        context: Context,
        userId: String,
        onComplete: (Boolean) -> Unit
    ) {
        val uri = selectedVideoUri ?: return
        val uploadUrl = "https://upload.imagekit.io/api/v1/files/upload"
        val privateApiKey = "private_DQx8pRGWDdYj2K04lEm7kGOzD1M="
        val filename = "video_${System.currentTimeMillis()}_$userId"

        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val videoBytes = inputStream?.readBytes() ?: run {
            onComplete(false)
            return
        }

        val videoRequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", filename,
                videoBytes.toRequestBody("video/*".toMediaTypeOrNull())
            )
            .addFormDataPart("fileName", filename)
            .addFormDataPart("useUniqueFilename", "true")
            .addFormDataPart("folder", "/videos")
            .build()

        val credential = Credentials.basic(privateApiKey, "")
        val videoRequest = Request.Builder()
            .url(uploadUrl)
            .addHeader("Authorization", credential)
            .post(videoRequestBody)
            .build()

        val client = OkHttpClient()
        isLoading = true
        client.newCall(videoRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    isLoading = false
                    uploadSuccess = false
                    onComplete(false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    val json = JSONObject(bodyString ?: "")
                    val videoUrl = json.optString("url", null)

                    if (videoUrl != null) {
                        val db = FirebaseFirestore.getInstance()
                        val videoData = mapOf(
                            "title" to title,
                            "description" to description,
                            "userId" to userId,
                            "videoUrl" to videoUrl,
                            "tags" to selectedTags.toList(),
                            "views" to 0,
                            "uploadDate" to System.currentTimeMillis()
                        )
                        db.collection("videos")
                            .add(videoData)
                            .addOnSuccessListener {
                                Handler(Looper.getMainLooper()).post {
                                    isLoading = false
                                    uploadSuccess = true
                                    onComplete(true)
                                }
                            }
                            .addOnFailureListener {
                                Handler(Looper.getMainLooper()).post {
                                    isLoading = false
                                    uploadSuccess = false
                                    onComplete(false)
                                }
                            }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            isLoading = false
                            uploadSuccess = false
                            onComplete(false)
                        }
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        isLoading = false
                        uploadSuccess = false
                        onComplete(false)
                    }
                }
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddVideoScreen(userId: String, navController: NavController, viewModel: AddVideoViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = LocalContext.current
    val videoTags = listOf(
        "йога",
        "силовая тренировка",
        "пробежка",
        "жиросжигающая тренировка",
        "тренировка рук",
        "тренировка ног",
        "тренировка спины",
        "кардио-тренировка"
    )
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectedVideoUri = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить видео", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.title,
                    onValueChange = { viewModel.title = it },
                    label = { Text("Название видео") },
                    leadingIcon = {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                OutlinedTextField(
                    value = viewModel.description,
                    onValueChange = { viewModel.description = it },
                    label = { Text("Описание") },
                    leadingIcon = {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Text(
                    text = "Выберите теги:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    videoTags.forEach { tag ->
                        FilterChip(
                            selected = viewModel.selectedTags.contains(tag),
                            onClick = {
                                viewModel.selectedTags = if (viewModel.selectedTags.contains(tag)) {
                                    viewModel.selectedTags - tag
                                } else {
                                    viewModel.selectedTags + tag
                                }
                            },
                            label = { Text(tag, style = MaterialTheme.typography.labelLarge) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Button(
                    onClick = { videoPicker.launch("video/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выбрать видео", style = MaterialTheme.typography.titleMedium)
                }

                AnimatedVisibility(visible = viewModel.selectedVideoUri != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Видео выбрано", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (viewModel.title.isNotBlank() && viewModel.selectedTags.isNotEmpty() && viewModel.selectedVideoUri != null) {
                            viewModel.uploadVideo(context, userId) { success ->
                                if (success) {
                                    Toast.makeText(context, "Видео успешно загружено!", Toast.LENGTH_SHORT).show()
                                    viewModel.reset()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, "Ошибка загрузки видео", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = !viewModel.isLoading && viewModel.title.isNotBlank() && viewModel.selectedTags.isNotEmpty() && viewModel.selectedVideoUri != null
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Загрузить видео", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
} 