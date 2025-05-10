package com.example.fitness_app

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitness_app.model.FitnessGoal
import com.example.fitness_app.ui.AddGoalDialog
import com.example.fitness_app.ui.FinishedGoalItem
import com.example.fitness_app.ui.GoalItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FitnessGoalsScreen() {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val activeGoals = remember { mutableStateListOf<FitnessGoal>() }
    val finishedGoals = remember { mutableStateListOf<FitnessGoal>() }
    var showAddDialog by remember { mutableStateOf(false) }

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
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(activeGoals, key = { it.id }) { goal ->
                    GoalItem(goal, firestore)
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
                    FinishedGoalItem(goal, firestore)
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
                firestore.collection("fitness_goals").add(newGoal)
            }
        )
    }
}