package com.app.summa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ScheduledTask(
    val id: Long,
    val title: String,
    val time: LocalTime,
    val duration: Int, // minutes
    val isCommitment: Boolean,
    val isCompleted: Boolean = false,
    val hasStreak: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen() {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var viewMode by remember { mutableStateOf("day") } // day, week, month
    var showFocusMode by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<ScheduledTask?>(null) }

    val tasks = remember {
        mutableStateListOf(
            ScheduledTask(1, "Latihan Pagi", LocalTime.of(6, 0), 30, false, false, true),
            ScheduledTask(2, "Membaca Quran", LocalTime.of(6, 30), 20, false, false, true),
            ScheduledTask(3, "Review Email", LocalTime.of(9, 0), 30, true, false),
            ScheduledTask(4, "Meeting Tim", LocalTime.of(10, 0), 60, true, false),
            ScheduledTask(5, "Laporan Proyek", LocalTime.of(14, 0), 120, true, false),
            ScheduledTask(6, "Olahraga Sore", LocalTime.of(17, 0), 45, false),
            ScheduledTask(7, "Refleksi Harian", LocalTime.of(21, 0), 15, false)
        )
    }

    if (showFocusMode && selectedTask != null) {
        FocusModeScreen(
            task = selectedTask!!,
            onComplete = {
                showFocusMode = false
                selectedTask = null
            },
            onCancel = {
                showFocusMode = false
                selectedTask = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Planner",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Calendar picker */ }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                        IconButton(onClick = { /* Add task */ }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Task")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // View Mode Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = viewMode == "day",
                        onClick = { viewMode = "day" },
                        label = { Text("Harian") }
                    )
                    FilterChip(
                        selected = viewMode == "week",
                        onClick = { viewMode = "week" },
                        label = { Text("Mingguan") }
                    )
                    FilterChip(
                        selected = viewMode == "month",
                        onClick = { viewMode = "month" },
                        label = { Text("Bulanan") }
                    )
                }

                // Timeline View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(24) { hour ->
                        TimelineHour(
                            hour = hour,
                            tasks = tasks.filter { it.time.hour == hour },
                            onTaskClick = { task ->
                                selectedTask = task
                                showFocusMode = true
                            }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun TimelineHour(
    hour: Int,
    tasks: List<ScheduledTask>,
    onTaskClick: (ScheduledTask) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        // Hour label
        Box(
            modifier = Modifier
                .width(60.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = String.format("%02d:00", hour),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // Timeline content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                .padding(4.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tasks.forEach { task ->
                    TaskBlock(
                        task = task,
                        onClick = { onTaskClick(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskBlock(
    task: ScheduledTask,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = if (task.isCommitment)
            DeepTeal.copy(alpha = 0.9f)
        else
            MaterialTheme.colorScheme.surfaceVariant,
        border = if (!task.isCommitment)
            androidx.compose.foundation.BorderStroke(
                1.dp,
                DeepTeal.copy(alpha = 0.5f)
            )
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.hasStreak) {
                Text(
                    "🔥",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (task.isCommitment) FontWeight.Bold else FontWeight.Normal,
                    color = if (task.isCommitment) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${task.duration} menit",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.isCommitment)
                        Color.White.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (task.isCompleted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = SuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// File: ui/screens/FocusModeScreen.kt
@Composable
fun FocusModeScreen(
    task: ScheduledTask,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    var timeRemaining by remember { mutableStateOf(task.duration * 60) } // seconds
    var paperclipsLeft by remember { mutableStateOf(100) }
    var paperclipsMoved by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        if (isRunning && timeRemaining > 0) {
            kotlinx.coroutines.delay(1000)
            timeRemaining--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Cancel")
                }
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { /* Menu */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            }

            Spacer(Modifier.height(32.dp))

            // Timer
            Text(
                text = String.format(
                    "%02d:%02d",
                    timeRemaining / 60,
                    timeRemaining % 60
                ),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = DeepTeal
            )

            Spacer(Modifier.height(48.dp))

            // Paperclip Jars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PaperclipJar(
                    count = paperclipsLeft,
                    label = "Tersisa",
                    isFull = true
                )

                PaperclipJar(
                    count = paperclipsMoved,
                    label = "Selesai",
                    isFull = false
                )
            }

            Spacer(Modifier.weight(1f))

            // Progress text
            Text(
                "Pindahkan klip untuk menandai progres",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(16.dp))

            // Main Action Button
            Button(
                onClick = {
                    if (paperclipsLeft > 0) {
                        paperclipsLeft--
                        paperclipsMoved++
                        if (!isRunning) isRunning = true
                        // Play sound effect here
                    } else {
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "📎",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "PINDAHKAN 1 KLIP",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Secondary button
            if (paperclipsMoved >= paperclipsLeft + paperclipsMoved) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("SELESAI")
                }
            }
        }
    }
}

@Composable
fun PaperclipJar(
    count: Int,
    label: String,
    isFull: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(140.dp)
    ) {
        // Jar illustration
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    if (isFull)
                        DeepTeal.copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "📎",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(8.dp)
                )
                if (isFull) {
                    repeat(minOf(3, count / 25)) {
                        Text(
                            "📎📎📎📎📎",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (isFull) DeepTeal else GoldAccent
        )

        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}