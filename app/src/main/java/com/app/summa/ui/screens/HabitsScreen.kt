package com.app.summa.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.HabitLog
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.HabitViewModel
// PENAMBAHAN: Import model UI dari file baru
import com.app.summa.ui.model.HabitItem
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// PERBAIKAN: Definisi 'data class HabitItem' sudah dipindahkan ke file terpisah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: HabitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (uiState.selectedHabit != null) {
        HabitDetailScreen(
            habit = uiState.selectedHabit!!,
            onBack = { viewModel.onBackFromDetail() },
            logs = uiState.habitLogs
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
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    items(uiState.habits) { habit ->
                        HabitListItem(
                            habit = habit,
                            onClick = { viewModel.selectHabit(habit) },
                            onIncrement = { viewModel.incrementHabit(habit) },
                            onDecrement = { viewModel.decrementHabit(habit) }
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, icon, target ->
                viewModel.addHabit(name, icon, target)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, icon: String, target: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🎯") }
    var target by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Kebiasaan Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kebiasaan") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("Ikon (Emoji)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target Hitungan Harian") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(name, icon, target.toIntOrNull() ?: 1)
                }
            ) {
                Text("Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HabitListItem(
    habit: HabitItem,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val isComplete = habit.targetCount > 0 && habit.currentCount >= habit.targetCount
    val isOverAchieved = habit.targetCount > 0 && habit.currentCount > habit.targetCount

    SummaCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> -height } + fadeOut())
                    },
                    label = "count_animation"
                ) { count ->
                    Text(
                        text = if (habit.targetCount > 1) "$count / ${habit.targetCount}" else "$count",
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
    onBack: () -> Unit,
    logs: List<HabitLog>
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(habit.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaCard(modifier = Modifier.weight(1f)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
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

            item {
                Text(
                    "Kalender Heatmap",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // PERBAIKAN: Mengganti VStack dengan Column
            item {
                HabitHeatmap(logs = logs, targetCount = habit.targetCount)
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun HabitHeatmap(
    logs: List<HabitLog>,
    targetCount: Int
) {
    val today = LocalDate.now()
    val weeks = 12
    val daysToShow = weeks * 7

    val logsMap = remember(logs) {
        logs.associateBy { LocalDate.parse(it.date) }
    }

    val data = remember(logsMap) {
        (0 until daysToShow).map { dayOffset ->
            val date = today.minusDays(dayOffset.toLong())
            val log = logsMap[date]
            val count = log?.count ?: 0

            val value = when {
                count <= 0 -> 0 // Tidak ada log
                targetCount > 0 && count < targetCount -> 1 // Parsial
                targetCount > 0 && count == targetCount -> 2 // Sempurna
                targetCount > 0 && count > targetCount -> 3 // Over-achievement
                targetCount == 0 && count > 0 -> 2 // Selesai (untuk habit checklist)
                else -> 0
            }
            date to value
        }.reversed()
    }

    SummaCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (0..2).map { i ->
                    val date = today.minusWeeks((weeks - 1 - i * (weeks / 3)).toLong())
                    Text(
                        text = date.month.getDisplayName(TextStyle.SHORT, Locale("id")),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (0..6).forEach { dayOfWeek ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (0 until weeks).forEach { week ->
                            val index = (week * 7) + dayOfWeek
                            if (index < data.size) {
                                val (_, value) = data[index]
                                HeatmapCell(value = value)
                            } else {
                                Box(Modifier.size(14.dp)) // Placeholder
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Lebih sedikit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    HeatmapCell(value = 0)
                    HeatmapCell(value = 1)
                    HeatmapCell(value = 2)
                    HeatmapCell(value = 3)
                }
                Spacer(Modifier.width(4.dp))
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
    val cellSize = 14.dp
    val color = when (value) {
        0 -> MaterialTheme.colorScheme.surfaceVariant // Gagal
        1 -> DeepTeal.copy(alpha = 0.3f) // Parsial
        2 -> DeepTeal.copy(alpha = 0.8f) // Sempurna
        else -> GoldAccent // Over-achievement (value == 3)
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.extraSmall
            )
    )
}