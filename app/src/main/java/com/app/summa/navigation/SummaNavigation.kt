package com.app.summa.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
// PERUBAHAN: Ikon baru
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.summa.ui.screens.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dasbor", Icons.Default.Home)
    object Planner : Screen("planner", "Planner", Icons.Default.CalendarToday)
    object Habits : Screen("habits", "Habits", Icons.Default.CheckCircle)
    object Money : Screen("money", "Money", Icons.Default.AccountBalanceWallet)
    // PERUBAHAN: Ganti nama Notes menjadi Knowledge
    object Knowledge : Screen("knowledge", "Pustaka", Icons.Default.Book)
    object Reflections : Screen("reflections", "Refleksi", Icons.Default.RateReview)
}

// PERUBAHAN: Ganti nama NoteDetailRoute
object KnowledgeDetailRoute {
    const val route = "knowledge_detail/{noteId}"
    fun createRoute(noteId: Long) = "knowledge_detail/$noteId"
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
                    onClick = {
                        // FAB sekarang membuat Catatan "Inbox" baru
                        navController.navigate(KnowledgeDetailRoute.createRoute(0L))
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("Catat Cepat") }, // Ganti nama FAB
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
    val finalItems = listOf(
        Screen.Dashboard,
        Screen.Planner,
        Screen.Habits,
        // PERUBAHAN: Ganti Notes menjadi Knowledge
        Screen.Knowledge,
        Screen.Reflections
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        finalItems.forEach { screen ->
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
            DashboardScreen(
                onNavigateToPlanner = { navController.navigate(Screen.Planner.route) },
                onNavigateToMoney = { navController.navigate(Screen.Money.route) },
                // PERUBAHAN: Navigasi ke Knowledge
                onNavigateToNotes = { navController.navigate(Screen.Knowledge.route) },
                onNavigateToReflections = { navController.navigate(Screen.Reflections.route) },
                onNavigateToHabitDetail = { /* TODO: Navigasi ke detail habit */ }
            )
        }
        composable(Screen.Planner.route) {
            LaunchedEffect(Unit) { onFabVisibilityChange(true) }
            PlannerScreen()
        }
        composable(Screen.Habits.route) {
            LaunchedEffect(Unit) { onFabVisibilityChange(true) }
            HabitsScreen()
        }
        composable(Screen.Money.route) {
            LaunchedEffect(Unit) { onFabVisibilityChange(true) }
            MoneyScreen()
        }
        // PERUBAHAN: Rute untuk layar daftar catatan
        composable(Screen.Knowledge.route) {
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
        // PERUBAHAN: Rute untuk layar detail catatan
        composable(
            route = KnowledgeDetailRoute.route,
            // PERBAIKAN: Argumen harus Long, bukan String
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) {
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            KnowledgeDetailScreen(
                onBack = { navController.popBackStack() },
                onConvertToTask = { title, content ->
                    navController.popBackStack()
                    navController.navigate(Screen.Planner.route)
                }
            )
        }
    }
}