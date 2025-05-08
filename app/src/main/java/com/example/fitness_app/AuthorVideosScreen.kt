package com.example.fitness_app

import androidx.compose.foundation.layout.*
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil.compose.rememberAsyncImagePainter
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


@Composable
fun AuthorVideosScreen(authorId: String, navController: NavController) {
    val viewModel = remember { AuthorVideosViewModel(authorId) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { AuthorTopBar(author = uiState.author, navController = navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        AuthorContent(
            padding = padding,
            uiState = uiState,
            onVideoClick = { videoId -> navController.navigate("video_player/$videoId") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthorTopBar(author: User?, navController: NavController) {
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
}

@Composable
private fun AuthorContent(
    padding: PaddingValues,
    uiState: AuthorVideosUiState,
    onVideoClick: (String) -> Unit
) {
    if (uiState.isLoading) {
        LoadingContent(padding)
    } else {
        AuthorVideosList(
            padding = padding,
            author = uiState.author,
            videos = uiState.videos,
            onVideoClick = onVideoClick
        )
    }
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AuthorVideosList(
    padding: PaddingValues,
    author: User?,
    videos: List<Video>,
    onVideoClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AuthorCard(author = author)
        VideosList(videos = videos, onVideoClick = onVideoClick)
    }
}

@Composable
private fun AuthorCard(author: User?) {
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
            AuthorAvatar(author?.photoUrl)
            AuthorInfo(author)
        }
    }
}

@Composable
private fun AuthorAvatar(photoUrl: String?) {
    val avatarUrl = photoUrl?.let { url ->
        if (url.contains("?")) "$url&tr=w-64,h-64" else "$url?tr=w-64,h-64"
    } ?: ""
    
    Image(
        painter = rememberAsyncImagePainter(model = avatarUrl),
        contentDescription = "Аватар автора",
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun AuthorInfo(author: User?) {
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

@Composable
private fun VideosList(videos: List<Video>, onVideoClick: (String) -> Unit) {
    if (videos.isEmpty()) {
        EmptyVideosMessage()
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            videos.forEach { video ->
                WorkoutVideoCard(
                    video = video,
                    onClick = { onVideoClick(video.id) },
                    cardWidth = Modifier.fillMaxWidth().let { Dp.Unspecified }
                )
            }
        }
    }
}

@Composable
private fun EmptyVideosMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "У автора пока нет видео",
            style = MaterialTheme.typography.bodyLarge
        )
            }
    }

data class AuthorVideosUiState(
    val isLoading: Boolean = true,
    val author: User? = null,
    val videos: List<Video> = emptyList()
)

class AuthorVideosViewModel(private val authorId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthorVideosUiState())
    val uiState: StateFlow<AuthorVideosUiState> = _uiState.asStateFlow()
    private val db = FirebaseFirestore.getInstance()

    init {
        loadAuthorData()
        loadVideos()
    }

    private fun loadAuthorData() {
        db.collection("users")
            .document(authorId)
            .get()
            .addOnSuccessListener { document ->
                _uiState.update { it.copy(author = document.toUser()) }
            }
    }

    private fun loadVideos() {
        db.collection("videos")
            .whereEqualTo("userId", authorId)
            .addSnapshotListener { snapshot, _ ->
                val videoList = snapshot?.documents?.mapNotNull { document ->
                    Video(
                        id = document.id,
                        title = document.getString("title") ?: "",
                        description = document.getString("description") ?: "",
                        userId = document.getString("userId") ?: "",
                        videoUrl = document.getString("videoUrl") ?: "",
                        tags = (document.get("tags") as? List<String>) ?: emptyList(),
                        views = document.getLong("views") ?: 0,
                        uploadDate = document.getLong("uploadDate") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                
                _uiState.update { 
                    it.copy(
                        videos = videoList.sortedByDescending { video -> video.uploadDate },
                        isLoading = false
                    )
            }
        }
    }
} 