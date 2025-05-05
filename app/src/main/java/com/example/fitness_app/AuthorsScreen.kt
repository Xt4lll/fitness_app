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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.ui.unit.Dp

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

@OptIn(ExperimentalMaterial3Api::class)
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
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Авторы и видео", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск авторов и видео") },
                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(16.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Search results section
                if (searchQuery.isNotBlank()) {
                    // Videos found in search
                    if (filteredVideos.isNotEmpty()) {
                        Text(
                            text = "Найденные видео",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
                        )
                        val listState = remember { LazyListState() }
                        val flingBehavior = rememberSnapFlingBehavior(listState)
                        LazyRow(
                            state = listState,
                            flingBehavior = flingBehavior,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            items(filteredVideos) { video ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
                                ) {
                                    WorkoutVideoCard(
                                        video = video,
                                        onClick = { navController.navigate("video_player/${video.id}") },
                                        cardWidth = screenWidth * 0.92f
                                    )
                                }
                            }
                        }
                    }

                    // Authors found in search
                    if (filteredAuthors.isNotEmpty()) {
                        Text(
                            text = "Найденные авторы",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            filteredAuthors.forEach { author ->
                                AuthorItem(
                                    author = author,
                                    isSubscribed = subscriptions.value.contains(author.userId),
                                    onSubscribe = { subscribeToAuthor(userId, author) {} },
                                    onUnsubscribe = { unsubscribeFromAuthor(userId, author) {} },
                                    onClick = { navController.navigate("author_videos/${author.userId}") },
                                    showEmail = false
                                )
                            }
                        }
                    }

                    // No results message
                    if (filteredVideos.isEmpty() && filteredAuthors.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ничего не найдено по запросу '$searchQuery'", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else {
                    // Videos by tags section
                    tags.forEach { tag ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .clickable { navController.navigate("videos/tag/$tag") }
                            )
                            val listState = remember { LazyListState() }
                            val flingBehavior = rememberSnapFlingBehavior(listState)
                            LazyRow(
                                state = listState,
                                flingBehavior = flingBehavior,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                items(videosByTag[tag] ?: emptyList()) { video ->
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
                                    ) {
                                        WorkoutVideoCard(
                                            video = video,
                                            onClick = { navController.navigate("video_player/${video.id}") },
                                            cardWidth = screenWidth * 0.92f
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "Авторы",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
                    )

                    if (authors.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Нет доступных авторов", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 32.dp)
                        ) {
                            authors.forEach { author ->
                                AuthorItem(
                                    author = author,
                                    isSubscribed = subscriptions.value.contains(author.userId),
                                    onSubscribe = { subscribeToAuthor(userId, author) {} },
                                    onUnsubscribe = { unsubscribeFromAuthor(userId, author) {} },
                                    onClick = { navController.navigate("author_videos/${author.userId}") },
                                    showEmail = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutVideoCard(video: Video, onClick: () -> Unit, cardWidth: Dp = 300.dp) {
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
            .width(cardWidth)
            .height(cardWidth * 0.6f)
            .padding(8.dp)
            .clickable(onClick = onClick)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(12.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
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