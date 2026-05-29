package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BloodConnectViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize main ViewModel
                val viewModel: BloodConnectViewModel = viewModel()
                
                // Set up type-safe navigation composite
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("app_navigation_bar"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            val navItems = listOf(
                                NavigationBarItemData("home", "Home", Icons.Default.Dashboard, "Home Navigation"),
                                NavigationBarItemData("finder", "Find Donors", Icons.Default.Search, "Finder Navigation"),
                                NavigationBarItemData("requests", "Requests", Icons.Default.Bloodtype, "Requests Navigation"),
                                NavigationBarItemData("register", "QR Card", Icons.Default.QrCode, "Register Navigation"),
                                NavigationBarItemData("rewards", "Rewards", Icons.Default.EmojiEvents, "Rewards Navigation"),
                                NavigationBarItemData("admin", "Admin Panel", Icons.Default.AdminPanelSettings, "Admin Navigation")
                            )

                            navItems.forEach { item ->
                                val isSelected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.contentDescription
                                        )
                                    },
                                    label = { Text(item.label, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("nav_tab_${item.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToFinder = { navController.navigate("finder") },
                                onNavigateToRequests = { navController.navigate("requests") },
                                onNavigateToRegister = { navController.navigate("register") }
                            )
                        }
                        composable("finder") {
                            DonorFinderScreen(viewModel = viewModel)
                        }
                        composable("requests") {
                            RequestsScreen(
                                viewModel = viewModel,
                                onNavigateToFinder = { navController.navigate("finder") }
                            )
                        }
                        composable("register") {
                            RegistrationScreen(
                                viewModel = viewModel,
                                onNavigateToDashboard = { navController.navigate("home") }
                            )
                        }
                        composable("rewards") {
                            RewardsScreen(
                                viewModel = viewModel,
                                onNavigateToRegister = { navController.navigate("register") }
                            )
                        }
                        composable("admin") {
                            AdminScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

data class NavigationBarItemData(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val contentDescription: String
)

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
