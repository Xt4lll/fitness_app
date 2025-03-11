package com.example.fitness_app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StepCounterScreen() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var steps by remember { mutableStateOf(0) }
    var initialSteps by remember { mutableStateOf(-1f) }
    var hasStepSensor by remember { mutableStateOf(false) }

    // Проверка разрешения для Android 10+
    val activityRecognitionPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(android.Manifest.permission.ACTIVITY_RECOGNITION)
    } else null

    LaunchedEffect(Unit) {
        hasStepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    }

    val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (initialSteps < 0) {
                    initialSteps = event.values[0]
                }
                steps = (event.values[0] - initialSteps).toInt()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    DisposableEffect(Unit) {
        if (hasStepSensor && (activityRecognitionPermission?.status?.isGranted != false)) {
            val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            sensorManager.registerListener(
                sensorListener,
                stepSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasStepSensor) {
            Text("Ваше устройство не поддерживает шагомер", style = MaterialTheme.typography.bodyMedium)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            activityRecognitionPermission?.status?.isGranted == false) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Для работы шагомера необходимо разрешение")
                Button(onClick = { activityRecognitionPermission.launchPermissionRequest() }) {
                    Text("Запросить разрешение")
                }
            }
        } else {
            Text(
                text = "Шагов сегодня:",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$steps",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Продолжайте идти!",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}