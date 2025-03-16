package com.example.fitness_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FitnessApp()
        }
    }
}

@Composable
fun FitnessApp() {
    val navController = rememberNavController()
    var userId by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid) }

    // Если пользователь не авторизован, перенаправляем на экран авторизации
    if (userId == null) {
        AuthScreen { newUserId ->
            userId = newUserId
        }
    } else {
        // Основной интерфейс с NavigationBar (Material 3)
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Элементы NavigationBar
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Профиль") },
                        label = { Text("Профиль") },
                        selected = currentRoute == "profile",
                        onClick = {
                            navController.navigate("profile") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Цели") },
                        label = { Text("Цели") },
                        selected = currentRoute == "goals",
                        onClick = {
                            navController.navigate("goals") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DirectionsWalk, contentDescription = "Шагомер") },
                        label = { Text("Шагомер") },
                        selected = currentRoute == "step_counter",
                        onClick = {
                            navController.navigate("step_counter") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "profile",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("profile") {
                    ProfileScreen(
                        userId = userId!!,
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()
                            userId = null
                        },
                        navController = navController
                    )
                }
                composable("bmi_calculator") {
                    BMICalculatorScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("goals") {
                    FitnessGoalsScreen()
                }
                composable("step_counter") {
                    StepCounterScreen()
                }
            }
        }
    }
}