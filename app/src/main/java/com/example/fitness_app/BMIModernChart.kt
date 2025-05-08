package com.example.fitness_app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun BMIModernChart(bmiValue: Float, categories: List<BMISection>) {
    val animatedBmi by animateFloatAsState(
        targetValue = bmiValue,
        animationSpec = tween(durationMillis = 1000)
    )
    var arrowX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val yCenter = size.height / 2
            var startX = 0f
            categories.forEachIndexed { idx, section ->
                val sectionWidth = (section.rangeEnd - section.rangeStart) / 60f * totalWidth
                val isFirst = idx == 0
                val isLast = idx == categories.lastIndex
                val cornerRadius = when {
                    isFirst -> CornerRadius(12f, 0f)
                    isLast -> CornerRadius(0f, 12f)
                    else -> CornerRadius(0f, 0f)
                }
                val topLeft = Offset(startX, yCenter - 18f)
                val sizeRect = Size(sectionWidth, 36f)
                drawRoundRect(
                    color = section.color,
                    topLeft = topLeft,
                    size = sizeRect,
                    cornerRadius = cornerRadius,
                    style = Fill
                )
                startX += sectionWidth
            }
            arrowX = (animatedBmi.coerceIn(0f, 60f) / 60f) * totalWidth
        }
        val arrowXdp = with(density) { arrowX.toDp() }
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Текущий ИМТ",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .offset(x = arrowXdp - 12.dp, y = 40.dp)
                .size(24.dp)
                .rotate(180f)
        )
    }
} 