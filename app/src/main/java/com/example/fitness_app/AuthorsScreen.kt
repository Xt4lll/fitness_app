package com.example.fitness_app

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material3.OutlinedTextField
import androidx.navigation.NavController
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.unit.Dp

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
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    // Загрузка данных
    LaunchedEffect(userId) {
        loadAuthorsData(userId, db, authors, subscriptions, videos, tags, videosByTag, context) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Авторы и видео", 
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                        )
                    ) 
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Поиск
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
                if (searchQuery.isNotBlank()) {
                    // Результаты поиска
                    val filteredAuthors = authors.filter {
                        it.nickname.contains(searchQuery, ignoreCase = true) ||
                        it.email.contains(searchQuery, ignoreCase = true)
                    }
                    val filteredVideos = videos.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredVideos.isNotEmpty()) {
                        Text(
                            text = "Найденные видео",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
                        )
                        VideoList(
                            videos = filteredVideos,
                            screenWidth = screenWidth,
                            onVideoClick = { videoId -> navController.navigate("video_player/$videoId") }
                        )
                    }

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

                    if (filteredVideos.isEmpty() && filteredAuthors.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Ничего не найдено",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    // Видео по тегам
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
                            VideoList(
                                videos = videosByTag[tag] ?: emptyList(),
                                screenWidth = screenWidth,
                                onVideoClick = { videoId -> navController.navigate("video_player/$videoId") }
                            )
                        }
                    }
                    
                    // Список авторов
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
                            Text(
                                "Нет доступных авторов",
                                style = MaterialTheme.typography.bodyLarge
                            )
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
private fun VideoList(
    videos: List<Video>,
    screenWidth: Dp,
    onVideoClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
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
        items(videos) { video ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 })
            ) {
                WorkoutVideoCard(
                    video = video,
                    onClick = { onVideoClick(video.id) },
                    cardWidth = screenWidth * 0.92f
                )
            }
        }
    }
}

private fun loadAuthorsData(
    userId: String,
    db: FirebaseFirestore,
    authors: MutableList<User>,
    subscriptions: MutableState<Set<String>>,
    videos: MutableList<Video>,
    tags: MutableList<String>,
    videosByTag: MutableMap<String, List<Video>>,
    context: android.content.Context,
    onComplete: () -> Unit
) {
    // Загрузка подписок
    db.collection("subscriptions")
        .whereEqualTo("followerId", userId)
        .addSnapshotListener { snapshot, _ ->
            snapshot?.documents?.let {
                subscriptions.value = it.mapNotNull { doc -> doc.getString("authorId") }.toSet()
            }
        }

    // Загрузка видео
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
            
            // Группировка видео по тегам
            tagSet.forEach { tag ->
                videosByTag[tag] = videoList.filter { it.tags.contains(tag) }
            }
        }

    // Загрузка авторов
    db.collection("users")
        .get()
        .addOnSuccessListener { result ->
            authors.clear()
            result.documents.forEach { doc ->
                if (doc.id != userId) authors.add(doc.toUser())
            }
            onComplete()
        }
        .addOnFailureListener {
            onComplete()
            Toast.makeText(context, "Ошибка загрузки авторов", Toast.LENGTH_SHORT).show()
        }
}