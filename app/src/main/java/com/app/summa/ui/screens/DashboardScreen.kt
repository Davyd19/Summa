package com.app.summa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.DeepTeal
import com.app.summa.ui.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToHabitDetail: (com.app.summa.data.model.HabitItem) -> Unit = {},
    onNavigateToMoney: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToReflections: () -> Unit = {},
    onNavigateToIdentityProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToHabits: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showModeDialog by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val dayLabel = today.dayOfMonth.toString()
    val monthLabel = today.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.brutalBorder(strokeWidth = 2.dp, cornerRadius = 0.dp)
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, "Dashboard") },
                    label = { Text("DASHBOARD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToPlanner,
                    icon = { Icon(Icons.Default.CalendarToday, "Planner") },
                    label = { Text("PLANNER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHabits,
                    icon = { Icon(Icons.Default.CheckCircle, "Habits") },
                    label = { Text("HABITS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToIdentityProfile,
                    icon = { Icon(Icons.Default.Person, "Identity") },
                    label = { Text("IDENTITY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToMoney,
                    icon = { Icon(Icons.Default.AccountBalanceWallet, "Money") },
                    label = { Text("MONEY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToReflections,
                    icon = { Icon(Icons.Default.RateReview, "Reflection") },
                    label = { Text("REFLECTION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Date + Greeting
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    BrutalistDateDisplay(
                        day = dayLabel,
                        month = monthLabel
                    )
                    
                    // Greeting card with icons
                    Surface(
                        modifier = Modifier.brutalBorder(strokeWidth = 2.dp, cornerRadius = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Hello, Summa",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = uiState.greeting,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            IconButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Settings, "Settings", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Daily Goal Card
            item {
                BrutalistDailyGoalCard(
                    progress = uiState.todayProgress,
                    completedHabits = uiState.completedHabits,
                    totalHabits = uiState.todayHabits.size
                )
            }

            // Metric Cards Grid (Task & XP)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrutalistMetricCard(
                        value = uiState.activeTasks.toString(),
                        label = "Task",
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.surface
                    )
                    BrutalistMetricCard(
                        value = uiState.summaPoints.toString(),
                        label = "XP",
                        icon = Icons.Default.BarChart,
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color.Black,
                        contentColor = Color.White
                    )
                }
            }

            // Motivational Card
            item {
                BrutalistCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Semua Beres! 🎉",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BrutalTextButton(
                            text = "RENCANA BESOK",
                            onClick = onNavigateToPlanner
                        )
                    }
                }
            }

            // Quick Access Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = onNavigateToMoney,
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .brutalBorder(strokeWidth = 2.dp, cornerRadius = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = DeepTeal,
                        contentColor = Color.White
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(uiState.totalNetWorth),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Surface(
                        onClick = onNavigateToNotes,
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .brutalBorder(strokeWidth = 2.dp, cornerRadius = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Book, null, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Notes",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Bottom Action Button - Full Width
            item {
                BrutalistLargeButton(
                    text = "Catat Cepat",
                    onClick = onNavigateToPlanner,
                    icon = Icons.Default.Add,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showModeDialog) {
        BrutalistModeDialog(
            currentMode = currentMode,
            onDismiss = { showModeDialog = false },
            onModeSelected = {
                onModeSelected(it)
                showModeDialog = false
            }
        )
    }
}
