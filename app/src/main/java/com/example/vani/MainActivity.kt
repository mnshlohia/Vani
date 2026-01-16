package com.example.vani

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.vani.presentation.HomeScreen
import com.example.vani.presentation.HomeViewModel
import com.example.vani.presentation.HomeViewModelFactory
import com.example.vani.presentation.PlayerScreen
import com.example.vani.presentation.PlayerViewModel
import com.example.vani.presentation.PlayerViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VaniApp()
        }
    }
}

@Composable
fun VaniApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val context = androidx.compose.ui.platform.LocalContext.current
            val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(context))
            HomeScreen(navController = navController, viewModel = viewModel)
        }

        composable(
            route = "player/{url}",
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: return@composable
            val context = androidx.compose.ui.platform.LocalContext.current
            val viewModel: PlayerViewModel = viewModel(factory = PlayerViewModelFactory(context))
            PlayerScreen(url = url, viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
