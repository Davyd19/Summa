package com.app.summa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
// PENAMBAHAN: import items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
// PENAMBAHAN: import KeyboardOptions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
// PENAMBAHAN: import text input
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
// PENAMBAHAN: import ViewModel
import androidx.hilt.navigation.compose.hiltViewModel
// PERBAIKAN: Hapus data class palsu, import data class asli
import com.app.summa.data.model.Task
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.*
// PENAMBAHAN: import ViewModel
import com.app.summa.ui.viewmodel.PlannerViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// PERBAIKAN: Hapus data class 'ScheduledTask'
// Kita akan menggunakan data class 'Task' langsung dari database

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    // PENAMBAHAN: Injeksi ViewModel
    viewModel: PlannerViewModel = hiltViewModel()
) {
    // PENAMBAHAN: Ambil state dari ViewModel
    val uiState by viewModel.uiState.collectAsState()

    var viewMode by remember { mutableStateOf("day") } // day, week, month
    var showFocusMode by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    // PENAMBAHAN: State untuk dialog
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // PERBAIKAN: Kelompokkan task berdasarkan jam
    val tasksByHour = remember(uiState.tasks) {
        uiState.tasks.groupBy {
            try {
                LocalTime.parse(it.scheduledTime ?: "00:00").hour
            } catch (e: Exception) {
                0 // Fallback jika format waktu salah
            }
        }
    }

    if (showFocusMode && selectedTask != null) {
        FocusModeScreen(
            task = selectedTask!!,
            onComplete = {
                // Panggil ViewModel untuk menyelesaikan task
                viewModel.completeTask(selectedTask!!.id)
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
                                // Gunakan selectedDate dari ViewModel
                                uiState.selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* TODO: Calendar picker */ }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                        // PERBAIKAN: Tampilkan dialog
                        IconButton(onClick = { showAddTaskDialog = true }) {
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
                // View Mode Selector (Tidak berubah)
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
                            // PERBAIKAN: Ambil task dari map
                            tasks = tasksByHour[hour] ?: emptyList(),
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

    // PENAMBAHAN: Dialog untuk menambah task
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAddTask = { title, time, isCommitment, twoMinAction ->
                viewModel.addTask(
                    title = title,
                    scheduledTime = time,
                    isCommitment = isCommitment,
                    twoMinuteAction = twoMinAction
                )
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun TimelineHour(
    hour: Int,
    // PERBAIKAN: Gunakan 'Task'
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // PERBAIKAN: Gunakan minHeight agar bisa membesar jika task banyak
            .defaultMinSize(minHeight = 80.dp)
    ) {
        // Hour label
        Box(
            modifier = Modifier
                .width(60.dp)
                // PERBAIKAN: Sesuaikan tinggi dengan konten
                .wrapContentHeight(),
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
                if (tasks.isEmpty()) {
                    // Beri ruang kosong agar border tetap terlihat
                    Spacer(modifier = Modifier.height(72.dp))
                } else {
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
}

@Composable
fun TaskBlock(
    // PERBAIKAN: Gunakan 'Task'
    task: Task,
    onClick: () -> Unit
) {
    // Tentukan tampilan berdasarkan Blok Komitmen vs Aspirasi
    val isCommitment = task.isCommitment
    val backgroundColor = if (isCommitment) DeepTeal.copy(alpha = 0.9f)
    else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isCommitment) Color.White
    else MaterialTheme.colorScheme.onSurface
    val border = if (!isCommitment)
        androidx.compose.foundation.BorderStroke(
            1.dp,
            DeepTeal.copy(alpha = 0.5f)
        )
    else null

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = backgroundColor,
        border = border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TODO: Tambahkan logika untuk streak (jika task ini adalah bagian dari habit)
            // if (task.hasStreak) { ... }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCommitment) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
                // Tampilkan waktu
                Text(
                    task.scheduledTime ?: "Sepanjang hari",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }

            // Tampilkan status selesai
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

// PENAMBAHAN: Dialog untuk "Aturan 2 Menit"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (title: String, time: String, isCommitment: Boolean, twoMinAction: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var twoMinAction by remember { mutableStateOf("") }
    var time by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var isCommitment by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Tugas Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Tugas") },
                    modifier = Modifier.fillMaxWidth()
                )
                // IMPLEMENTASI: Aturan 2 Menit
                OutlinedTextField(
                    value = twoMinAction,
                    onValueChange = { twoMinAction = it },
                    label = { Text("Langkah 2 Menit (Opsional)") },
                    placeholder = { Text("Cth: Buka file & tulis 1 paragraf") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Waktu (HH:mm)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isCommitment,
                        onCheckedChange = { isCommitment = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isCommitment) "Blok Komitmen" else "Blok Aspirasi")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAddTask(title, time, isCommitment, twoMinAction)
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


// PERBAIKAN: FocusModeScreen sekarang menerima 'Task'
@Composable
fun FocusModeScreen(
    task: Task, // Menggunakan data class asli
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    // TODO: Ganti durasi dengan data dari task jika ada
    var timeRemaining by remember { mutableStateOf(60 * 25) } // Default 25 menit
    var paperclipsLeft by remember { mutableStateOf(10) } // Default 10 klip
    var paperclipsMoved by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }

    // Timer logic
    LaunchedEffect(isRunning, timeRemaining) {
        if (isRunning && timeRemaining > 0) {
            kotlinx.coroutines.delay(1000)
            timeRemaining--
        } else if (timeRemaining == 0) {
            isRunning = false
            // TODO: Tambahkan notifikasi/suara
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
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                // Tombol Selesai Cepat
                TextButton(onClick = onComplete) {
                    Text("Selesai")
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
            // Tombol Play/Pause
            IconButton(onClick = { isRunning = !isRunning }) {
                Icon(
                    if (isRunning) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = if (isRunning) "Pause" else "Play",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }


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

            // Tampilkan Langkah 2 Menit!
            if (task.twoMinuteAction.isNotBlank()) {
                Text(
                    "Langkah 2 Menit:",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    task.twoMinuteAction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(16.dp))
            } else {
                Text(
                    "Pindahkan klip untuk menandai progres",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }


            Spacer(Modifier.height(16.dp))

            // Main Action Button
            Button(
                onClick = {
                    if (paperclipsLeft > 0) {
                        paperclipsLeft--
                        paperclipsMoved++
                        if (!isRunning) isRunning = true
                        // TODO: Play sound effect here
                    }
                    if (paperclipsLeft == 0) {
                        onComplete() // Selesai otomatis jika klip habis
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = paperclipsLeft > 0 // Nonaktifkan jika klip habis
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

            // Tombol Selesai jika klip masih ada
            if (paperclipsLeft > 0 && paperclipsMoved > 0) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("SELESAI LEBIH AWAL")
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
                // PERBAIKAN: Ukuran lebih besar
                .width(140.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 32.dp, bottomEnd = 32.dp))
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 32.dp, bottomEnd = 32.dp)
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
                // Ilustrasi tumpukan klip
                if (isFull) {
                    repeat(minOf(3, count / 5)) {
                        Text(
                            "📎📎📎📎📎",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    repeat(minOf(3, count / 5)) {
                        Text(
                            "📎📎📎📎📎",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GoldAccent.copy(alpha = 0.8f)
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