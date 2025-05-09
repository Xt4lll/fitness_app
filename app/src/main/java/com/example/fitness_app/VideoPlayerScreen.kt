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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlaylistAdd
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.RectangleShape
import com.example.fitness_app.model.Playlist
import com.example.fitness_app.model.User
import com.example.fitness_app.model.Video
import com.google.firebase.Timestamp

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
    var isFavorite by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? androidx.appcompat.app.AppCompatActivity
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val autoHideControlsDelay = 3000L // 3 секунды
    val coroutineScope = rememberCoroutineScope()

    // Улучшенная анимация
    var animateTrigger by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (animateTrigger) 1.35f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 400f), label = "favScale"
    )
    val animatedRotation by animateFloatAsState(
        targetValue = if (animateTrigger) -20f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f), label = "favRot"
    )
    val animatedColor by animateColorAsState(
        targetValue = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(stiffness = 400f), label = "favColor"
    )

    // --- Playlist add dialog ---
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var userPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var isPlaylistsLoading by remember { mutableStateOf(false) }
    var playlistAddSuccess by remember { mutableStateOf(false) }

    fun loadUserPlaylists() {
        val uid = currentUser?.uid ?: return
        isPlaylistsLoading = true
        db.collection("playlists")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snap ->
                userPlaylists = snap.documents.map { doc ->
                    Playlist(
                        id = doc.id,
                        name = doc.getString("name") ?: "Без названия",
                        userId = doc.getString("userId") ?: "",
                        videos = (doc.get("videos") as? List<String>) ?: emptyList()
                    )
                }
                isPlaylistsLoading = false
            }
    }

    fun addVideoToPlaylist(playlist: Playlist) {
        val newVideos = playlist.videos.toMutableList().apply { if (!contains(videoId)) add(videoId) }
        db.collection("playlists").document(playlist.id)
            .update("videos", newVideos)
            .addOnSuccessListener { playlistAddSuccess = true }
    }

    // Проверяем, находится ли видео в избранном
    LaunchedEffect(videoId, currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            db.collection("favorites")
                .whereEqualTo("userId", userId)
                .whereEqualTo("videoId", videoId)
                .get()
                .addOnSuccessListener { documents ->
                    isFavorite = !documents.isEmpty
                }
        }
    }

    // Функция для добавления/удаления из избранного
    fun triggerFavoriteAnimation() {
        animateTrigger = true
        coroutineScope.launch {
            kotlinx.coroutines.delay(220)
            animateTrigger = false
        }
    }

    fun toggleFavorite() {
        val userId = currentUser?.uid ?: return
        if (isFavorite) {
            db.collection("favorites")
                .whereEqualTo("userId", userId)
                .whereEqualTo("videoId", videoId)
                .get()
                .addOnSuccessListener { documents ->
                    documents.documents.forEach { doc ->
                        doc.reference.delete()
                    }
                    isFavorite = false
                    triggerFavoriteAnimation()
                }
        } else {
            db.collection("favorites")
                .add(
                    hashMapOf(
                        "userId" to userId,
                        "videoId" to videoId,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                )
                .addOnSuccessListener {
                    isFavorite = true
                    triggerFavoriteAnimation()
                }
        }
    }

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
                                    createdAt = userDoc.getTimestamp("created_at") ?: Timestamp.now()
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
                    title = { Text(video?.title ?: "Воспроизведение видео", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        if (currentUser != null) {
                            IconButton(
                                onClick = { toggleFavorite() },
                                modifier = Modifier
                                    .scale(animatedScale)
                                    .graphicsLayer {
                                        rotationZ = animatedRotation
                                    }
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Удалить из избранного" else "Добавить в избранное",
                                    tint = animatedColor
                                )
                            }
                            IconButton(
                                onClick = {
                                    loadUserPlaylists()
                                    showAddToPlaylistDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = "Добавить в плейлист"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isFullscreen) Modifier else Modifier.verticalScroll(rememberScrollState()))
                .background(MaterialTheme.colorScheme.background)
                .padding(if (isFullscreen) PaddingValues(0.dp) else padding),
            verticalArrangement = Arrangement.Top,
        ) {
            // Видео-плеер в карточке
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp)
                    .shadow(8.dp, RectangleShape),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black
                )
            ) {
                Box(
                    modifier = if (isFullscreen)
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
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
                                    .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape),
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
                                    .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape),
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
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(16.dp),
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
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
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
                                    IconButton(
                                        onClick = { seekBy(-10_000) },
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.FastRewind, contentDescription = "Назад 10 сек", tint = Color.White, modifier = Modifier.size(40.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    IconButton(
                                        onClick = { togglePlayPause() },
                                        modifier = Modifier.size(80.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                                            tint = Color.White,
                                            modifier = Modifier.size(56.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    IconButton(
                                        onClick = { seekBy(10_000) },
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.FastForward, contentDescription = "Вперёд 10 сек", tint = Color.White, modifier = Modifier.size(40.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    IconButton(
                                        onClick = { toggleFullscreen() },
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                            contentDescription = if (isFullscreen) "Выйти из полноэкранного режима" else "Полноэкранный режим",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isFullscreen) {
                // Информация о видео и авторе в карточке
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    var subscriberCount by remember { mutableStateOf(0) }
                    val authorId = author?.userId
                    // Слушатель количества подписчиков
                    LaunchedEffect(authorId) {
                        authorId?.let { id ->
                            db.collection("subscriptions")
                                .whereEqualTo("authorId", id)
                                .addSnapshotListener { snapshot, _ ->
                                    subscriberCount = snapshot?.size() ?: 0
                                }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Автор
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    author?.userId?.let { userId ->
                                        navController.navigate("author_videos/$userId")
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val avatarUrl = author?.photoUrl?.let { url ->
                                if (url.contains("?")) "$url&tr=w-64,h-64" else "$url?tr=w-64,h-64"
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
                                Text(
                                    text = "$subscriberCount подписчиков",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Заголовок
                        Text(
                            text = video?.title ?: "",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // Описание
                        Text(
                            text = video?.description ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Просмотры и дата
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Просмотров: ${video?.views ?: 0}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = video?.uploadDate?.let { "Дата: " + java.text.SimpleDateFormat("dd.MM.yyyy").format(java.util.Date(it)) } ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Теги
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            video?.tags?.forEach { tag ->
                                SuggestionChip(
                                    onClick = {
                                        navController.navigate("videos/tag/$tag")
                                    },
                                    label = { Text(tag, style = MaterialTheme.typography.labelLarge) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showAddToPlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showAddToPlaylistDialog = false; playlistAddSuccess = false },
                title = { Text("Добавить в плейлист") },
                text = {
                    if (isPlaylistsLoading) {
                        CircularProgressIndicator()
                    } else if (userPlaylists.isEmpty()) {
                        Text("У вас нет плейлистов. Создайте их на странице авторов.")
                    } else if (playlistAddSuccess) {
                        Text("Видео добавлено!")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            userPlaylists.forEach { playlist ->
                                Button(
                                    onClick = {
                                        addVideoToPlaylist(playlist)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(playlist.name)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddToPlaylistDialog = false; playlistAddSuccess = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }
    }
}