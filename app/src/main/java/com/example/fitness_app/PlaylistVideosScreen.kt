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
import com.example.fitness_app.model.Playlist
import com.example.fitness_app.model.Video
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistVideosScreen(playlistId: String, navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var playlist by remember { mutableStateOf<Playlist?>(null) }
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(playlistId) {
        db.collection("playlists").document(playlistId).get()
            .addOnSuccessListener { doc ->
                val pl = Playlist(
                    id = doc.id,
                    name = doc.getString("name") ?: "Без названия",
                    userId = doc.getString("userId") ?: "",
                    videos = (doc.get("videos") as? List<String>) ?: emptyList()
                )
                playlist = pl
                if (pl.videos.isNotEmpty()) {
                    db.collection("videos")
                        .whereIn("__name__", pl.videos)
                        .get()
                        .addOnSuccessListener { vids ->
                            videos = vids.documents.map { d ->
                                Video(
                                    id = d.id,
                                    title = d.getString("title") ?: "",
                                    description = d.getString("description") ?: "",
                                    userId = d.getString("userId") ?: "",
                                    videoUrl = d.getString("videoUrl") ?: "",
                                    tags = (d.get("tags") as? List<String>) ?: emptyList(),
                                    views = d.getLong("views") ?: 0,
                                    uploadDate = d.getLong("uploadDate") ?: System.currentTimeMillis()
                                )
                            }
                            isLoading = false
                        }
                } else {
                    videos = emptyList()
                    isLoading = false
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name ?: "Плейлист", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (videos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("В плейлисте нет видео", style = MaterialTheme.typography.bodyLarge)
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
                    WorkoutVideoCard(
                        video = video,
                        onClick = { navController.navigate("video_player/${video.id}") },
                        cardWidth = Modifier.fillMaxWidth().let { androidx.compose.ui.unit.Dp.Unspecified }
                    )
                }
            }
        }
    }
} 