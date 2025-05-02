package com.example.fitness_app

import android.net.Uri
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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
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
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.NavController
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.Player
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import android.content.pm.ActivityInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoPlayerScreen(videoId: String, navController: NavController) {
    var video by remember { mutableStateOf<Video?>(null) }
    var author by remember { mutableStateOf<User?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showRewind by remember { mutableStateOf(false) }
    var showForward by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? androidx.appcompat.app.AppCompatActivity
    val db = FirebaseFirestore.getInstance()
    val autoHideControlsDelay = 3000L // 3 секунды

    // Загружаем данные видео и автора
    LaunchedEffect(videoId) {
        db.collection("videos")
            .document(videoId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userId = document.getString("userId") ?: ""
                    video = Video(
                        id = document.id,
                        title = document.getString("title") ?: "",
                        description = document.getString("description") ?: "",
                        userId = userId,
                        videoUrl = document.getString("videoUrl") ?: "",
                        tags = (document.get("tags") as? List<String>) ?: emptyList(),
                        views = document.getLong("views") ?: 0,
                        uploadDate = document.getLong("uploadDate") ?: 0
                    )

                    // Загружаем данные автора
                    db.collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                author = User(
                                    userId = userDoc.id,
                                    email = userDoc.getString("email") ?: "",
                                    nickname = userDoc.getString("nickname") ?: "",
                                    photoUrl = userDoc.getString("photoUrl"),
                                    height = userDoc.getDouble("height"),
                                    weight = userDoc.getDouble("weight"),
                                    goalWeight = userDoc.getDouble("goal_weight"),
                                    dailyStepGoal = userDoc.getLong("daily_step_goal")?.toInt(),
                                    createdAt = userDoc.getTimestamp("created_at") ?: com.google.firebase.Timestamp.now()
                                )
                            }
                        }
                }
            }
    }

    // Инициализация ExoPlayer и обновление просмотров при первом воспроизведении
    LaunchedEffect(video?.videoUrl) {
        video?.videoUrl?.let { url ->
            val player = ExoPlayer.Builder(context).build()
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = isPlaying
            exoPlayer = player

            var viewUpdated = false
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY && !viewUpdated) {
                        // Обновляем просмотры только при первом начале воспроизведения
                        viewUpdated = true
                        video?.let { currentVideo ->
                            val currentViews = currentVideo.views
                            val updatedViews = currentViews + 1
                            
                            db.collection("videos")
                                .document(videoId)
                                .update("views", updatedViews)
                                .addOnSuccessListener {
                                    video = currentVideo.copy(views = updatedViews)
                                    println("DEBUG: Views updated in Firebase to $updatedViews")
                                }
                                .addOnFailureListener { e ->
                                    println("DEBUG: Failed to update views in Firebase: ${e.message}")
                                }
                        }
                    }
                    if (playbackState == Player.STATE_READY) {
                        duration = player.duration
                    }
                }

                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    isPlaying = isPlayingNow
                }
            })
        }
    }

    // Отдельный эффект для отслеживания позиции видео
    LaunchedEffect(exoPlayer, isPlaying) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            if (isPlaying) {
                currentPosition = player.currentPosition
            }
            delay(16) // Примерно 60 fps для плавного обновления
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
            delay(700)
            showRewind = false
        }
    }
    LaunchedEffect(showForward) {
        if (showForward) {
            delay(700)
            showForward = false
        }
    }

    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Автоматическое скрытие контролов в полноэкранном режиме
    LaunchedEffect(showControls, isFullscreen, isPlaying) {
        if (showControls && isFullscreen && isPlaying) {
            delay(autoHideControlsDelay)
            showControls = false
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
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text(video?.title ?: "Воспроизведение видео") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isFullscreen) Modifier else Modifier.verticalScroll(rememberScrollState()))
                .padding(if (isFullscreen) PaddingValues(0.dp) else padding)
        ) {
            Box(
                modifier = if (isFullscreen)
                    Modifier
                        .fillMaxSize()
                        .clickable { showControls = !showControls }
                else
                    Modifier
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
                                Icon(imageVector = Icons.Default.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
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
                                Icon(imageVector = Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
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
                                    Icon(imageVector = Icons.Default.FastRewind, contentDescription = "Назад 10 сек", tint = Color.White)
                                }
                                IconButton(onClick = { togglePlayPause() }) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = { seekBy(10_000) }) {
                                    Icon(imageVector = Icons.Default.FastForward, contentDescription = "Вперёд 10 сек", tint = Color.White)
                                }
                                IconButton(onClick = { toggleFullscreen() }) {
                                    Icon(
                                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = if (isFullscreen) "Выйти из полноэкранного режима" else "Полноэкранный режим",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isFullscreen) {
                // Информация о видео
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Информация об авторе
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                author?.userId?.let { userId ->
                                    navController.navigate("profile/$userId")
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val avatarUrl = author?.photoUrl?.let { url ->
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
                        Text(
                            text = author?.nickname ?: "Загрузка...",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

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
                                onClick = { 
                                    navController.navigate("videos/tag/$tag")
                                },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
            }
        }
    }
}