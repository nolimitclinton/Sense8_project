package com.example.sense8.presentation.navgraph

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sense8.presentation.home.HomeScreen
import com.example.sense8.presentation.mainActivity.MainViewModel
import com.example.sense8.presentation.navigation.NavigationScreen
import com.example.sense8.presentation.utils.Routes

@Composable
fun NavGraph(
    startDestination: String,
    userChoice: String?,
    onUserChoice: (String) -> Unit,
    viewModel: MainViewModel,
    context: Context
) {
    val navController: NavHostController = rememberNavController()

    LaunchedEffect(userChoice) {
        Log.d(
            "NavGraph",
            "NavGraph LaunchedEffect triggered with userChoice: $userChoice and userHasChosen: ${viewModel.userHasChosen}"
        )

        if (userChoice != null && viewModel.userHasChosen) {
            when (userChoice.lowercase()) {
                "navigation" -> {
                    navController.navigate(Routes.ROUTE_NAVIGATION_SCREEN) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                    Log.d("NavGraph", "Navigation to Routes.ROUTE_NAVIGATION_SCREEN initiated")
                }

                "object detection" -> {
                    // Since object detection is the default, trigger a UI change to remove the waiting screen
                    navController.navigate("default_object_detection_screen") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                    Log.d("NavGraph", "Object detection chosen; showing the default screen")
                }

                else -> {
                    Log.w("NavGraph", "Invalid choice: $userChoice")
                    Toast.makeText(
                        context,
                        "Invalid choice. Please say 'navigation' or 'object detection'",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Log.d(
                "NavGraph",
                "No valid user choice or userHasChosen is false; staying on waiting_screen"
            )
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = "waiting_screen") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator() // Simple waiting screen with a loading spinner
            }
        }

        composable(route = "default_object_detection_screen") {
            HomeScreen(navController)
        }

        composable(route = Routes.ROUTE_NAVIGATION_SCREEN) {
            NavigationScreen()
        }
    }
}
