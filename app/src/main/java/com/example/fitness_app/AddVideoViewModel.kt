package com.example.fitness_app

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitness_app.model.AddVideoUiState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddVideoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AddVideoUiState())
    val uiState: StateFlow<AddVideoUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun toggleTag(tag: String) {
        val currentTags = _uiState.value.selectedTags
        _uiState.value = _uiState.value.copy(
            selectedTags = if (currentTags.contains(tag)) {
                currentTags - tag
            } else {
                currentTags + tag
            }
        )
    }

    fun setSelectedVideo(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedVideoUri = uri)
    }

    fun uploadVideo(context: Context, userId: String, onComplete: (Boolean) -> Unit) {
        val uri = _uiState.value.selectedVideoUri ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        val videoUploadService = VideoUploadService(context)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val videoUrlResult = videoUploadService.uploadVideo(uri, userId)
                videoUrlResult.fold(
                    onSuccess = { videoUrl ->
                        val videoData = mapOf(
                            "title" to _uiState.value.title,
                            "description" to _uiState.value.description,
                            "userId" to userId,
                            "videoUrl" to videoUrl,
                            "tags" to _uiState.value.selectedTags.toList(),
                            "views" to 0,
                            "uploadDate" to System.currentTimeMillis()
                        )

                        firestore.collection("videos")
                            .add(videoData)
                            .addOnSuccessListener {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    uploadSuccess = true
                                )
                                onComplete(true)
                            }
                            .addOnFailureListener { e ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    uploadSuccess = false,
                                    error = e.message
                                )
                                onComplete(false)
                            }
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            uploadSuccess = false,
                            error = e.message
                        )
                        onComplete(false)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    uploadSuccess = false,
                    error = e.message
                )
                onComplete(false)
            }
        }
    }

    fun reset() {
        _uiState.value = AddVideoUiState()
    }
}