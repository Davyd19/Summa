package com.app.summa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.*
import java.time.LocalTime
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    var selectedMode by remember { mutableStateOf("Normal") }
    var showModeDialog by remember { mutableStateOf(false) }

    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Selamat Pagi"
        in 12..14 -> "Selamat Siang"
        in 15..18 -> "Selamat Sore"
        else -> "Selamat Malam"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "$greeting, Davyd!",
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
                    FilledTonalButton(
                        onClick = { showModeDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mode")
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
                                text = "Hitungan Summa: +12 poin",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        ProgressCircle(
                            progress = 0.7f,
                            size = 80.dp,
                            strokeWidth = 8.dp
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
                SummaCard(
                    onClick = { /* Start Focus Mode */ }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = DeepTeal,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "14:00",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Laporan Proyek",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(onClick = { /* Focus */ }) {
                            Text("Mulai Fokus")
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
                HabitQuickList()
            }

            // Quick Widgets
            item {
                Text(
                    text = "Widget Cepat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaCard(
                        modifier = Modifier.weight(1f),
                        onClick = { /* Budget */ }
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null)
                        Spacer(Modifier.height(8.dp))
                        Text("Budget", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Jajan: Aman",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessGreen
                        )
                    }

                    SummaCard(
                        modifier = Modifier.weight(1f),
                        onClick = { /* Journal */ }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.height(8.dp))
                        Text("Jurnal", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Catat ide?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showModeDialog) {
        ModeSelectionDialog(
            currentMode = selectedMode,
            onDismiss = { showModeDialog = false },
            onModeSelected = {
                selectedMode = it
                showModeDialog = false
            }
        )
    }
}

@Composable
fun HabitQuickList() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HabitQuickItem("Latihan Fisik", "Hal Ateuo", 1, 3)
        HabitQuickItem("Latihan Fisik", "Menuasra Quran", 0, 3, isBudgelift = true)
        HabitQuickItem("Membaca Quran", "Bihad 1 Halal", 2, 3, progress = 0.7f)
    }
}

@Composable
fun HabitQuickItem(
    name: String,
    subtitle: String,
    current: Int,
    target: Int,
    progress: Float = current.toFloat() / target,
    isBudgelift: Boolean = false
) {
    SummaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (progress >= 1f) SuccessGreen else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (isBudgelift) {
                Chip(
                    text = "Budgelift",
                    color = WarningOrange
                )
            } else {
                Text(
                    text = "$current / $target",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        progress >= 1.3f -> GoldAccent
                        progress >= 1f -> SuccessGreen
                        progress >= 0.7f -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
            }
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