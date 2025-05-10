package com.example.fitness_app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitness_app.model.FitnessGoal
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalItem(goal: FitnessGoal, firestore: FirebaseFirestore) {
    var isTimerRunning by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(goal.currentProgress) }
    val isCompleted by remember(goal) { derivedStateOf { currentProgress >= goal.target } }
    val animatedProgress by animateFloatAsState(
        targetValue = currentProgress.toFloat() / goal.target.toFloat(),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )
    val coroutineScope = rememberCoroutineScope()

    fun updateProgress(newValue: Long) {
        if (newValue > goal.target) return

        val updates = mapOf(
            "currentProgress" to newValue,
            "isCompleted" to (newValue >= goal.target)
        )

        firestore.collection("fitness_goals").document(goal.id)
            .update(updates)
            .addOnSuccessListener {
                currentProgress = newValue
                if (newValue >= goal.target) {
                    moveGoalToFinished(goal.copy(currentProgress = newValue), firestore)
                }
            }
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (isTimerRunning) {
                delay(1000L)
                if (currentProgress < goal.target) {
                    updateProgress(currentProgress + 1)
                } else {
                    isTimerRunning = false
                    break
                }
            }
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                firestore.collection("fitness_goals").document(goal.id).delete()
                true
            } else {
                false
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.error)
                        .clip(RoundedCornerShape(18.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            content = {
                GoalContent(
                    goal = goal,
                    currentProgress = currentProgress,
                    animatedProgress = animatedProgress,
                    isTimerRunning = isTimerRunning,
                    onTimerToggle = { isTimerRunning = it },
                    onProgressUpdate = { updateProgress(it) }
                )
            }
        )
    }
}

@Composable
private fun GoalContent(
    goal: FitnessGoal,
    currentProgress: Long,
    animatedProgress: Float,
    isTimerRunning: Boolean,
    onTimerToggle: (Boolean) -> Unit,
    onProgressUpdate: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp)
    ) {
        Column {
            Text(
                goal.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = animatedProgress.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Прогресс: $currentProgress/${goal.target}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${SimpleDateFormat("dd.MM.yyyy").format(Date(goal.plannedDate.seconds * 1000))}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            when (goal.type) {
                FitnessGoal.GoalType.REPS -> RepsControl(
                    current = currentProgress,
                    onUpdate = onProgressUpdate,
                    max = goal.target
                )
                FitnessGoal.GoalType.TIME -> TimeControl(
                    isRunning = isTimerRunning,
                    onToggle = onTimerToggle,
                    current = currentProgress,
                    onUpdate = onProgressUpdate,
                    max = goal.target
                )
            }
        }
    }
}

@Composable
fun RepsControl(current: Long, onUpdate: (Long) -> Unit, max: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onUpdate((current - 1).coerceAtLeast(0)) },
            enabled = current > 0,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "-",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        Text(
            "$current",
            modifier = Modifier.padding(horizontal = 12.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        IconButton(
            onClick = { onUpdate((current + 1).coerceAtMost(max)) },
            enabled = current < max,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "+",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun TimeControl(
    isRunning: Boolean,
    onToggle: (Boolean) -> Unit,
    current: Long,
    onUpdate: (Long) -> Unit,
    max: Long
) {
    fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onToggle(!isRunning) },
            enabled = current < max,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Пауза" else "Старт",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            formatTime(current),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
        )
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(
            onClick = { onUpdate(0L) },
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Сбросить",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedGoalItem(goal: FitnessGoal, firestore: FirebaseFirestore) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                firestore.collection("finished_goals").document(goal.id).delete()
                true
            } else {
                false
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.error)
                        .clip(RoundedCornerShape(18.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                goal.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            )
                            Text(
                                "Выполнено: ${SimpleDateFormat("dd.MM.yyyy").format(Date(goal.createdDate.seconds * 1000))}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                "Цель: ${goal.target} ${if (goal.type == FitnessGoal.GoalType.REPS) "повторений" else "секунд"}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        )
    }
}

private fun moveGoalToFinished(goal: FitnessGoal, firestore: FirebaseFirestore) {
    val finishedGoalsRef = firestore.collection("finished_goals").document()
    val finishedGoal = goal.copy(
        id = finishedGoalsRef.id,
        isCompleted = true,
        currentProgress = goal.target,
        createdDate = Timestamp.now()
    )

    firestore.runBatch { batch ->
        batch.set(finishedGoalsRef, finishedGoal.toMap())
        batch.delete(firestore.collection("fitness_goals").document(goal.id))
    }
} 