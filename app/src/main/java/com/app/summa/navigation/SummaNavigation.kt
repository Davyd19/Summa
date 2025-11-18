package com.app.summa.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.summa.ui.screens.*
import com.app.summa.ui.viewmodel.MainViewModel
import java.net.URLEncoder

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dasbor", Icons.Default.Home)
    object Planner : Screen("planner?noteTitle={noteTitle}&noteContent={noteContent}", "Planner", Icons.Default.CalendarToday)
    object Habits : Screen("habits", "Habits", Icons.Default.CheckCircle)
    object Money : Screen("money", "Money", Icons.Default.AccountBalanceWallet)
    object Knowledge : Screen("knowledge", "Pustaka", Icons.Default.Book)
    object Reflections : Screen("reflections", "Refleksi", Icons.Default.RateReview)
}

object KnowledgeDetailRoute {
    const val route = "knowledge_detail/{noteId}"
    fun createRoute(noteId: Long) = "knowledge_detail/$noteId"
}


@Composable
fun SummaApp() {
    val navController = rememberNavController()
    var showFab by remember { mutableStateOf(false) }

    val mainViewModel: MainViewModel = hiltViewModel()
    val mainUiState by mainViewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                currentMode = mainUiState.currentMode
            )
        },
        floatingActionButton = {
            if (showFab) {
                ExtendedFloatingActionButton(
                    onClick = {
                        navController.navigate(KnowledgeDetailRoute.createRoute(0L))
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("Catat Cepat") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        NavigationGraph(
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            onFabVisibilityChange = { showFab = it },
            currentMode = mainUiState.currentMode,
            onModeSelected = { mainViewModel.setMode(it) }
        )
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    currentMode: String
) {
    val baseItems = listOf(
        Screen.Dashboard,
        Screen.Planner,
        Screen.Habits,
        Screen.Knowledge,
        Screen.Money,
        Screen.Reflections
    )

    val finalItems = when (currentMode) {
        "Fokus" -> listOf(Screen.Dashboard, Screen.Planner, Screen.Knowledge)
        "Pagi" -> listOf(Screen.Dashboard, Screen.Habits, Screen.Reflections)
        else -> baseItems
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        finalItems.forEach { screen ->
            val isSelected = currentRoute?.startsWith(screen.route.substringBefore("?")) ?: false

            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        val route = if (screen == Screen.Planner) "planner" else screen.route
                        navController.navigate(route) {
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
    onFabVisibilityChange: (Boolean) -> Unit,
    currentMode: String,
    onModeSelected: (String) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            // FAB TAMPIL DI DASHBOARD
            LaunchedEffect(Unit) { onFabVisibilityChange(true) }
            DashboardScreen(
                currentMode = currentMode,
                onModeSelected = onModeSelected,
                onNavigateToPlanner = { navController.navigate("planner") },
                onNavigateToMoney = { navController.navigate(Screen.Money.route) },
                onNavigateToNotes = { navController.navigate(Screen.Knowledge.route) },
                onNavigateToReflections = { navController.navigate(Screen.Reflections.route) },
                onNavigateToHabitDetail = { /* TODO */ }
            )
        }
        composable(
            route = Screen.Planner.route,
            arguments = listOf(
                navArgument("noteTitle") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("noteContent") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {
            // FAB DISEMBUNYIKAN DI PLANNER (Planner punya tombol tambah sendiri)
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            PlannerScreen()
        }
        composable(Screen.Habits.route) {
            // FAB DISEMBUNYIKAN DI HABITS
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            HabitsScreen()
        }
        composable(Screen.Money.route) {
            // FAB DISEMBUNYIKAN DI MONEY
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            MoneyScreen()
        }
        composable(Screen.Knowledge.route) {
            // FAB TAMPIL DI KNOWLEDGE (Untuk tambah catatan)
            LaunchedEffect(Unit) { onFabVisibilityChange(true) }
            KnowledgeBaseScreen(
                onNoteClick = { noteId ->
                    navController.navigate(KnowledgeDetailRoute.createRoute(noteId))
                },
                onAddNoteClick = {
                    navController.navigate(KnowledgeDetailRoute.createRoute(0L))
                }
            )
        }
        composable(Screen.Reflections.route) {
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            ReflectionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = KnowledgeDetailRoute.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) {
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            KnowledgeDetailScreen(
                onBack = { navController.popBackStack() },
                onConvertToTask = { title, content ->
                    val encodedTitle = URLEncoder.encode(title, "UTF-8")
                    val encodedContent = URLEncoder.encode(content, "UTF-8")
                    navController.popBackStack()
                    navController.navigate("planner?noteTitle=$encodedTitle&noteContent=$encodedContent")
                }
            )
        }
    }
}