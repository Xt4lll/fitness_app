package com.example.fitness_app

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.NavController
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoPlayerScreen(videoId: String, navController: NavController) {
    var video by remember { mutableStateOf<Video?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showRewind by remember { mutableStateOf(false) }
    var showForward by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // Auto-hide controls after 3 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    // Загрузка данных видео
    LaunchedEffect(videoId) {
        db.collection("videos")
            .document(videoId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    video = Video(
                        id = document.id,
                        title = document.getString("title") ?: "",
                        description = document.getString("description") ?: "",
                        userId = document.getString("userId") ?: "",
                        videoUrl = document.getString("videoUrl") ?: "",
                        thumbnailUrl = document.getString("thumbnailUrl") ?: "",
                        tags = (document.get("tags") as? List<String>) ?: emptyList(),
                        views = document.getLong("views") ?: 0,
                        uploadDate = document.getLong("uploadDate") ?: 0
                    )
                }
            }
    }

    // Инициализация ExoPlayer
    LaunchedEffect(video?.videoUrl) {
        video?.videoUrl?.let { url ->
            val player = ExoPlayer.Builder(context).build()
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = isPlaying
            exoPlayer = player

            player.addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    isPlaying = isPlayingNow
                }
                override fun onEvents(player: androidx.media3.common.Player, events: androidx.media3.common.Player.Events) {
                    currentPosition = player.currentPosition
                    duration = player.duration.takeIf { it > 0 } ?: 0L
                }
            })
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                isPlaying = false
            } else {
                player.play()
                isPlaying = true
            }
        }
    }

    fun seekBy(millis: Long) {
        exoPlayer?.let { player ->
            val newPosition = (player.currentPosition + millis).coerceIn(0, player.duration)
            player.seekTo(newPosition)
            if (millis < 0) {
                showRewind = true
            } else {
                showForward = true
            }
        }
    }

    LaunchedEffect(showRewind) {
        if (showRewind) {
            kotlinx.coroutines.delay(700)
            showRewind = false
        }
    }
    LaunchedEffect(showForward) {
        if (showForward) {
            kotlinx.coroutines.delay(700)
            showForward = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
        }
    }

    fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(video?.title ?: "Воспроизведение видео") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
                    .clickable { showControls = !showControls }
            ) {
                exoPlayer?.let { player ->
                    AndroidView(
                        factory = { context ->
                            PlayerView(context).apply {
                                this.player = player
                                useController = false
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Используем Box для позиционирования rewind/forward
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showRewind) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(80.dp)
                                .background(Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                                Text("10", color = Color.White, fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                            }
                        }
                    }
                    if (showForward) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(80.dp)
                                .background(Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                                Text("10", color = Color.White, fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                            }
                        }
                    }
                }
                // Controls with animation
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    AnimatedVisibility(
                        visible = showControls,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatTime(currentPosition),
                                    color = Color.White,
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    modifier = Modifier.width(48.dp),
                                    textAlign = TextAlign.Center
                                )
                                Slider(
                                    value = if (duration > 0) currentPosition / duration.toFloat() else 0f,
                                    onValueChange = { fraction ->
                                        val newPosition = (fraction * duration).roundToInt().toLong()
                                        exoPlayer?.seekTo(newPosition)
                                        currentPosition = newPosition
                                    },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatTime(duration),
                                    color = Color.White,
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    modifier = Modifier.width(48.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { seekBy(-10_000) }) {
                                    Icon(Icons.Default.FastRewind, contentDescription = "Назад 10 сек", tint = Color.White)
                                }
                                IconButton(onClick = { togglePlayPause() }) {
                                    Icon(
                                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = { seekBy(10_000) }) {
                                    Icon(Icons.Default.FastForward, contentDescription = "Вперёд 10 сек", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Информация о видео
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = video?.title ?: "",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = video?.description ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Просмотров: ${video?.views ?: 0}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    video?.tags?.forEach { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(tag) }
                        )
                    }
                }
            }
        }
    }
}