package com.example.fitness_app

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import org.json.JSONObject
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CreativeStudioScreen(userId: String, navController: NavController) {
    var videos by remember { mutableStateOf(listOf<Video>()) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(userId) {
        db.collection("videos")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                val videoList = mutableListOf<Video>()
                snapshot?.documents?.forEach { document ->
                    val video = Video(
                        id = document.id,
                        title = document.getString("title") ?: "",
                        description = document.getString("description") ?: "",
                        userId = document.getString("userId") ?: "",
                        videoUrl = document.getString("videoUrl") ?: "",
                        tags = (document.get("tags") as? List<String>) ?: emptyList(),
                        views = document.getLong("views") ?: 0,
                        uploadDate = document.getLong("uploadDate") ?: System.currentTimeMillis()
                    )
                    videoList.add(video)
                }
                videos = videoList.sortedByDescending { it.uploadDate }
            }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_video") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить видео")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Мои видео",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(videos) { video ->
                    VideoCard(video = video, onClick = {
                        navController.navigate("video_player/${video.id}")
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoCard(video: Video, onClick: () -> Unit) {
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

    // Загружаем превью из видео
    LaunchedEffect(video.videoUrl) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(video.videoUrl)
                thumbnail = retriever.getFrameAtTime(0)
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                thumbnail?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Превью видео",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Воспроизвести",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formatDate(video.uploadDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${video.views} просмотров",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (video.tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        video.tags.take(3).forEach { tag ->
                            Surface(
                                modifier = Modifier.padding(vertical = 2.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return formatter.format(date)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddVideoScreen(userId: String, navController: NavController) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }

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
        uri?.let { selectedVideoUri = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить видео") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название видео") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Text(
                text = "Выберите теги:",
                style = MaterialTheme.typography.titleMedium
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                videoTags.forEach { tag ->
                    FilterChip(
                        selected = selectedTags.contains(tag),
                        onClick = {
                            selectedTags = if (selectedTags.contains(tag)) {
                                selectedTags - tag
                            } else {
                                selectedTags + tag
                            }
                        },
                        label = { Text(tag) }
                    )
                }
            }

            Button(
                onClick = { videoPicker.launch("video/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Выбрать видео")
            }

            if (selectedVideoUri != null) {
                Text(
                    text = "Видео выбрано",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = {
                    if (title.text.isNotBlank() && selectedTags.isNotEmpty() && selectedVideoUri != null) {
                        isLoading = true
                        uploadVideoToImageKit(
                            context = context,
                            userId = userId,
                            uri = selectedVideoUri!!,
                            title = title.text,
                            description = description.text,
                            tags = selectedTags.toList(),
                            onComplete = { success, _ ->
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "Видео успешно загружено!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, "Ошибка загрузки видео", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && title.text.isNotBlank() && selectedTags.isNotEmpty() && selectedVideoUri != null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Загрузить видео")
                }
            }
        }
    }
}

private fun uploadVideoToImageKit(
    context: Context,
    userId: String,
    uri: Uri,
    title: String,
    description: String,
    tags: List<String>,
    onComplete: (Boolean, String?) -> Unit
) {
    val uploadUrl = "https://upload.imagekit.io/api/v1/files/upload"
    val privateApiKey = "private_DQx8pRGWDdYj2K04lEm7kGOzD1M="
    val filename = "video_${System.currentTimeMillis()}_$userId"

    val contentResolver = context.contentResolver
    val inputStream = contentResolver.openInputStream(uri)
    val videoBytes = inputStream?.readBytes() ?: run {
        onComplete(false, null)
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

    client.newCall(videoRequest).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Handler(Looper.getMainLooper()).post {
                onComplete(false, null)
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
                        "tags" to tags,
                        "views" to 0,
                        "uploadDate" to System.currentTimeMillis()
                    )

                    db.collection("videos")
                        .add(videoData)
                        .addOnSuccessListener {
                            Handler(Looper.getMainLooper()).post {
                                onComplete(true, videoUrl)
                            }
                        }
                        .addOnFailureListener { e ->
                            Handler(Looper.getMainLooper()).post {
                                onComplete(false, null)
                            }
                        }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        onComplete(false, null)
                    }
                }
            } else {
                Handler(Looper.getMainLooper()).post {
                    onComplete(false, null)
                }
            }
        }
    })
}