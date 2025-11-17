package com.app.summa.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.*
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class HabitItem(
    val id: Long,
    val name: String,
    val icon: String,
    val currentCount: Int,
    val targetCount: Int,
    val totalSum: Int,
    val currentStreak: Int,
    val perfectStreak: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen() {
    var selectedHabit by remember { mutableStateOf<HabitItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val habits = remember {
        mutableStateListOf(
            HabitItem(1, "Latihan Fisik", "💪", 3, 3, 450, 45, 7),
            HabitItem(2, "Membaca Quran", "📖", 1, 3, 285, 30, 5),
            HabitItem(3, "Menulis Jurnal", "✍️", 1, 1, 120, 20, 10),
            HabitItem(4, "Meditasi", "🧘", 0, 3, 90, 15, 3)
        )
    }

    if (selectedHabit != null) {
        HabitDetailScreen(
            habit = selectedHabit!!,
            onBack = { selectedHabit = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Kebiasaan",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Konsistensi adalah kunci",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Habit")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                items(habits) { habit ->
                    HabitListItem(
                        habit = habit,
                        onClick = { selectedHabit = habit },
                        onIncrement = {
                            val index = habits.indexOf(habit)
                            habits[index] = habit.copy(
                                currentCount = (habit.currentCount + 1).coerceAtMost(habit.targetCount + 3)
                            )
                        },
                        onDecrement = {
                            val index = habits.indexOf(habit)
                            habits[index] = habit.copy(
                                currentCount = (habit.currentCount - 1).coerceAtLeast(0)
                            )
                        }
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HabitListItem(
    habit: HabitItem,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val isComplete = habit.currentCount >= habit.targetCount
    val isOverAchieved = habit.currentCount > habit.targetCount

    SummaCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isOverAchieved -> GoldAccent.copy(alpha = 0.2f)
                            isComplete -> SuccessGreen.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = habit.icon,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(Modifier.width(16.dp))

            // Name and Streak
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔥 ${habit.currentStreak}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningOrange
                    )
                    if (habit.perfectStreak > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "👑 ${habit.perfectStreak}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent
                        )
                    }
                }
            }

            // Counter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onDecrement,
                    enabled = habit.currentCount > 0
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = if (habit.currentCount > 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                AnimatedContent(
                    targetState = habit.currentCount,
                    transitionSpec = {
                        slideInVertically { -it } + fadeIn() with
                                slideOutVertically { it } + fadeOut()
                    }
                ) { count ->
                    Text(
                        text = "$count / ${habit.targetCount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isOverAchieved -> GoldAccent
                            isComplete -> SuccessGreen
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                IconButton(onClick = onIncrement) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = when {
                            isOverAchieved -> GoldAccent
                            isComplete -> SuccessGreen
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habit: HabitItem,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(habit.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Edit */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { /* Delete */ }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Total Sum
            item {
                SummaCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "HITUNGAN SUMMA",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${habit.totalSum}",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepTeal
                        )
                        Text(
                            text = "Total sepanjang masa",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Streaks
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaCard(modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔥", style = MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${habit.currentStreak}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Streak Konsistensi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    SummaCard(modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👑", style = MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${habit.perfectStreak}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                            Text(
                                "Streak Sempurna",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Heatmap
            item {
                Text(
                    "Kalender Heatmap",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                HabitHeatmap(habitId = habit.id)
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun HabitHeatmap(habitId: Long) {
    // Generate dummy data for last 8 weeks
    val today = LocalDate.now()
    val weeks = 8
    val data = remember {
        (0 until weeks * 7).map { dayOffset ->
            val date = today.minusDays(dayOffset.toLong())
            val value = (0..4).random()
            date to value
        }.reversed()
    }

    SummaCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Month labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (weekOffset in 0 until weeks step 2) {
                    val date = today.minusWeeks(weekOffset.toLong())
                    Text(
                        text = date.month.getDisplayName(TextStyle.SHORT, Locale("id")),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Heatmap grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                data.chunked(7).forEach { week ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { (date, value) ->
                            HeatmapCell(value = value)
                        }
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Lebih sedikit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    HeatmapCell(value = 0)
                    HeatmapCell(value = 1)
                    HeatmapCell(value = 2)
                    HeatmapCell(value = 3)
                    HeatmapCell(value = 4)
                }
                Text(
                    "Lebih banyak",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun HeatmapCell(value: Int) {
    val color = when (value) {
        0 -> MaterialTheme.colorScheme.surfaceVariant
        1 -> DeepTeal.copy(alpha = 0.3f)
        2 -> DeepTeal.copy(alpha = 0.5f)
        3 -> DeepTeal.copy(alpha = 0.8f)
        else -> GoldAccent // Over-achievement
    }

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.extraSmall
            )
    )
}