package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.CalculatorScreen
import com.example.ui.VaultScreen
import com.example.ui.VaultViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge status and navigation bars padding support
        enableEdgeToEdge()
        
        setContent {
            val viewModel: VaultViewModel = viewModel()
            val currentTheme by viewModel.currentTheme.collectAsState()

            MyApplicationTheme(appTheme = currentTheme) {
                val navController = rememberNavController()

                // Register standard event listener for the secret PIN entry unlock trigger
                LaunchedEffect(navController) {
                    viewModel.navigateToVault.collect {
                        if (navController.currentDestination?.route != "vault") {
                            navController.navigate("vault") {
                                // Ensure back stack behavior doesn't loop navigation steps
                                launchSingleTop = true
                            }
                        }
                    }
                }

                // Register standard event listener to auto-lock the vault
                LaunchedEffect(navController) {
                    viewModel.navigateToCalculator.collect {
                        if (navController.currentDestination?.route == "vault") {
                            navController.popBackStack()
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "calculator"
                    ) {
                        composable("calculator") {
                            CalculatorScreen(viewModel = viewModel)
                        }
                        composable("vault") {
                            VaultScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
