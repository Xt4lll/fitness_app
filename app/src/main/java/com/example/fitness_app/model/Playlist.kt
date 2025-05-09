package com.example.fitness_app.model

data class Playlist(
    val id: String,
    val name: String,
    val userId: String,
    val videos: List<String>
)