package com.example.fitness_app

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DailyResetWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        FirebaseFirestore.getInstance().collection("steps")
            .document("$userId-$date")
            .set(mapOf("userId" to userId, "steps" to 0, "date" to date))

        //StepService.resetSteps(applicationContext)
        return Result.success()
    }
}
