package com.example.fitness_app

import com.google.firebase.Timestamp
import java.util.Date

data class FitnessGoal(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val type: GoalType = GoalType.REPS,
    val target: Int = 0,
    val currentProgress: Int = 0,
    val createdDate: Timestamp = Timestamp.now(),
    val plannedDate: Timestamp = Timestamp.now(),
    val isCompleted: Boolean = false
) {
    enum class GoalType(val displayName: String) {
        REPS("Повторения"),
        TIME("Время")
    }
}
