package com.example.fitness_app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorVideosScreen(authorId: String, navController: NavController) {
    var videos by remember { mutableStateOf(listOf<Video>()) }
    var isLoading by remember { mutableStateOf(true) }
    var author by remember { mutableStateOf<User?>(null) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(authorId) {
        // Load author info
        db.collection("users")
            .document(authorId)
            .get()
            .addOnSuccessListener { document ->
                author = document.toUser()
            }

        // Load author's videos
        db.collection("videos")
            .whereEqualTo("userId", authorId)
            .addSnapshotListener { snapshot, _ ->
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
                isLoading = false
            }
    }

    @Composable
    fun VideoCard(video: Video, onClick: () -> Unit) {
        var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
        val context = LocalContext.current

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
                .clickable(onClick = onClick)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
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
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Воспроизвести",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                // Video info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${video.views} просмотров",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(author?.nickname ?: "Автор", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Автор карточка
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        val avatarUrl = author?.photoUrl?.let { url ->
                            if (url.contains("?")) "$url&tr=w-64,h-64" else "$url?tr=w-64,h-64"
                        } ?: ""
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = avatarUrl
                            ),
                            contentDescription = "Аватар автора",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = author?.nickname ?: "Загрузка...",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = author?.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Видео
                if (videos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("У автора пока нет видео", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        videos.forEach { video ->
                            VideoCard(
                                video = video,
                                onClick = {
                                    navController.navigate("video_player/${video.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
} 