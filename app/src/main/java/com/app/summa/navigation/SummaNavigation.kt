package com.app.summa.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.summa.ui.screens.*
import com.app.summa.ui.viewmodel.KnowledgeViewModel
import com.app.summa.ui.viewmodel.MainViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val Dashboard = "dashboard"
    const val Planner = "planner"
    const val Habits = "habits"
    const val AddHabit = "add_habit"
    const val HabitDetail = "habit_detail/{habitId}"
    const val Money = "money"
    const val AddTransaction = "add_transaction"
    const val AddAccount = "add_account"
    const val Notes = "notes"
    const val NoteDetail = "knowledge_detail/{noteId}"
    const val Reflections = "reflections"
    const val IdentityProfile = "identity_profile"
    const val Settings = "settings"
    const val AddTask = "add_task"
    const val Focus = "focus"
}

@Composable
fun SummaApp() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val uiState by mainViewModel.uiState.collectAsState()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Screen routes that should show the bottom bar
    val bottomBarRoutes = listOf(
        Routes.Dashboard,
        "${Routes.Planner}?noteTitle={noteTitle}&noteContent={noteContent}", // Validating precise route might be tricky, checking simplified one
        Routes.Planner,
        Routes.Habits,
        Routes.Money,
        Routes.Notes,
        Routes.Reflections
    )
    
    // Simplified checker
    val showBottomBar = currentRoute in listOf(
        Routes.Dashboard,
        Routes.Habits,
        Routes.Money,
        Routes.Notes,
        Routes.Reflections
    ) || (currentRoute?.startsWith(Routes.Planner) == true)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BrutalBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Dashboard,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Dashboard) {
                DashboardScreen(
                    currentMode = uiState.currentMode,
                    onModeSelected = { mainViewModel.setMode(it) },
                    onNavigateToPlanner = { navController.navigate(Routes.Planner) },
                    onNavigateToHabitDetail = { habit -> navController.navigate("habit_detail/${habit.id}") },
                    onNavigateToMoney = { navController.navigate(Routes.Money) },
                    onNavigateToNotes = { navController.navigate(Routes.Notes) },
                    onNavigateToReflections = { navController.navigate(Routes.Reflections) },
                    onNavigateToIdentityProfile = { navController.navigate(Routes.IdentityProfile) },
                    onNavigateToSettings = { navController.navigate(Routes.Settings) },
                    onNavigateToHabits = { navController.navigate(Routes.Habits) },
                    onNavigateToAddTask = { navController.navigate(Routes.AddTask) },
                    onNavigateToFocus = { navController.navigate(Routes.Focus) },
                    onNavigateToAddTransaction = { navController.navigate(Routes.AddTransaction) }
                )
            }

            // Consolidated Planner Route
            composable(
                route = "${Routes.Planner}?noteTitle={noteTitle}&noteContent={noteContent}",
                arguments = listOf(
                    navArgument("noteTitle") { nullable = true; defaultValue = null },
                    navArgument("noteContent") { nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val noteTitle = backStackEntry.arguments?.getString("noteTitle")
                val noteContent = backStackEntry.arguments?.getString("noteContent")
                 PlannerScreen(
                    currentMode = uiState.currentMode,
                    noteTitle = noteTitle,
                    noteContent = noteContent,
                    onNavigateToAddTask = { navController.navigate(Routes.AddTask) }
                )
            }

            composable(Routes.Habits) {
                HabitsScreen(
                    onNavigateToDetail = { id -> navController.navigate("habit_detail/$id") },
                    onNavigateToIdentityProfile = { navController.navigate(Routes.IdentityProfile) },
                    onNavigateToAddHabit = { navController.navigate(Routes.AddHabit) }
                )
            }

            composable(Routes.AddHabit) {
                AddHabitScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.HabitDetail,
                arguments = listOf(navArgument("habitId") { type = NavType.LongType })
            ) {
                HabitDetailScreenDestination(
                    onBack = { navController.popBackStack() },
                    onNavigateToIdentityProfile = { navController.navigate(Routes.IdentityProfile) }
                )
            }

            composable(Routes.Money) {
                MoneyScreen(
                    onNavigateToAddTransaction = { navController.navigate(Routes.AddTransaction) },
                    onNavigateToAddAccount = { navController.navigate(Routes.AddAccount) }
                )
            }

            composable(Routes.AddTransaction) {
                AddTransactionScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.AddAccount) {
               AddAccountScreen(
                   onBack = { navController.popBackStack() }
               )
            }

            composable(Routes.Notes) {
                KnowledgeBaseScreen(
                    onNoteClick = { id -> navController.navigate("knowledge_detail/$id") },
                    onAddNoteClick = { navController.navigate("knowledge_detail/-1") }
                )
            }

            composable(
                route = Routes.NoteDetail,
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
                val viewModel: KnowledgeViewModel = hiltViewModel()
                LaunchedEffect(noteId) {
                    // If ID is -1, load 0L which signals new note in VM
                    viewModel.loadNoteDetail(if(noteId == -1L) 0L else noteId)
                }
                KnowledgeDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onConvertToTask = { title, content ->
                        val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
                        val encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8.toString())
                        navController.navigate("planner?noteTitle=$encodedTitle&noteContent=$encodedContent")
                    }
                )
            }

            composable(Routes.Reflections) {
                ReflectionScreen(
                    onBack = { navController.popBackStack() } 
                )
            }

            composable(Routes.IdentityProfile) {
                IdentityProfileScreen(
                    onBack = { navController.popBackStack() } 
                )
            }

            composable(Routes.Settings) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() } 
                )
            }

            composable(Routes.AddTask) {
                AddTaskScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.Focus) {
                UniversalFocusModeScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun BrutalBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        Triple(Routes.Dashboard, "Home", androidx.compose.material.icons.Icons.Default.Home),
        Triple(Routes.Planner, "Planner", androidx.compose.material.icons.Icons.Default.CalendarToday),
        Triple(Routes.Habits, "Habits", androidx.compose.material.icons.Icons.Default.CheckCircle),
        Triple(Routes.Money, "Money", androidx.compose.material.icons.Icons.Default.AccountBalanceWallet),
        Triple(Routes.Notes, "Pustaka", androidx.compose.material.icons.Icons.Default.Book),
        Triple(Routes.Reflections, "Refleksi", androidx.compose.material.icons.Icons.Default.RateReview)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        items.forEach { (route, title, icon) ->
            // Logic for planner matching (handles arguments)
            val selected = if (route == Routes.Planner) {
                currentRoute?.startsWith(Routes.Planner) == true
            } else {
                currentRoute == route
            }
            
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(route) },
                icon = { Icon(icon, contentDescription = title) },
                label = { Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
