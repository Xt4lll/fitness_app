package com.example.fitness_app

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AuthorItem(
    author: User,
    isSubscribed: Boolean,
    onSubscribe: () -> Unit,
    onUnsubscribe: () -> Unit,
    onClick: () -> Unit,
    avatarSize: Dp = 64.dp,
    buttonTextStyle: TextStyle = MaterialTheme.typography.labelLarge,
    buttonMaxLines: Int = 1,
    buttonOverflow: TextOverflow = TextOverflow.Ellipsis,
    showEmail: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                val avatarUrl = author.photoUrl?.let { url ->
                    if (url.contains("?")) "$url&tr=w-${avatarSize.value.toInt()},h-${avatarSize.value.toInt()}"
                    else "$url?tr=w-${avatarSize.value.toInt()},h-${avatarSize.value.toInt()}"
                } ?: ""

                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .placeholder(R.drawable.ic_default_avatar)
                            .error(R.drawable.ic_default_avatar)
                            .build()
                    ),
                    contentDescription = "Аватар автора",
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column(
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = author.nickname,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showEmail && author.email != null) {
                        Text(
                            text = author.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (isSubscribed) {
                OutlinedButton(
                    onClick = onUnsubscribe,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        "Отписаться",
                        style = buttonTextStyle,
                        maxLines = buttonMaxLines,
                        overflow = buttonOverflow
                    )
                }
            } else {
                Button(
                    onClick = onSubscribe,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        "Подписаться",
                        style = buttonTextStyle,
                        maxLines = buttonMaxLines,
                        overflow = buttonOverflow
                    )
                }
            }
        }
    }
}

// Функции для работы с подписками
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
    author: User,
    onSuccess: () -> Unit
) {
    FirebaseFirestore.getInstance().collection("subscriptions")
        .whereEqualTo("followerId", followerId)
        .whereEqualTo("authorId", author.userId)
        .get()
        .addOnSuccessListener { querySnapshot ->
            querySnapshot.documents.forEach { doc ->
                doc.reference.delete()
            }
            onSuccess()
        }
} 