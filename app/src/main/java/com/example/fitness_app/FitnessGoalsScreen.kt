package com.example.fitness_app

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.example.fitness_app.ui.theme.GreenishCyan
import com.example.fitness_app.ui.theme.Red
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun FitnessGoalsScreen() {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val activeGoals = remember { mutableStateListOf<FitnessGoal>() }
    val finishedGoals = remember { mutableStateListOf<FitnessGoal>() }
    var showAddDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(userId) {
        firestore.collection("fitness_goals")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIREBASE", "Error listening to fitness_goals: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.documents?.mapNotNull {
                    try {
                        it.toObject(FitnessGoal::class.java)?.copy(id = it.id)
                    } catch (e: Exception) {
                        Log.e("FIREBASE", "Error parsing goal: ${e.message}")
                        null
                    }
                }?.let {
                    activeGoals.clear()
                    activeGoals.addAll(it)
                }
            }

        firestore.collection("finished_goals")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIREBASE", "Error listening to finished_goals: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.documents?.mapNotNull {
                    try {
                        it.toObject(FitnessGoal::class.java)?.copy(id = it.id)
                    } catch (e: Exception) {
                        Log.e("FIREBASE", "Error parsing finished goal: ${e.message}")
                        null
                    }
                }?.let {
                    finishedGoals.clear()
                    finishedGoals.addAll(it)
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Твои цели",
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            if (activeGoals.isNotEmpty()) {
                item {
                    Text(
                        text = "Активные цели",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(activeGoals, key = { it.id }) { goal ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        GoalItem(goal, firestore)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            if (finishedGoals.isNotEmpty()) {
                item {
                    Text(
                        text = "Выполненные цели",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(finishedGoals, key = { it.id }) { goal ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        FinishedGoalItem(goal, firestore)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(64.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить цель", modifier = Modifier.size(32.dp))
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
    val context = LocalContext.current
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
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Error updating progress: ${e.message}")
                Toast.makeText(context, "Ошибка обновления прогресса", Toast.LENGTH_SHORT).show()
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
                firestore.collection("fitness_goals").document(goal.id)
                    .delete()
                    .addOnSuccessListener {
                        Log.d("FIREBASE", "Goal deleted successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FIREBASE", "Error deleting goal: ${e.message}")
                        Toast.makeText(context, "Ошибка удаления цели", Toast.LENGTH_SHORT).show()
                    }
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
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            goal.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
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
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                "${SimpleDateFormat("dd.MM.yyyy").format(Date(goal.plannedDate.seconds * 1000))}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        when (goal.type) {
                            FitnessGoal.GoalType.REPS -> RepsControl(
                                current = currentProgress,
                                onUpdate = { newValue -> updateProgress(newValue) },
                                max = goal.target
                            )
                            FitnessGoal.GoalType.TIME -> TimeControl(
                                isRunning = isTimerRunning,
                                onToggle = { isTimerRunning = it },
                                current = currentProgress,
                                onUpdate = { newValue -> updateProgress(newValue) },
                                max = goal.target
                            )
                        }
                    }
                }
            }
        )
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
        ) { Icon(Icons.Default.Remove, contentDescription = "-", tint = MaterialTheme.colorScheme.onPrimary) }

        Text("$current", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)

        IconButton(
            onClick = { onUpdate((current + 1).coerceAtMost(max)) },
            enabled = current < max,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) { Icon(Icons.Default.Add, contentDescription = "+", tint = MaterialTheme.colorScheme.onPrimary) }
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
    // Форматирование времени в mm:ss
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
            Icon(Icons.Default.Refresh, contentDescription = "Сбросить", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedGoalItem(goal: FitnessGoal, firestore: FirebaseFirestore) {
    val context = LocalContext.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                firestore.collection("finished_goals").document(goal.id)
                    .delete()
                    .addOnSuccessListener {
                        Log.d("FIREBASE", "Finished goal deleted successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FIREBASE", "Error deleting finished goal: ${e.message}")
                        Toast.makeText(context, "Ошибка удаления цели", Toast.LENGTH_SHORT).show()
                    }
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
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
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
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                "Цель: ${goal.target} ${if (goal.type == FitnessGoal.GoalType.REPS) "повторений" else "секунд"}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }
        )
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
    var repsTarget by remember { mutableStateOf("") }
    var goalType by remember { mutableStateOf(FitnessGoal.GoalType.REPS) }
    var plannedDate by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isDateValid by remember { mutableStateOf(true) }
    var minutes by remember { mutableStateOf(0) }
    var seconds by remember { mutableStateOf(0) }
    var minutesExpanded by remember { mutableStateOf(false) }
    var secondsExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = plannedDate,
            onDateSelected = { date ->
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
            modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(28.dp))
                .shadow(24.dp, RoundedCornerShape(28.dp))
                .widthIn(440.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Новая цель",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 24.sp
                    ),
                    modifier = Modifier.padding(bottom = 18.dp)
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название цели*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
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
                                onClick = { goalType = type },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = type.displayName,
                                color = if (goalType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (goalType == type) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                if (goalType == FitnessGoal.GoalType.TIME) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Minutes picker
                        Box {
                            OutlinedButton(
                                onClick = { minutesExpanded = true },
                                shape = RoundedCornerShape(14.dp),
                                border = ButtonDefaults.outlinedButtonBorder,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.width(100.dp)
                            ) {
                                Text("${minutes} мин")
                            }
                            DropdownMenu(
                                expanded = minutesExpanded,
                                onDismissRequest = { minutesExpanded = false }
                            ) {
                                (0..120).forEach { min ->
                                    DropdownMenuItem(
                                        text = { Text("$min мин") },
                                        onClick = {
                                            minutes = min
                                            minutesExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        // Seconds picker
                        Box {
                            OutlinedButton(
                                onClick = { secondsExpanded = true },
                                shape = RoundedCornerShape(14.dp),
                                border = ButtonDefaults.outlinedButtonBorder,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.width(100.dp)
                            ) {
                                Text("${seconds} сек")
                            }
                            DropdownMenu(
                                expanded = secondsExpanded,
                                onDismissRequest = { secondsExpanded = false }
                            ) {
                                (0..59).forEach { sec ->
                                    DropdownMenuItem(
                                        text = { Text("$sec сек") },
                                        onClick = {
                                            seconds = sec
                                            secondsExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = repsTarget,
                        onValueChange = { repsTarget = it },
                        label = { Text("Цель (повторений)*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Дата выполнения: ${SimpleDateFormat("dd.MM.yyyy").format(plannedDate)}")
                }
                if (!isDateValid) {
                    Text("Дата не может быть в прошлом", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = ButtonDefaults.outlinedButtonBorder,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) { Text("Отмена") }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val targetInSeconds = if (goalType == FitnessGoal.GoalType.TIME) {
                                minutes * 60 + seconds
                            } else {
                                repsTarget.toLongOrNull() ?: 0L
                            }
                            val newGoal = hashMapOf<String, Any>(
                                "userId" to userId,
                                "title" to title,
                                "type" to goalType.name,
                                "target" to targetInSeconds,
                                "currentProgress" to 0L,
                                "createdDate" to Timestamp.now(),
                                "plannedDate" to Timestamp(plannedDate),
                                "isCompleted" to false
                            )
                            onSave(newGoal)
                            onDismiss()
                        },
                        enabled = (goalType == FitnessGoal.GoalType.REPS && repsTarget.isNotBlank() || goalType == FitnessGoal.GoalType.TIME && (minutes > 0 || seconds > 0)) && title.isNotBlank() && isDateValid,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) { Text("Сохранить", fontWeight = FontWeight.Bold) }
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
    }.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Log.d("FIREBASE", "Goal moved to finished successfully")
        } else {
            Log.e("FIREBASE", "Error moving goal to finished: ${task.exception?.message}")
            // Revert the completion status in case of failure
            firestore.collection("fitness_goals").document(goal.id)
                .update("isCompleted", false)
                .addOnFailureListener { e ->
                    Log.e("FIREBASE", "Error reverting completion status: ${e.message}")
                }
        }
    }
}