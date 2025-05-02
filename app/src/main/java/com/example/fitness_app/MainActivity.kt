package com.example.fitness_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitness_app.VideoPlayerScreen

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

    if (userId == null) {
        AuthScreen { newUserId ->
            userId = newUserId
        }
    } else {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isVideoPlayerScreen = currentRoute?.startsWith("video_player") == true
        Scaffold(
            bottomBar = {
                if (!isVideoPlayerScreen) {
                    NavigationBar {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

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

                        NavigationBarItem(
                            icon = { Icon(Icons.Default.People, contentDescription = "Авторы") },
                            label = { Text("Авторы") },
                            selected = currentRoute == "authors",
                            onClick = {
                                navController.navigate("authors") {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
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
                composable("authors") {
                    AuthorsScreen(
                        userId = userId!!,
                        navController = navController
                    )
                }
                composable("subscriptions") {
                    SubscriptionsScreen(userId = userId!!, navController = navController)
                }
                composable("video_player/{videoId}") { backStackEntry ->
                    val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
                    VideoPlayerScreen(videoId = videoId, navController = navController)
                }
                composable("creative_studio") {
                    CreativeStudioScreen(userId = userId!!, navController = navController)
                }
                composable("add_video") {
                    AddVideoScreen(userId = userId!!, navController = navController)
                }
                composable("author_videos/{authorId}") { backStackEntry ->
                    val authorId = backStackEntry.arguments?.getString("authorId") ?: return@composable
                    AuthorVideosScreen(authorId = authorId, navController = navController)
                }
                composable("videos/tag/{tag}") { backStackEntry ->
                    val tag = backStackEntry.arguments?.getString("tag") ?: return@composable
                    TagVideosScreen(tag = tag, navController = navController)
                }
            }
        }
    }
}