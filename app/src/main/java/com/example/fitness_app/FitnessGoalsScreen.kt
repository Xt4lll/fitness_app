package com.example.fitness_app

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessGoalsScreen() {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val activeGoals = remember { mutableStateListOf<FitnessGoal>() }
    val finishedGoals = remember { mutableStateListOf<FitnessGoal>() }
    var showAddDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        firestore.collection("fitness_goals")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                snapshot?.documents?.mapNotNull {
                    it.toObject(FitnessGoal::class.java)?.copy(id = it.id)
                }?.let {
                    activeGoals.clear()
                    activeGoals.addAll(it)
                }
            }

        firestore.collection("finished_goals")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                snapshot?.documents?.mapNotNull {
                    it.toObject(FitnessGoal::class.java)?.copy(id = it.id)
                }?.let {
                    finishedGoals.clear()
                    finishedGoals.addAll(it)
                }
            }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Фитнес-цели") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Добавить цель")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { Text("Активные цели", style = MaterialTheme.typography.headlineSmall) }
            items(activeGoals) { goal ->
                GoalItem(goal, firestore)
                Divider()
            }

            item { Text("Выполненные цели", style = MaterialTheme.typography.headlineSmall) }
            items(finishedGoals) { goal ->
                FinishedGoalItem(goal)
                Divider()
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            userId = userId,
            onDismiss = { showAddDialog = false },
            onSave = { newGoal ->
                coroutineScope.launch {
                    firestore.collection("fitness_goals").add(newGoal.toMap())
                }
            }
        )
    }
}

@Composable
fun GoalItem(goal: FitnessGoal, firestore: FirebaseFirestore) {
    var isTimerRunning by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(goal.currentProgress) }
    val isCompleted by remember(goal) { derivedStateOf { currentProgress >= goal.target } }
    val animatedProgress by animateFloatAsState(
        targetValue = currentProgress.toFloat() / goal.target.toFloat(),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    fun updateProgress(newValue: Long) {
        if (newValue > goal.target) return

        firestore.collection("fitness_goals").document(goal.id)
            .update(mapOf(
                "currentProgress" to newValue,
                "isCompleted" to (newValue >= goal.target)
            ))
            .addOnSuccessListener {
                currentProgress = newValue
                if (newValue >= goal.target) {
                    moveGoalToFinished(goal.copy(currentProgress = newValue), firestore)
                }
            }
    }

    LaunchedEffect(isCompleted) {
        if (isCompleted && !goal.isCompleted) {
            moveGoalToFinished(goal.copy(currentProgress = currentProgress), firestore)
        }
    }

    // Таймер для TIME
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

    // Обработчик достижения цели
    LaunchedEffect(currentProgress) {
        if (currentProgress >= goal.target && !goal.isCompleted) {
            moveGoalToFinished(goal.copy(currentProgress = currentProgress), firestore)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = null,
                    enabled = false,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(goal.title, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { firestore.collection("fitness_goals").document(goal.id).delete() }
                ) {
                    Icon(Icons.Default.Delete, "Удалить")
                }
            }

            LinearProgressIndicator(
                progress = animatedProgress.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )

            Text("Прогресс: $currentProgress/${goal.target}")
            Text("Дата выполнения: ${SimpleDateFormat("dd.MM.yyyy").format(Date(goal.plannedDate.seconds * 1000))}")

            when (goal.type) {
                FitnessGoal.GoalType.REPS -> RepsControl(
                    current = currentProgress,
                    onUpdate = { newValue ->
                        updateProgress(newValue) // Непосредственно вызываем обновление в Firebase
                    },
                    max = goal.target
                )
                FitnessGoal.GoalType.TIME -> TimeControl(
                    isRunning = isTimerRunning,
                    onToggle = { isTimerRunning = it },
                    current = currentProgress,
                    onUpdate = { newValue ->
                        updateProgress(newValue) // Аналогично для TimeControl
                    },
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
            enabled = current > 0
        ) { Icon(Icons.Default.Remove, "-") }

        Text("$current", modifier = Modifier.padding(horizontal = 8.dp))

        IconButton(
            onClick = { onUpdate((current + 1).coerceAtMost(max)) },
            enabled = current < max
        ) { Icon(Icons.Default.Add, "+") }
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
//    LaunchedEffect(isRunning) {
//        while (isRunning && current < max) {
//            delay(1000L)
//            onUpdate(current + 1) // Теперь это будет вызывать updateProgress
//        }
//        if (current >= max) onToggle(false)
//    }

    Button(
        onClick = { onToggle(!isRunning) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRunning) Color.Red else Color.Green
        ),
        enabled = current < max
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isRunning) "Пауза" else "Старт"
        )
    }
}

@Composable
fun FinishedGoalItem(goal: FitnessGoal) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.LightGray.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(goal.title, style = MaterialTheme.typography.titleMedium)
            Text("Выполнено: ${SimpleDateFormat("dd.MM.yyyy").format(Date(goal.createdDate.seconds * 1000))}")
            Text("Цель: ${goal.target} ${if (goal.type == FitnessGoal.GoalType.REPS) "повторений" else "секунд"}")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(
    userId: String,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var goalType by remember { mutableStateOf(FitnessGoal.GoalType.REPS) }
    var plannedDate by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isDateValid by remember { mutableStateOf(true) } // Флаг для валидации даты

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = plannedDate,
            onDateSelected = { date ->
                // Проверка, что выбранная дата не прошедшая
                if (date.before(Date())) {
                    isDateValid = false
                } else {
                    plannedDate = date
                    isDateValid = true
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Новая цель", style = MaterialTheme.typography.headlineSmall)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название цели*") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FitnessGoal.GoalType.values().forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { goalType = type }
                        ) {
                            RadioButton(
                                selected = goalType == type,
                                onClick = { goalType = type }
                            )
                            Text(text = type.displayName)
                        }
                    }
                }

                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Цель (${if (goalType == FitnessGoal.GoalType.REPS) "повторений" else "секунд"})*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Дата выполнения: ${SimpleDateFormat("dd.MM.yyyy").format(plannedDate)}")
                }

                // Если дата не валидна, показываем ошибку
                if (!isDateValid) {
                    Text("Дата не может быть в прошлом", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = onDismiss) { Text("Отмена") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newGoal = hashMapOf<String, Any>(
                                "userId" to userId,
                                "title" to title,
                                "type" to goalType.name,
                                "target" to (target.toLongOrNull() ?: 0L),
                                "currentProgress" to 0L,
                                "createdDate" to Timestamp.now(),
                                "plannedDate" to Timestamp(plannedDate),
                                "isCompleted" to false
                            )
                            onSave(newGoal)
                            onDismiss()
                        },
                        enabled = title.isNotBlank() && target.isNotBlank() && isDateValid // Проверка на валидность даты
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: Date,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.time
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(Date(it))
                    }
                }
            ) {
                Text("Выбрать")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

private fun moveGoalToFinished(goal: FitnessGoal, firestore: FirebaseFirestore) {
    val finishedGoal = goal.copy(
        isCompleted = true,
        currentProgress = goal.target,
        createdDate = Timestamp.now()
    )

    firestore.runBatch { batch ->
        batch.set(firestore.collection("finished_goals").document(), finishedGoal.toMap())
        batch.delete(firestore.collection("fitness_goals").document(goal.id))
    }.addOnCompleteListener {
        if (it.isSuccessful) {
            Log.d("FIREBASE", "Цель перемещена")
        } else {
            Log.e("FIREBASE", "Ошибка: ${it.exception}")
            // Откатываем статус
            firestore.collection("fitness_goals").document(goal.id)
                .update("isCompleted", false)
        }
    }
}