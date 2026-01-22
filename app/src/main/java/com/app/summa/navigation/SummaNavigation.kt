package com.app.summa.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
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
import com.app.summa.ui.components.MorningBriefingDialog
import com.app.summa.ui.components.LevelUpDialog // Import Komponen Baru
import com.app.summa.ui.viewmodel.MainViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import java.net.URLEncoder

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dasbor", Icons.Default.Home)
    object Planner : Screen("planner?noteTitle={noteTitle}&noteContent={noteContent}", "Planner", Icons.Default.CalendarToday)
    object Habits : Screen("habits", "Habits", Icons.Default.CheckCircle)
    object Money : Screen("money", "Money", Icons.Default.AccountBalanceWallet)
    object Knowledge : Screen("knowledge", "Pustaka", Icons.Default.Book)
    object Reflections : Screen("reflections", "Refleksi", Icons.Default.RateReview)

    object IdentityProfile : Screen("identity_profile", "Profil", Icons.Default.Person)
    object HabitDetail : Screen("habit_detail/{habitId}", "Detail Kebiasaan", Icons.Default.Edit) {
        fun createRoute(habitId: Long) = "habit_detail/$habitId"
    }
}

object KnowledgeDetailRoute {
    const val route = "knowledge_detail/{noteId}"
    fun createRoute(noteId: Long) = "knowledge_detail/$noteId"
}

fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun SummaApp() {
    val navController = rememberNavController()
    var showFab by remember { mutableStateOf(false) }

    val mainViewModel: MainViewModel = hiltViewModel()
    val mainUiState by mainViewModel.uiState.collectAsState()

    // --- DIALOG GLOBAL (SYSTEM EVENTS) ---

    // 1. Level Up Celebration (Prioritas Visual Tinggi)
    if (mainUiState.levelUpEvent != null) {
        LevelUpDialog(
            event = mainUiState.levelUpEvent!!,
            onDismiss = { mainViewModel.dismissLevelUp() }
        )
    }

    // 2. Morning Briefing (Laporan Harian)
    if (mainUiState.morningBriefing != null) {
        MorningBriefingDialog(
            data = mainUiState.morningBriefing!!,
            onDismiss = { mainViewModel.dismissBriefing() }
        )
    }
    // -------------------------------------

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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .brutalBorder(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            finalItems.forEach { screen ->
                val screenBaseRoute = screen.route.substringBefore("?")
                val currentBaseRoute = currentRoute?.substringBefore("?")
                val isSelected = currentBaseRoute == screenBaseRoute

                val onItemClick = {
                    val targetRoute = screen.route.substringBefore("?")
                    if (isSelected) {
                        navController.popBackStack(targetRoute, false)
                    } else {
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                val isDetailScreen = currentRoute?.let { route ->
                                    route.contains("detail") ||
                                            route.contains("profile") ||
                                            route.contains("settings")
                                } == true

                                saveState = !isDetailScreen
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .brutalBorder(
                            color = MaterialTheme.colorScheme.onBackground,
                            strokeWidth = 2.dp
                        )
                        .clickable { onItemClick() }
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        screen.icon,
                        contentDescription = screen.title,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = screen.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.brutalBorder(
    strokeWidth: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.onBackground
): Modifier = this.border(
    width = strokeWidth,
    color = color,
    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
)

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
                onNavigateToPlanner = { navController.navigateToTab("planner") },
                onNavigateToMoney = { navController.navigateToTab(Screen.Money.route) },
                onNavigateToNotes = { navController.navigateToTab(Screen.Knowledge.route) },
                onNavigateToReflections = { navController.navigateToTab(Screen.Reflections.route) },
                onNavigateToIdentityProfile = { navController.navigate(Screen.IdentityProfile.route) },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHabits = { navController.navigateToTab(Screen.Habits.route) },
                onNavigateToHabitDetail = { habit ->
                    navController.navigate(Screen.HabitDetail.createRoute(habit.id))
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
            PlannerScreen(currentMode = currentMode)
        }

        composable(route = Screen.Habits.route) {
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            HabitsScreen(
                onNavigateToDetail = { habitId ->
                    navController.navigate(Screen.HabitDetail.createRoute(habitId))
                },
                onNavigateToIdentityProfile = { navController.navigate(Screen.IdentityProfile.route) }
            )
        }

        composable(
            route = Screen.HabitDetail.route,
            arguments = listOf(navArgument("habitId") { type = NavType.LongType })
        ) {
            LaunchedEffect(Unit) { onFabVisibilityChange(false) }
            HabitDetailScreenDestination(
                onBack = { navController.popBackStack() },
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
        composable("settings") {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
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
                    navController.navigate("planner?noteTitle=$encodedTitle&noteContent=$encodedContent") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}