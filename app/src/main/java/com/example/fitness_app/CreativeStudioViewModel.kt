package com.example.fitness_app

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CreativeStudioUiState(
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class CreativeStudioViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CreativeStudioUiState())
    val uiState: StateFlow<CreativeStudioUiState> = _uiState.asStateFlow()
    
    private val db = FirebaseFirestore.getInstance()

    fun loadUserVideos(userId: String) {
        db.collection("videos")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                    return@addSnapshotListener
                }

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

                _uiState.value = _uiState.value.copy(
                    videos = videoList.sortedByDescending { it.uploadDate },
                    isLoading = false
                )
            }
    }
} 