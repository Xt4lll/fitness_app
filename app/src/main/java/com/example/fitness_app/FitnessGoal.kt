package com.example.fitness_app

import com.google.firebase.Timestamp


data class FitnessGoal(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val type: GoalType = GoalType.REPS,
    val target: Long = 0L,
    val currentProgress: Long = 0L,
    val createdDate: Timestamp = Timestamp.now(),
    val plannedDate: Timestamp = Timestamp.now(),
    val isCompleted: Boolean = false
) {
    enum class GoalType(val displayName: String) {
        REPS("Повторения"),
        TIME("Время")
    }

    fun toMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title,
        "type" to type.name,
        "target" to target,
        "currentProgress" to currentProgress,
        "createdDate" to createdDate,
        "plannedDate" to plannedDate,
        "isCompleted" to isCompleted
    )
}