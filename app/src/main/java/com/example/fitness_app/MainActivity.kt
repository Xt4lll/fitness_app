package com.example.fitness_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.fitness_app.ui.theme.*
import com.example.fitness_app.PlaylistVideosScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FitnessAppTheme {
                FitnessApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessApp() {
    var showSplash by remember { mutableStateOf(true) }
    val navController = rememberNavController()
    var userId by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid) }
    val colorScheme = MaterialTheme.colorScheme

    if (showSplash) {
        SplashScreen(onSplashFinished = { showSplash = false })
    } else if (userId == null) {
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
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        listOf(
                            NavigationItem("profile", Icons.Default.Person, "Профиль"),
                            NavigationItem("goals", Icons.Default.FitnessCenter, "Цели"),
                            NavigationItem("step_counter", Icons.Default.DirectionsWalk, "Шагомер"),
                            NavigationItem("authors", Icons.Default.People, "Авторы")
                        ).forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                )
                            )
                        }
                    }
                }
            },
            containerColor = colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.background)
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "profile",
                    modifier = Modifier.fillMaxSize()
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
                    composable("playlist_videos/{playlistId}") { backStackEntry ->
                        val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
                        PlaylistVideosScreen(playlistId = playlistId, navController = navController)
                    }
                }
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)