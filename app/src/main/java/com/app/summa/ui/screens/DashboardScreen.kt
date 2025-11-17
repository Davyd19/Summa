package com.app.summa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.ui.components.*
import com.app.summa.ui.model.HabitItem
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToHabitDetail: (HabitItem) -> Unit = {},
    onNavigateToMoney: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    // PENAMBAHAN: Parameter navigasi baru
    onNavigateToReflections: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showModeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${uiState.greeting}, Davyd!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Target Ringkasan Hari Ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent
                        )
                    }
                },
                actions = {
                    // PENAMBAHAN: Tombol untuk Tinjauan Harian
                    IconButton(onClick = onNavigateToReflections) {
                        Icon(Icons.Default.RateReview, contentDescription = "Tinjauan Harian")
                    }
                    FilledTonalButton(
                        onClick = { showModeDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(uiState.currentMode)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                // Daily Summary Card
                item {
                    SummaCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Progres Hari Ini",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Hitungan Summa: ${uiState.summaPoints} poin",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            ProgressCircle(
                                progress = uiState.todayProgress,
                                size = 80.dp,
                                strokeWidth = 8.dp,
                                progressColor = GoldAccent
                            )
                        }
                    }
                }

                // Next Action Card
                item {
                    Text(
                        text = "Hal Berikutnya",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    val nextTask = uiState.nextTask
                    SummaCard(
                        onClick = {
                            if (nextTask != null) {
                                // TODO: Navigasi ke Focus Mode
                            } else {
                                onNavigateToPlanner()
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (nextTask != null) Icons.Default.PlayArrow else Icons.Default.Add,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (nextTask != null) {
                                    Text(
                                        text = nextTask.scheduledTime ?: "Sepanjang hari",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = nextTask.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "Tidak ada tugas berikutnya",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Ketuk untuk menambah",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            if (nextTask != null) {
                                Button(onClick = { /* TODO: Navigasi ke Focus Mode */ }) {
                                    Text("Mulai Fokus")
                                }
                            }
                        }
                    }
                }

                // Quick Habits
                item {
                    Text(
                        text = "Kebiasaan Hari Ini",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    HabitQuickList(
                        habits = uiState.todayHabits,
                        onHabitClick = { onNavigateToHabitDetail(it) }
                    )
                }

                // Quick Widgets
                item {
                    Text(
                        text = "Ringkasan Modul",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Money Widget
                        SummaCard(
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToMoney() }
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                            Spacer(Modifier.height(8.dp))
                            Text("Keuangan", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(uiState.totalNetWorth),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Notes Widget
                        SummaCard(
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToNotes() }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null)
                            Spacer(Modifier.height(8.dp))
                            Text("Catatan", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Lihat semua ide",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showModeDialog) {
        ModeSelectionDialog(
            currentMode = uiState.currentMode,
            onDismiss = { showModeDialog = false },
            onModeSelected = {
                viewModel.setMode(it)
                showModeDialog = false
            }
        )
    }
}

@Composable
fun HabitQuickList(
    habits: List<HabitItem>,
    onHabitClick: (HabitItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        habits.take(3).forEach { habit ->
            HabitQuickItem(
                habit = habit,
                onClick = { onHabitClick(habit) }
            )
        }
    }
}

@Composable
fun HabitQuickItem(
    habit: HabitItem,
    onClick: () -> Unit
) {
    val isComplete = habit.targetCount > 0 && habit.currentCount >= habit.targetCount
    val isOverAchieved = habit.targetCount > 0 && habit.currentCount > habit.targetCount

    SummaCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.FitnessCenter, // Placeholder
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isComplete) SuccessGreen else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "🔥 ${habit.currentStreak} hari",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Text(
                text = if (habit.targetCount > 1) "${habit.currentCount} / ${habit.targetCount}" else "${habit.currentCount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    isOverAchieved -> GoldAccent
                    isComplete -> SuccessGreen
                    habit.currentCount > 0 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
        }
    }
}

@Composable
fun Chip(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun ModeSelectionDialog(
    currentMode: String,
    onDismiss: () -> Unit,
    onModeSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Mode Kontekstual") },
        text = {
            Column {
                ModeOption("Normal", "Tampilan lengkap", currentMode == "Normal") {
                    onModeSelected("Normal")
                }
                ModeOption("Fokus", "Hanya tampilkan tugas", currentMode == "Fokus") {
                    onModeSelected("Fokus")
                }
                ModeOption("Pagi", "Rutinitas pagi hari", currentMode == "Pagi") {
                    onModeSelected("Pagi")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun ModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}