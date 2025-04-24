package com.example.fitness_app

import androidx.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AuthViewModel : ViewModel() {
    fun logout(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onFailure("Пользователь не авторизован")
            return
        }

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        FirebaseFirestore.getInstance().collection("steps")
            .document("$userId-$date")
            .set(mapOf(
                "userId" to userId,
                "steps" to StepService.stepFlow.value,
                "date" to date
            ))
            .addOnSuccessListener {
                StepService.resetStepsForNewUser()
                context.stopService(Intent(context, StepService::class.java))
                FirebaseAuth.getInstance().signOut()
                context.startService(Intent(context, StepService::class.java))
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure("Ошибка сохранения данных: ${e.message}")
            }
    }
}