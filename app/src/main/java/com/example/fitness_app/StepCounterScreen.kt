package com.example.fitness_app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepCounterScreen() {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val firestore = FirebaseFirestore.getInstance()

    var dailyGoal by remember { mutableStateOf(10000) }
    var steps by remember { mutableStateOf(0) }
    var stepsHistory by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var hasRequiredPermissions by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val activityRecognitionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else true

        val foregroundServiceHealthGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE_HEALTH) == PackageManager.PERMISSION_GRANTED
        } else true

        hasRequiredPermissions = activityRecognitionGranted && foregroundServiceHealthGranted
    }

    LaunchedEffect(userId) {
        val snapshot = firestore.collection("users").document(userId).get().await()
        dailyGoal = snapshot.getLong("daily_step_goal")?.toInt() ?: 10000

        StepService.loadStepsHistory(context) { history ->
            stepsHistory = history
        }
    }

    LaunchedEffect(Unit) {
        if (hasRequiredPermissions) {
            ContextCompat.startForegroundService(context, Intent(context, StepService::class.java))
        }
    }

    val stepFlow = remember { StepService.stepFlow }
    LaunchedEffect(stepFlow) {
        stepFlow.collect { currentSteps ->
            steps = currentSteps
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (steps / dailyGoal.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    val animatedSteps by animateIntAsState(
        targetValue = steps,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
    )

    val animatedCalories by animateIntAsState(
        targetValue = steps / 20,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
    )

    if (!hasRequiredPermissions) {
        RequestPermissionIfNeeded()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Шагомер", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!hasRequiredPermissions) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Для работы шагомера необходимы разрешения",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                // Основной счетчик шагов
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = 1f,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 12.dp,
                                modifier = Modifier.size(220.dp)
                            )
                            CircularProgressIndicator(
                                progress = animatedProgress,
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 12.dp,
                                modifier = Modifier.size(220.dp)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$animatedSteps",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "шагов",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Статистика
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatisticItem(
                                icon = Icons.Filled.LocalFireDepartment,
                                value = "$animatedCalories",
                                label = "Калории",
                                color = Color.Red
                            )
                            StatisticItem(
                                icon = Icons.Filled.DirectionsWalk,
                                value = "$animatedSteps/$dailyGoal",
                                label = "Цель",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // График статистики
                StepHistoryChart(stepsHistory = stepsHistory)

                // Информация о пользе ходьбы
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Польза ходьбы",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            "Ходьба и бег – это не только удобные способы передвижения, но и важные составляющие здорового образа жизни. Регулярные прогулки и пробежки помогают укрепить сердечно-сосудистую систему, улучшить общее самочувствие и снизить уровень стресса.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "Калории = (шаги * 0.04) * вес (кг)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepHistoryChart(stepsHistory: List<Pair<String, Int>>) {
    if (stepsHistory.isEmpty()) return

    val maxSteps = stepsHistory.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val labelFormat = SimpleDateFormat("dd.MM", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Активность за неделю",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                stepsHistory.forEach { (date, steps) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(40.dp)
                    ) {
                        val height = (steps.toFloat() / maxSteps * 120).dp

                        Text(
                            text = "$steps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Box(
                            modifier = Modifier
                                .height(height)
                                .width(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )

                        Text(
                            text = try {
                                labelFormat.format(dateFormat.parse(date))
                            } catch (e: Exception) {
                                date.substring(5)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestPermissionIfNeeded() {
    val context = LocalContext.current
    val activityRecognitionPermission = Manifest.permission.ACTIVITY_RECOGNITION
    val foregroundServiceHealthPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Manifest.permission.FOREGROUND_SERVICE_HEALTH
    } else null

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { (permission, isGranted) ->
            Log.d("Permissions", "Разрешение $permission: $isGranted")
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            checkSelfPermission(context, activityRecognitionPermission) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(activityRecognitionPermission)
        }

        if (foregroundServiceHealthPermission != null &&
            checkSelfPermission(context, foregroundServiceHealthPermission) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(foregroundServiceHealthPermission)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}