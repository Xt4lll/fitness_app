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
import com.example.fitness_app.model.Video
import com.google.firebase.firestore.DocumentSnapshot

@Composable
fun TagVideosScreen(tag: String, navController: NavController) {
    var videos by remember { mutableStateOf(listOf<Video>()) }
    var isLoading by remember { mutableStateOf(true) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(tag) {
        loadVideosByTag(db, tag) { videoList ->
            videos = videoList
            isLoading = false
        }
    }

    Scaffold(
        topBar = { TagVideosTopBar(tag, navController) }
    ) { padding ->
        TagVideosContent(
            modifier = Modifier.padding(padding),
            isLoading = isLoading,
            videos = videos,
            tag = tag,
            onVideoClick = { videoId ->
                navController.navigate("video_player/$videoId")
            }
        )
    }
}

private fun loadVideosByTag(
    db: FirebaseFirestore,
    tag: String,
    onVideosLoaded: (List<Video>) -> Unit
) {
    db.collection("videos")
        .whereArrayContains("tags", tag)
        .addSnapshotListener { snapshot, _ ->
            val videoList = snapshot?.documents?.mapNotNull { document ->
                document.toVideo()
            } ?: emptyList()
            onVideosLoaded(videoList.sortedByDescending { it.uploadDate })
        }
}

private fun DocumentSnapshot.toVideo(): Video? {
    return try {
        Video(
            id = id,
            title = getString("title") ?: "",
            description = getString("description") ?: "",
            userId = getString("userId") ?: "",
            videoUrl = getString("videoUrl") ?: "",
            tags = (get("tags") as? List<String>) ?: emptyList(),
            views = getLong("views") ?: 0,
            uploadDate = getLong("uploadDate") ?: System.currentTimeMillis()
        )
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagVideosTopBar(tag: String, navController: NavController) {
    TopAppBar(
        title = { Text("Видео по тегу: #$tag") },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
        }
    )
}

@Composable
private fun TagVideosContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    videos: List<Video>,
    tag: String,
    onVideoClick: (String) -> Unit
) {
    when {
        isLoading -> LoadingContent(modifier)
        videos.isEmpty() -> EmptyContent(modifier, tag)
        else -> VideoList(modifier, videos, onVideoClick)
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(modifier: Modifier, tag: String) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Нет видео с тегом #$tag")
    }
}

@Composable
private fun VideoList(
    modifier: Modifier,
    videos: List<Video>,
    onVideoClick: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(videos) { video ->
            AnimatedVideoCard(
                video = video,
                onClick = { onVideoClick(video.id) }
            )
        }
    }
}

