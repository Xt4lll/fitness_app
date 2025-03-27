package com.example.fitness_app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class StepService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var initialSteps = -1f

    companion object {
        private var _stepFlow = MutableStateFlow(0)
        val stepFlow: StateFlow<Int> = _stepFlow.asStateFlow()

        fun resetSteps(context: Context) {
            _stepFlow.tryEmit(0)
            saveSteps(context, 0)
        }

        fun saveSteps(context: Context, steps: Int) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            FirebaseFirestore.getInstance()
                .collection("steps")
                .document("$userId-$date")
                .set(mapOf("userId" to userId, "steps" to steps, "date" to date))
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Log.e("StepService", "Датчик шагов не найден!")
        } else {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }

        startForegroundService()
        scheduleDailyReset(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (initialSteps < 0) {
            initialSteps = event?.values?.get(0) ?: 0f
        }
        val stepsToday = (event?.values?.get(0) ?: 0f) - initialSteps
        _stepFlow.tryEmit(stepsToday.toInt())
        saveSteps(this, stepsToday.toInt())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val notificationChannelId = "step_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId, "Шагомер", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Шагомер работает")
            .setContentText("Шаги считаются в фоновом режиме")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    private fun scheduleDailyReset(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<DailyResetWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(getInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "dailyReset", ExistingPeriodicWorkPolicy.REPLACE, workRequest
        )
    }

    private fun getInitialDelay(): Long {
        val now = Calendar.getInstance()
        val nextMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return nextMidnight.timeInMillis - now.timeInMillis
    }
}
