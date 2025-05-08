package com.example.fitness_app

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitness_app.ui.VideoTitleField
import com.example.fitness_app.ui.VideoDescriptionField
import com.example.fitness_app.ui.TagSelection

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddVideoScreen(
    userId: String,
    navController: NavController,
    viewModel: AddVideoViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setSelectedVideo(uri)
    }

    Scaffold(
        topBar = { AddVideoTopBar(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                VideoTitleField(
                    title = uiState.title,
                    onTitleChange = { viewModel.updateTitle(it) }
                )
                VideoDescriptionField(
                    description = uiState.description,
                    onDescriptionChange = { viewModel.updateDescription(it) }
                )
                TagSelection(
                    selectedTags = uiState.selectedTags,
                    onTagToggle = { viewModel.toggleTag(it) }
                )
                VideoPickerButton { videoPicker.launch("video/*") }
                SelectedVideoCard(uiState.selectedVideoUri)
                UploadButton(
                    uiState = uiState,
                    onUpload = {
                        viewModel.uploadVideo(context, userId) { success ->
                            if (success) {
                                Toast.makeText(context, "Видео успешно загружено!", Toast.LENGTH_SHORT).show()
                                viewModel.reset()
                                navController.popBackStack()
                            } else {
                                Toast.makeText(context, "Ошибка загрузки видео", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVideoTopBar(navController: NavController) {
    TopAppBar(
        title = { Text("Добавить видео", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)) },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Назад"
                )
            }
        }
    )
}

@Composable
private fun VideoPickerButton(onPick: () -> Unit) {
    Button(
        onClick = onPick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Выбрать видео", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SelectedVideoCard(selectedVideoUri: Uri?) {
    AnimatedVisibility(visible = selectedVideoUri != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Видео выбрано", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun UploadButton(
    uiState: AddVideoUiState,
    onUpload: () -> Unit
) {
    Button(
        onClick = {
            if (uiState.title.isNotBlank() && uiState.selectedTags.isNotEmpty() && uiState.selectedVideoUri != null) {
                onUpload()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        enabled = !uiState.isLoading && uiState.title.isNotBlank() && uiState.selectedTags.isNotEmpty() && uiState.selectedVideoUri != null
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Загрузить видео", style = MaterialTheme.typography.titleMedium)
        }
    }
} 