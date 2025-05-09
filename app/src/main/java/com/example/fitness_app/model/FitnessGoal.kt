package com.example.fitness_app.model

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

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

    fun getFormattedPlannedDate(): String {
        return SimpleDateFormat("dd.MM.yyyy").format(Date(plannedDate.seconds * 1000))
    }

    fun getFormattedCreatedDate(): String {
        return SimpleDateFormat("dd.MM.yyyy").format(Date(createdDate.seconds * 1000))
    }

    fun getProgressPercentage(): Float {
        return (currentProgress.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    }

    fun getTargetDisplay(): String {
        return when (type) {
            GoalType.REPS -> "$target повторений"
            GoalType.TIME -> formatTime(target)
        }
    }

    companion object {
        fun formatTime(seconds: Long): String {
            val m = seconds / 60
            val s = seconds % 60
            return "%02d:%02d".format(m, s)
        }
    }
}