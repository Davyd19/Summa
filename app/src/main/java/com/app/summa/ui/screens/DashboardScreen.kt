package com.app.summa.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.HabitItem
import com.app.summa.data.model.Task
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.BrutalBlack
import com.app.summa.ui.theme.BrutalBlue
import com.app.summa.ui.theme.BrutalWhite
import com.app.summa.ui.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToHabitDetail: (HabitItem) -> Unit = {},
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
    val dayLabel = today.dayOfMonth.toString().padStart(2, '0')
    val monthLabel = today.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BrutalistHeader(
            day = dayLabel,
            month = monthLabel,
            greeting = uiState.greeting,
            currentMode = currentMode,
            onModeClick = { showModeDialog = true },
            onSettingsClick = onNavigateToSettings
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrutalistDailyGoalCard(
                progress = uiState.todayProgress,
                completedHabits = uiState.completedHabits,
                totalHabits = uiState.todayHabits.size
            )

            BrutalistStatGrid(
                tasksLeft = uiState.activeTasks,
                points = uiState.summaPoints,
                paperclips = uiState.totalPaperclips
            )

            BrutalistNextActionCard(
                task = uiState.nextTask,
                onPrimaryAction = onNavigateToPlanner,
                onProfileClick = onNavigateToIdentityProfile
            )

            BrutalistHabitsSection(
                habits = uiState.todayHabits,
                onHabitClick = onNavigateToHabitDetail
            )

            BrutalistQuickAccessRow(
                netWorth = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(uiState.totalNetWorth),
                onMoneyClick = onNavigateToMoney,
                onNotesClick = onNavigateToNotes,
                onReflectionClick = onNavigateToReflections
            )
        }

        BrutalistActionSection(
            onStartSession = onNavigateToPlanner,
            onEditHabits = onNavigateToHabits,
            onHistoryClick = onNavigateToReflections
        )
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

@Composable
private fun BrutalistHeader(
    day: String,
    month: String,
    greeting: String,
    currentMode: String,
    onModeClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = day,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = MaterialTheme.typography.displayMedium.lineHeight * 0.9f
            )
            Text(
                text = month,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .shadow(0.dp)
                .brutalBorder(),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column {
                    Text(
                        text = "Hello, Summa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                IconButton(
                    onClick = onModeClick,
                    modifier = Modifier
                        .size(40.dp)
                        .brutalBorder()
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Mode")
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .brutalBorder()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
    }
}


// Removed local definitions as they are now in BrutalistUi.kt

