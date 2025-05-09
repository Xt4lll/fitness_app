package com.example.fitness_app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fitness_app.model.FitnessGoal
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

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
                
                GoalTitleField(title) { title = it }
                Spacer(modifier = Modifier.height(14.dp))
                
                GoalTypeSelector(goalType) { goalType = it }
                
                if (goalType == FitnessGoal.GoalType.TIME) {
                    TimeInputFields(
                        minutes = minutes,
                        seconds = seconds,
                        onMinutesChange = { minutes = it },
                        onSecondsChange = { seconds = it },
                        minutesExpanded = minutesExpanded,
                        secondsExpanded = secondsExpanded,
                        onMinutesExpandedChange = { minutesExpanded = it },
                        onSecondsExpandedChange = { secondsExpanded = it }
                    )
                } else {
                    RepsInputField(repsTarget) { repsTarget = it }
                }
                
                DateSelectorButton(
                    plannedDate = plannedDate,
                    onDateClick = { showDatePicker = true },
                    isDateValid = isDateValid
                )
                
                DialogButtons(
                    onDismiss = onDismiss,
                    onSave = {
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
                    isSaveEnabled = (goalType == FitnessGoal.GoalType.REPS && repsTarget.isNotBlank() ||
                                  goalType == FitnessGoal.GoalType.TIME && (minutes > 0 || seconds > 0)) &&
                                  title.isNotBlank() && isDateValid
                )
            }
        }
    }
}

@Composable
private fun GoalTitleField(title: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = title,
        onValueChange = onValueChange,
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
}

@Composable
private fun GoalTypeSelector(selectedType: FitnessGoal.GoalType, onTypeSelected: (FitnessGoal.GoalType) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        FitnessGoal.GoalType.values().forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTypeSelected(type) }
            ) {
                RadioButton(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = type.displayName,
                    color = if (selectedType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun TimeInputFields(
    minutes: Int,
    seconds: Int,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    minutesExpanded: Boolean,
    secondsExpanded: Boolean,
    onMinutesExpandedChange: (Boolean) -> Unit,
    onSecondsExpandedChange: (Boolean) -> Unit
) {
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        TimePickerField(
            value = minutes,
            label = "мин",
            expanded = minutesExpanded,
            onExpandedChange = onMinutesExpandedChange,
            onValueChange = onMinutesChange,
            range = 0..120
        )
        Spacer(modifier = Modifier.width(16.dp))
        TimePickerField(
            value = seconds,
            label = "сек",
            expanded = secondsExpanded,
            onExpandedChange = onSecondsExpandedChange,
            onValueChange = onSecondsChange,
            range = 0..59
        )
    }
}

@Composable
private fun TimePickerField(
    value: Int,
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Box {
        OutlinedButton(
            onClick = { onExpandedChange(true) },
            shape = RoundedCornerShape(14.dp),
            border = ButtonDefaults.outlinedButtonBorder,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.width(100.dp)
        ) {
            Text("$value $label")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            range.forEach { value ->
                DropdownMenuItem(
                    text = { Text("$value $label") },
                    onClick = {
                        onValueChange(value)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun RepsInputField(value: String, onValueChange: (String) -> Unit) {
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
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

@Composable
private fun DateSelectorButton(
    plannedDate: Date,
    onDateClick: () -> Unit,
    isDateValid: Boolean
) {
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = onDateClick,
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
        Text(
            "Дата не может быть в прошлом",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DialogButtons(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    isSaveEnabled: Boolean
) {
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
            onClick = onSave,
            enabled = isSaveEnabled,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) { Text("Сохранить", fontWeight = FontWeight.Bold) }
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