package com.app.summa.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.summa.app.ui.screens.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dasbor", Icons.Default.Home)
    object Planner : Screen("planner", "Planner", Icons.Default.CalendarToday)
    object Habits : Screen("habits", "Habits", Icons.Default.CheckCircle)
    object Money : Screen("money", "Money", Icons.Default.AccountBalanceWallet)
}

@Composable
fun SummaApp() {
    val navController = rememberNavController()
    var showFab by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) },
        floatingActionButton = {
            if (showFab) {
                ExtendedFloatingActionButton(
                    onClick = { /* Quick Add */ },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("Quick Add") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        NavigationGraph(
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            onFabVisibilityChange = { showFab = it }
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Dashboard,
        Screen.Planner,
        Screen.Habits,
        Screen.Money
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onFabVisibilityChange: (Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            LaunchedEffect(Unit) { onFabVisibilityChange(true) }
            DashboardScreen()
        }
        composable(Screen.Planner.route) {
            LaunchedEffect(Unit) { onFabVisibilityChange(true) }
            PlannerScreen()
        }
        composable(Screen.Habits.route) {
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            HabitsScreen()
        }
        composable(Screen.Money.route) {
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            MoneyScreen()
        }
    }
}