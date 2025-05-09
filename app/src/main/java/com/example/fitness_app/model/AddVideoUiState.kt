package com.example.fitness_app.model

import android.net.Uri

data class AddVideoUiState(
    val title: String = "",
    val description: String = "",
    val selectedTags: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val selectedVideoUri: Uri? = null,
    val uploadSuccess: Boolean? = null,
    val error: String? = null
)