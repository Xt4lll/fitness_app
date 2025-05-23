package com.example.fitness_app.model

data class Video(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val userId: String = "",
    val videoUrl: String = "",
    val tags: List<String> = emptyList(),
    val views: Long = 0,
    val uploadDate: Long = System.currentTimeMillis()
) 