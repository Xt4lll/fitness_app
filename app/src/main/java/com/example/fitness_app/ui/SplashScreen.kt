package com.example.fitness_app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(true) {
        scale.animateTo(1f, animationSpec = tween(900))
        alpha.animateTo(1f, animationSpec = tween(900))
        delay(1200)
        alpha.animateTo(0f, animationSpec = tween(400))
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Rep",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Riot",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
            )
        }
    }
} 