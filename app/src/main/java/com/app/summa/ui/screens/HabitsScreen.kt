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
import com.app.summa.ui.model.HabitItem
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: HabitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // PERBAIKAN: Gunakan Crossfade untuk transisi antar layar
    Crossfade(targetState = uiState.selectedHabit, label = "HabitScreen") { selectedHabit ->
        if (selectedHabit != null) {
            HabitDetailScreen(
                habit = selectedHabit,
                onBack = { viewModel.onBackFromDetail() },
                logs = uiState.habitLogs
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Kebiasaan",
                                // PERBAIKAN: Menggunakan tipografi headline
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Habit")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background // Transparan
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
                            .padding(paddingValues),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.habits) { habit ->
                            HabitListItem(
                                habit = habit,
                                onClick = { viewModel.selectHabit(habit) },
                                onIncrement = { viewModel.incrementHabit(habit) },
                                onDecrement = { viewModel.decrementHabit(habit) }
                            )
                        }
                    }
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

// (AddHabitDialog tidak berubah, jadi saya biarkan sama)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, icon: String, target: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🎯") } // Emoji default
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

// --- DESAIN ULANG BESAR PADA HABIT LIST ITEM ---
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
    val progress = (habit.currentCount.toFloat() / habit.targetCount.toFloat()).coerceAtMost(1f)

    SummaCard(onClick = onClick) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kiri: Ikon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        // PERBAIKAN: Background ikon lebih lembut
                        .background(
                            when {
                                isOverAchieved -> MaterialTheme.colorScheme.secondaryContainer
                                isComplete -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
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

                // Tengah: Judul dan Streak
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔥 ${habit.currentStreak}",
                            style = MaterialTheme.typography.labelMedium,
                            color = WarningOrange
                        )
                        if (habit.perfectStreak > 0) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "👑 ${habit.perfectStreak}",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldAccent
                            )
                        }
                    }
                }

                // Kanan: Kontrol +/-
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // PERBAIKAN: Tombol lebih kecil dan tonal
                    FilledTonalIconButton(
                        onClick = onDecrement,
                        enabled = habit.currentCount > 0
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
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
                            text = "$count",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isOverAchieved -> GoldAccent
                                isComplete -> SuccessGreen
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.widthIn(min = 28.dp)
                        )
                    }

                    // PERBAIKAN: Tombol lebih kecil dan tonal
                    FilledTonalIconButton(onClick = onIncrement) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }

            // PERBAIKAN: Progress bar di bawah
            if (habit.targetCount > 0) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isOverAchieved) GoldAccent else if (isComplete) SuccessGreen else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

// PERBAIKAN: Desain ulang Halaman Detail Habit
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
                title = { Text(habit.name, fontWeight = FontWeight.Bold) },
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // "Summa Points" Card
            item {
                SummaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    padding = 20.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "HITUNGAN SUMMA",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${habit.totalSum}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Total poin sepanjang masa",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Streak Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SummaCard(modifier = Modifier.weight(1f), padding = 20.dp) {
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
                                "Konsistensi",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SummaCard(
                        modifier = Modifier.weight(1f),
                        padding = 20.dp,
                        // Kartu "Sempurna" mendapat perlakuan aksen
                        backgroundColor = if (habit.perfectStreak > 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        borderColor = if (habit.perfectStreak > 0) MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
                    ) {
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
                                color = if (habit.perfectStreak > 0) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Sempurna",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (habit.perfectStreak > 0) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Heatmap
            item {
                Text(
                    "Kalender Progres",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            item {
                HabitHeatmap(logs = logs, targetCount = habit.targetCount)
            }
        }
    }
}

// PERBAIKAN: Desain ulang Heatmap
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

    // Menggunakan SummaCard baru (border tipis)
    SummaCard(padding = 20.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp) // Beri jarak lebih
        ) {
            // Header (Nama Bulan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tampilkan 3 nama bulan (Awal, Tengah, Akhir)
                val monthHeaders = listOf(weeks - 1, weeks / 2, 0).map {
                    today.minusWeeks(it.toLong()).month.getDisplayName(TextStyle.SHORT, Locale("id"))
                }.distinct()

                monthHeaders.forEach { monthName ->
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Grid Heatmap
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Render 7 kolom (Sen-Min)
                (0..6).forEach { dayOfWeek ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp) // Jarak antar sel
                    ) {
                        (0 until weeks).forEach { week ->
                            val index = (week * 7) + dayOfWeek
                            if (index < data.size) {
                                val (_, value) = data[index]
                                HeatmapCell(value = value)
                            } else {
                                Box(Modifier.size(16.dp)) // Placeholder
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeatmapCell(value: Int) {
    val cellSize = 16.dp // Ukuran sel sedikit lebih besar
    val color = when (value) {
        0 -> MaterialTheme.colorScheme.surfaceVariant // Gagal/Kosong
        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) // Parsial
        2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) // Sempurna
        else -> MaterialTheme.colorScheme.secondary // Over-achievement (value == 3)
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .clip(MaterialTheme.shapes.extraSmall) // Rounded corner 4dp
            .background(color)
    )
}