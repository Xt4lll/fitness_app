package com.example.fitness_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            var userId by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid) }

            NavHost(navController = navController, startDestination = if (userId != null) "profile" else "auth") {
                composable("auth") {
                    AuthScreen { newUserId ->
                        userId = newUserId
                        navController.navigate("profile") { popUpTo("auth") { inclusive = true } }
                    }
                }
                composable("profile") {
                    if (userId != null) {
                        ProfileScreen(
                            userId = userId!!,
                            onLogout = {
                                FirebaseAuth.getInstance().signOut()
                                userId = null
                                navController.navigate("auth") { popUpTo("profile") { inclusive = true } }
                            }
                        )
                    }
                }
            }
        }
    }
}
