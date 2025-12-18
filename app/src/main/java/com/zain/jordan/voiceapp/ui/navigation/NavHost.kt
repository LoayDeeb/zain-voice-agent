package com.zain.jordan.voiceapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zain.jordan.voiceapp.ui.screens.LiveKitCallScreen
import com.zain.jordan.voiceapp.ui.screens.LiveKitHomeScreen
import com.zain.jordan.voiceapp.viewmodel.LiveKitViewModel

object Routes {
    const val HOME = "home"
    const val CALL = "call"
}

@Composable
fun ZainNavHost() {
    val navController = rememberNavController()
    val liveKitViewModel: LiveKitViewModel = viewModel()
    
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            LiveKitHomeScreen(
                viewModel = liveKitViewModel,
                onNavigateToCall = {
                    navController.navigate(Routes.CALL)
                }
            )
        }
        
        composable(Routes.CALL) {
            LiveKitCallScreen(
                viewModel = liveKitViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
