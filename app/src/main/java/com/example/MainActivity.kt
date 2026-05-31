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
import com.example.ui.theme.WarmSlate
import com.example.ui.theme.BloodCrimson
import com.example.viewmodel.BloodConnectViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight

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

                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                val userRole by viewModel.userRole.collectAsState()

                // Security Session Router: Instantly throw back to login if logged out
                LaunchedEffect(isLoggedIn, currentRoute) {
                    if (currentRoute != null && !isLoggedIn && currentRoute != "splash" && currentRoute != "login") {
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                }

                val showBottomBar = currentRoute != "splash" && currentRoute != "login"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                modifier = Modifier.testTag("app_navigation_bar"),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                // Raw navigation options
                                val navItems = mutableListOf(
                                    NavigationBarItemData("home", "Home", Icons.Default.Dashboard, "Home Navigation"),
                                    NavigationBarItemData("finder", "Find Donors", Icons.Default.Search, "Finder Navigation"),
                                    NavigationBarItemData("requests", "Requests", Icons.Default.Bloodtype, "Requests Navigation"),
                                    NavigationBarItemData("profile", "Profile", Icons.Default.AccountCircle, "Profile Navigation"),
                                    NavigationBarItemData("rewards", "Rewards", Icons.Default.EmojiEvents, "Rewards Navigation")
                                )

                                // Conditional Admin Panel constraint: ONLY show if user has "Admin" role
                                if (userRole == "Admin") {
                                    navItems.add(
                                        NavigationBarItemData("admin", "Admin Panel", Icons.Default.AdminPanelSettings, "Admin Navigation")
                                    )
                                }

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
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash", // Boot into beautiful animated Splash
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("splash") {
                            SplashScreen(onNavigateToNext = {
                                if (isLoggedIn) {
                                    navController.navigate("home") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            })
                        }
                        composable("login") {
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("home") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToFinder = { navController.navigate("finder") },
                                onNavigateToRequests = { navController.navigate("requests") },
                                onNavigateToRegister = { navController.navigate("profile") }
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
                        composable("profile") {
                            RegistrationScreen(
                                viewModel = viewModel,
                                onNavigateToDashboard = { navController.navigate("home") }
                            )
                        }
                        composable("rewards") {
                            RewardsScreen(
                                viewModel = viewModel,
                                onNavigateToRegister = { navController.navigate("profile") }
                            )
                        }
                        composable("admin") {
                            // Defensive protection: if non-admin tries to navigate directly
                            if (userRole == "Admin") {
                                AdminScreen(viewModel = viewModel)
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(WarmSlate),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Unauthorized Access", color = BloodCrimson, fontWeight = FontWeight.Bold)
                                }
                            }
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
