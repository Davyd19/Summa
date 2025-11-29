package com.app.summa.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.summa.ui.screens.*
import com.app.summa.ui.viewmodel.MainViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import java.net.URLEncoder

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dasbor", Icons.Default.Home)
    object Planner : Screen("planner?noteTitle={noteTitle}&noteContent={noteContent}", "Planner", Icons.Default.CalendarToday)
    object Habits : Screen("habits?habitId={habitId}", "Habits", Icons.Default.CheckCircle)
    object Money : Screen("money", "Money", Icons.Default.AccountBalanceWallet)
    object Knowledge : Screen("knowledge", "Pustaka", Icons.Default.Book)
    object Reflections : Screen("reflections", "Refleksi", Icons.Default.RateReview)
    object IdentityProfile : Screen("identity_profile", "Profil", Icons.Default.Person)
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
            // Logika seleksi: Cek apakah route dasar cocok (mengabaikan parameter "?...")
            val isSelected = currentRoute?.substringBefore("?") == screen.route.substringBefore("?")

            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = isSelected,
                onClick = {
                    // PERBAIKAN LOGIKA NAVIGASI:
                    // 1. Ambil route dasar (misal "habits" dari "habits?habitId=...")
                    val targetRoute = screen.route.substringBefore("?")

                    navController.navigate(targetRoute) {
                        // Pop up ke Start Destination (Dashboard) untuk membersihkan stack
                        // Ini penting agar tombol Back di Android berfungsi normal (keluar aplikasi di Dashboard)
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }

                        // Hindari menumpuk halaman yang sama jika diklik berkali-kali
                        launchSingleTop = true

                        // Restore state (posisi scroll, dll) jika ada
                        restoreState = true
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
            LaunchedEffect(Unit) { onFabVisibilityChange(true) }
            DashboardScreen(
                currentMode = currentMode,
                onModeSelected = onModeSelected,
                onNavigateToPlanner = { navController.navigate("planner") },
                onNavigateToMoney = { navController.navigate(Screen.Money.route) },
                onNavigateToNotes = { navController.navigate(Screen.Knowledge.route) },
                onNavigateToReflections = { navController.navigate(Screen.Reflections.route) },
                onNavigateToIdentityProfile = { navController.navigate(Screen.IdentityProfile.route) },
                onNavigateToHabitDetail = { habit ->
                    // Navigasi ke detail habit dengan argumen
                    navController.navigate("habits?habitId=${habit.id}")
                }
            )
        }

        composable(
            route = Screen.Planner.route,
            arguments = listOf(
                navArgument("noteTitle") { type = NavType.StringType; nullable = true },
                navArgument("noteContent") { type = NavType.StringType; nullable = true }
            )
        ) {
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            PlannerScreen()
        }

        composable(
            route = Screen.Habits.route,
            arguments = listOf(
                navArgument("habitId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) {
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            HabitsScreen(
                onNavigateToIdentityProfile = { navController.navigate(Screen.IdentityProfile.route) }
            )
        }

        composable(Screen.Money.route) {
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            MoneyScreen()
        }
        composable(Screen.Knowledge.route) {
            LaunchedEffect(Unit) { onFabVisibilityChange(true) }
            KnowledgeBaseScreen(
                onNoteClick = { noteId -> navController.navigate(KnowledgeDetailRoute.createRoute(noteId)) },
                onAddNoteClick = { navController.navigate(KnowledgeDetailRoute.createRoute(0L)) }
            )
        }
        composable(Screen.Reflections.route) {
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            ReflectionScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.IdentityProfile.route) {
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            IdentityProfileScreen(onBack = { navController.popBackStack() })
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