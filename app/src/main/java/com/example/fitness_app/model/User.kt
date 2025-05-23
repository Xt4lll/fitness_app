package com.example.fitness_app.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.PropertyName

data class User(
    // Основные поля профиля
    @get:PropertyName("user_id")
    val userId: String = "", // ID документа в Firestore

    @get:PropertyName("email")
    val email: String = "",

    @get:PropertyName("nickname")
    val nickname: String = "",

    @get:PropertyName("photoUrl")
    val photoUrl: String? = null,

    // Физические параметры
    @get:PropertyName("height")
    val height: Double? = null, // в сантиметрах

    @get:PropertyName("weight")
    val weight: Double? = null, // в килограммах

    @get:PropertyName("goal_weight")
    val goalWeight: Double? = null, // целевой вес

    // Цели и активность
    @get:PropertyName("daily_step_goal")
    val dailyStepGoal: Int? = null, // цель по шагам

    // Метаданные
    @get:PropertyName("created_at")
    val createdAt: Timestamp = Timestamp.now(), // дата создания
) {
    // Дополнительные методы при необходимости
    fun isAuthor() = !userId.isBlank() // любой зарегистрированный пользователь может быть автором
}

fun DocumentSnapshot.toUser() = User(
    userId = id,
    email = getString("email") ?: "",
    nickname = getString("nickname") ?: "No name",
    photoUrl = getString("photoUrl"),
    height = getDouble("height"),
    weight = getDouble("weight"),
    goalWeight = getDouble("goal_weight"),
    dailyStepGoal = getLong("daily_step_goal")?.toInt(),
    createdAt = getTimestamp("created_at") ?: Timestamp.now()
)

