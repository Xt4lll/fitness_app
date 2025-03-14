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
import androidx.compose.foundation.ExperimentalFoundationApi


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessGoalsScreen() {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val goals = remember { mutableStateListOf<FitnessGoal>() }
    var showAddDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        firestore.collection("fitness_goals")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIREBASE", "Ошибка слушателя: ${error.message}")
                    return@addSnapshotListener
                }
                val newGoals = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FitnessGoal::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                goals.clear()
                goals.addAll(newGoals.sortedWith(
                    compareBy(
                        { it.isCompleted },
                        { -it.createdDate.seconds }
                    )
                ))
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
            items(goals) { goal ->
                GoalItem(goal, firestore)
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
                    try {
                        firestore.collection("fitness_goals")
                            .add(newGoal)
                            .addOnSuccessListener {
                                Log.d("FIREBASE", "Документ добавлен: ${it.id}")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FIREBASE", "Ошибка: ${e.message}")
                            }
                    } catch (e: Exception) {
                        Log.e("FIREBASE", "Исключение: ${e.message}")
                    }
                }
            }
        )
    }
}

@Composable
fun GoalItem(goal: FitnessGoal, firestore: FirebaseFirestore) {
    var isTimerRunning by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(goal.currentProgress) }
    var checkedState by remember { mutableStateOf(goal.isCompleted) }

    // Автоматическое завершение цели
    LaunchedEffect(currentProgress) {
        if (currentProgress >= goal.target && !checkedState) {
            firestore.collection("fitness_goals").document(goal.id)
                .update(mapOf(
                    "isCompleted" to true,
                    "currentProgress" to goal.target
                ))
                .addOnSuccessListener {
                    checkedState = true
                    currentProgress = goal.target
                }
        }
    }

    // Обновление прогресса с проверкой
    fun updateProgress(newValue: Long) {
        if (newValue > goal.target) return

        firestore.collection("fitness_goals").document(goal.id)
            .update("currentProgress", newValue)
            .addOnSuccessListener {
                currentProgress = newValue.toInt()
            }
    }

    // Таймер для времени
    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && currentProgress < goal.target) {
            delay(1000L)
            updateProgress(currentProgress + 1L)
        }
        isTimerRunning = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checkedState) Color.LightGray.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checkedState,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            firestore.collection("fitness_goals").document(goal.id)
                                .update(mapOf(
                                    "isCompleted" to true,
                                    "currentProgress" to goal.target
                                ))
                                .addOnSuccessListener {
                                    checkedState = true
                                    currentProgress = goal.target
                                }
                        } else {
                            firestore.collection("fitness_goals").document(goal.id)
                                .update("isCompleted", false)
                                .addOnSuccessListener { checkedState = false }
                        }
                    }
                )
                Text(goal.title, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        firestore.collection("fitness_goals")
                            .document(goal.id)
                            .delete()
                    }
                ) {
                    Icon(Icons.Default.Delete, "Удалить")
                }
            }

            LinearProgressIndicator(
                progress = (currentProgress.toFloat() / goal.target.toFloat()).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )

            Text("Прогресс: $currentProgress/${goal.target}")
            Text("Дата выполнения: ${SimpleDateFormat("dd.MM.yyyy").format(Date(goal.plannedDate.seconds * 1000))}")

            when (goal.type) {
                FitnessGoal.GoalType.REPS -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { updateProgress((currentProgress - 1L).coerceAtLeast(0)) }
                        ) {
                            Icon(Icons.Default.Remove, "-")
                        }
                        Text("$currentProgress")
                        IconButton(
                            onClick = { updateProgress(currentProgress + 1L) }
                        ) {
                            Icon(Icons.Default.Add, "+")
                        }
                    }
                }
                FitnessGoal.GoalType.TIME -> {
                    Button(
                        onClick = { isTimerRunning = !isTimerRunning },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTimerRunning) Color.Red else Color.Green
                        ),
                        enabled = !checkedState
                    ) {
                        Text(if (isTimerRunning) "Пауза" else "Старт")
                    }
                }
            }
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