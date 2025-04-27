package com.example.fitness_app

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.navigation.NavController
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SubscriptionsScreen(userId: String, navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val authors = remember { mutableStateListOf<User>() }
    val subscriptions = remember { mutableStateOf(emptySet<String>()) } // Добавлено состояние подписок
    var isLoading by remember { mutableStateOf(true) }

    // Добавлен слушатель подписок в реальном времени
    DisposableEffect(userId) {
        val listener = db.collection("subscriptions")
            .whereEqualTo("followerId", userId)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.let {
                    subscriptions.value = it.mapNotNull { doc -> doc.getString("authorId") }.toSet()
                }
            }

        onDispose { listener.remove() }
    }

    LaunchedEffect(userId) {
        db.collection("subscriptions")
            .whereEqualTo("followerId", userId)
            .get()
            .addOnSuccessListener { result ->
                val authorIds = result.documents.mapNotNull { it.getString("authorId") }
                if (authorIds.isNotEmpty()) {
                    db.collection("users")
                        .whereIn(FieldPath.documentId(), authorIds)
                        .get()
                        .addOnSuccessListener { usersResult ->
                            authors.clear()
                            usersResult.documents.forEach { doc ->
                                authors.add(
                                    User(
                                        userId = doc.id,
                                        nickname = doc.getString("nickname") ?: "Без имени",
                                        photoUrl = doc.getString("photoUrl")
                                    )
                                )
                            }
                            isLoading = false
                        }
                } else {
                    isLoading = false
                }
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Ошибка загрузки подписок", Toast.LENGTH_SHORT).show()
            }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (authors.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Вы ни на кого не подписаны")
        }
    } else {
        LazyColumn(Modifier.padding(16.dp)) {
            items(authors) { author ->
                val avatarUrl = author.photoUrl?.let { url ->
                    if (url.contains("?")) "$url&tr=w-48,h-48" else "$url?tr=w-48,h-48"
                } ?: ""
                AuthorItem(
                    author = author.copy(photoUrl = avatarUrl),
                    isSubscribed = subscriptions.value.contains(author.userId), // Используем актуальное состояние
                    onSubscribe = {
                        subscribeToAuthor(userId, author) {
                            // Автоматически обновится через snapshotListener
                        }
                    },
                    onUnsubscribe = {
                        unsubscribeFromAuthor(userId, author) {
                            // Автоматически обновится через snapshotListener
                        }
                    }
                )
            }
        }
    }
}