package com.example.fitness_app

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.navigation.NavController
import com.google.firebase.firestore.DocumentSnapshot
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import com.imagekit.android.ImageKit
import com.imagekit.android.entity.TransformationPosition
import com.imagekit.android.entity.UploadPolicy
import coil.request.ImageRequest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.asImageBitmap

fun DocumentSnapshot.toUser() = User(
    userId = id,
    email = getString("email") ?: "",
    nickname = getString("nickname") ?: "No name",
    photoUrl = getString("photoUrl"),
    height = getDouble("height"),
    weight = getDouble("weight"),
    goalWeight = getDouble("goal_weight"),
    dailyStepGoal = getLong("daily_step_goal")?.toInt(),
    createdAt = getTimestamp("created_at") ?: com.google.firebase.Timestamp.now()
)

@Composable
fun AuthorsScreen(userId: String, navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val authors = remember { mutableStateListOf<User>() }
    val subscriptions = remember { mutableStateOf(emptySet<String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    val videos = remember { mutableStateListOf<Video>() }
    val tags = remember { mutableStateListOf<String>() }
    val videosByTag = remember { mutableStateMapOf<String, List<Video>>() }

    // Filtered results
    val filteredAuthors by remember(authors, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                authors
            } else {
                authors.filter {
                    it.nickname.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    val filteredVideos by remember(videos, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                emptyList()
            } else {
                videos.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    DisposableEffect(userId) {
        val subscriptionListener = db.collection("subscriptions")
            .whereEqualTo("followerId", userId)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.let {
                    subscriptions.value = it.mapNotNull { doc -> doc.getString("authorId") }.toSet()
                }
            }

        // Load all videos
        db.collection("videos")
            .addSnapshotListener { snapshot, _ ->
                val videoList = mutableListOf<Video>()
                val tagSet = mutableSetOf<String>()
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
                    tagSet.addAll(video.tags)
                }
                videos.clear()
                videos.addAll(videoList.sortedByDescending { it.uploadDate })
                tags.clear()
                tags.addAll(tagSet.sorted())
                
                // Group videos by tag
                tagSet.forEach { tag ->
                    videosByTag[tag] = videoList.filter { it.tags.contains(tag) }
                }
            }

        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                authors.clear()
                result.documents.forEach { doc ->
                    if (doc.id != userId) authors.add(doc.toUser())
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Ошибка загрузки авторов", Toast.LENGTH_SHORT).show()
            }

        onDispose { subscriptionListener.remove() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Поиск авторов и видео") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn {
                // Search results section
                if (searchQuery.isNotBlank()) {
                    // Videos found in search
                    if (filteredVideos.isNotEmpty()) {
                        item {
                            Text(
                                text = "Найденные видео",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        item {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(filteredVideos) { video ->
                                    WorkoutVideoCard(
                                        video = video,
                                        onClick = { navController.navigate("video_player/${video.id}") }
                                    )
                                }
                            }
                        }
                    }

                    // Authors found in search
                    if (filteredAuthors.isNotEmpty()) {
                        item {
                            Text(
                                text = "Найденные авторы",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        items(filteredAuthors) { author ->
                            AuthorItem(
                                author = author,
                                isSubscribed = subscriptions.value.contains(author.userId),
                                onSubscribe = { subscribeToAuthor(userId, author) {} },
                                onUnsubscribe = { unsubscribeFromAuthor(userId, author) {} },
                                onClick = { navController.navigate("author_videos/${author.userId}") }
                            )
                        }
                    }

                    // No results message
                    if (filteredVideos.isEmpty() && filteredAuthors.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Ничего не найдено по запросу '$searchQuery'")
                            }
                        }
                    }
                } else {
                    // Videos by tags section
                    items(tags) { tag ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "$tag",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .clickable { navController.navigate("videos/tag/$tag") },
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(videosByTag[tag] ?: emptyList()) { video ->
                                    WorkoutVideoCard(
                                        video = video,
                                        onClick = { navController.navigate("video_player/${video.id}") }
                                    )
                                }
                            }
                        }
                    }

                    // Authors section (only shown when not searching)
                    item {
                        Text(
                            text = "Авторы",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    if (authors.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Нет доступных авторов")
                            }
                        }
                    } else {
                        items(authors) { author ->
                            AuthorItem(
                                author = author,
                                isSubscribed = subscriptions.value.contains(author.userId),
                                onSubscribe = { subscribeToAuthor(userId, author) {} },
                                onUnsubscribe = { unsubscribeFromAuthor(userId, author) {} },
                                onClick = { navController.navigate("author_videos/${author.userId}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutVideoCard(video: Video, onClick: () -> Unit) {
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

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
            .width(300.dp)
            .height(200.dp)
            .padding(8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            thumbnail?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Превью видео",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } ?: Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
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

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${video.views} просмотров",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AuthorItem(
    author: User,
    isSubscribed: Boolean,
    onSubscribe: () -> Unit,
    onUnsubscribe: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                val avatarUrl = author.photoUrl?.let { url ->
                    if (url.contains("?")) "$url&tr=w-48,h-48" else "$url?tr=w-48,h-48"
                } ?: ""

                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .placeholder(R.drawable.ic_default_avatar)
                            .error(R.drawable.ic_default_avatar)
                            .build()
                    ),
                    contentDescription = "Аватар автора",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = author.nickname,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            if (isSubscribed) {
                OutlinedButton(onClick = onUnsubscribe) { Text("Отписаться") }
            } else {
                Button(onClick = onSubscribe) { Text("Подписаться") }
            }
        }
    }
}

fun subscribeToAuthor(
    followerId: String,
    author: User,
    onSuccess: () -> Unit
) {
    FirebaseFirestore.getInstance().collection("subscriptions")
        .add(
            hashMapOf(
                "followerId" to followerId,
                "authorId" to author.userId,
                "timestamp" to FieldValue.serverTimestamp()
            )
        )
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> Log.e("Subscribe", "Ошибка: ${e.message}") }
}

fun unsubscribeFromAuthor(
    followerId: String,
    author: User, // Принимаем объект User
    onSuccess: () -> Unit
) {
    FirebaseFirestore.getInstance().collection("subscriptions")
        .whereEqualTo("followerId", followerId)
        .whereEqualTo("authorId", author.userId) // Используем author.userId
        .get()
        .addOnSuccessListener { querySnapshot ->
            querySnapshot.documents.forEach { doc ->
                doc.reference.delete()
            }
            onSuccess()
        }
}