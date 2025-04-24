package com.example.fitness_app

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.navigation.NavController
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toUser() = User(
    userId = id,
    email = getString("email") ?: "",
    nickname = getString("nickname") ?: "No name",
    height = getDouble("height") ?: 0.0,
    weight = getDouble("weight") ?: 0.0,
    goalWeight = getDouble("goal_weight") ?: 0.0,
    dailyStepGoal = getLong("daily_step_goal")?.toInt() ?: 10000,
    createdAt = getTimestamp("created_at") ?: com.google.firebase.Timestamp.now()
)

@Composable
fun AuthorsScreen(userId: String, navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val authors = remember { mutableStateListOf<User>() }
    val subscriptions = remember { mutableStateOf(emptySet<String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredAuthors by remember(authors, searchQuery) {
        derivedStateOf {
            authors.filter {
                it.nickname.contains(searchQuery, ignoreCase = true) ||
                        it.email.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    DisposableEffect(userId) {
        val subscriptionListener = db.collection("subscriptions")
            .whereEqualTo("followerId", userId)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.let {
                    subscriptions.value = it.mapNotNull { doc -> doc.getString("authorId") }.toSet()
                }
            }

        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                authors.clear()
                result.documents.forEach { doc ->
                    if (doc.id != userId) authors.add(doc.toUser())
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Ошибка загрузки авторов", Toast.LENGTH_SHORT).show()
            }

        onDispose { subscriptionListener.remove() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Поле поиска
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Поиск авторов") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredAuthors.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (authors.isEmpty()) "Нет доступных авторов"
                    else "Ничего не найдено по запросу '$searchQuery'"
                )
            }
        } else {
            LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                items(filteredAuthors) { author ->
                    AuthorItem(
                        author = author,
                        isSubscribed = subscriptions.value.contains(author.userId),
                        onSubscribe = { subscribeToAuthor(userId, author) {} },
                        onUnsubscribe = { unsubscribeFromAuthor(userId, author) {} }
                    )
                }
            }
        }
    }
}

@Composable
fun AuthorItem(
    author: User,
    isSubscribed: Boolean,
    onSubscribe: () -> Unit,
    onUnsubscribe: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(author.nickname, style = MaterialTheme.typography.titleMedium)
            }
            if (isSubscribed) {
                OutlinedButton(onClick = onUnsubscribe) { Text("Отписаться") }
            } else {
                Button(onClick = onSubscribe) { Text("Подписаться") }
            }
        }
    }
}

fun subscribeToAuthor(
    followerId: String,
    author: User,
    onSuccess: () -> Unit
) {
    FirebaseFirestore.getInstance().collection("subscriptions")
        .add(
            hashMapOf(
                "followerId" to followerId,
                "authorId" to author.userId,
                "timestamp" to FieldValue.serverTimestamp()
            )
        )
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> Log.e("Subscribe", "Ошибка: ${e.message}") }
}

fun unsubscribeFromAuthor(
    followerId: String,
    author: User, // Принимаем объект User
    onSuccess: () -> Unit
) {
    FirebaseFirestore.getInstance().collection("subscriptions")
        .whereEqualTo("followerId", followerId)
        .whereEqualTo("authorId", author.userId) // Используем author.userId
        .get()
        .addOnSuccessListener { querySnapshot ->
            querySnapshot.documents.forEach { doc ->
                doc.reference.delete()
            }
            onSuccess()
        }
}