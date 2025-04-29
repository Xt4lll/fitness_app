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
                        thumbnailUrl = document.getString("thumbnailUrl") ?: "",
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(author?.nickname ?: "Автор") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
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
        } else if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("У автора пока нет видео")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(videos) { video ->
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