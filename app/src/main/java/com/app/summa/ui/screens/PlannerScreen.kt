package com.app.summa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.Task
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.PlannerUiState
import com.app.summa.ui.viewmodel.PlannerViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var viewMode by remember { mutableStateOf("day") } // day, week, month
    var showFocusMode by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    val tasksByHour = remember(uiState.tasksForDay) {
        uiState.tasksForDay.groupBy {
            try {
                LocalTime.parse(it.scheduledTime ?: "00:00").hour
            } catch (e: Exception) {
                0 // Fallback jika format waktu salah
            }
        }
    }

    LaunchedEffect(uiState.initialTaskTitle) {
        if (uiState.initialTaskTitle != null) {
            showAddTaskDialog = true
        }
    }

    if (showFocusMode && selectedTask != null) {
        FocusModeScreen(
            task = selectedTask!!,
            onComplete = {
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

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    when (viewMode) {
                        "day" -> DailyTimelineView(
                            tasksByHour = tasksByHour,
                            onTaskClick = { task ->
                                selectedTask = task
                                showFocusMode = true
                            }
                        )
                        // PERBAIKAN: Memanggil implementasi grid mingguan baru
                        "week" -> WeeklyCalendarView(
                            viewModel = viewModel,
                            uiState = uiState,
                            onTaskClick = { task ->
                                selectedTask = task
                                showFocusMode = true
                            }
                        )
                        "month" -> MonthlyCalendarView(
                            viewModel = viewModel,
                            uiState = uiState,
                            onTaskClick = { task ->
                                selectedTask = task
                                showFocusMode = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            initialTitle = uiState.initialTaskTitle ?: "",
            initialDescription = uiState.initialTaskContent ?: "",
            onDismiss = {
                showAddTaskDialog = false
                viewModel.clearInitialTask()
            },
            onAddTask = { title, time, isCommitment, twoMinAction, description ->
                viewModel.addTask(
                    title = title,
                    description = description,
                    scheduledTime = time,
                    isCommitment = isCommitment,
                    twoMinuteAction = twoMinAction
                )
                showAddTaskDialog = false
                viewModel.clearInitialTask()
            }
        )
    }
}

@Composable
fun DailyTimelineView(
    tasksByHour: Map<Int, List<Task>>,
    onTaskClick: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(24) { hour ->
            TimelineHour(
                hour = hour,
                tasks = tasksByHour[hour] ?: emptyList(),
                onTaskClick = onTaskClick
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// --- FUNGSI BARU UNTUK TAMPILAN MINGGUAN ---

@Composable
fun WeeklyCalendarView(
    viewModel: PlannerViewModel,
    uiState: PlannerUiState,
    onTaskClick: (Task) -> Unit
) {
    // 1. Dapatkan 7 hari dalam seminggu (Senin - Minggu)
    val selectedDate = uiState.selectedDate
    val firstDayOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDays = remember(firstDayOfWeek) {
        (0..6).map { firstDayOfWeek.plusDays(it.toLong()) }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)) {

        // 2. Render baris pemilih 7 hari
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround // Beri jarak merata
        ) {
            weekDays.forEach { date ->
                WeeklyDayCell(
                    date = date,
                    isSelected = date.isEqual(selectedDate),
                    isToday = date.isEqual(LocalDate.now()),
                    onClick = { viewModel.selectDate(date) } // Klik untuk mengubah tanggal
                )
            }
        }

        // 3. Render daftar tugas untuk hari yang dipilih
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            // Tampilkan header untuk hari yang dipilih
            text = "Tugas pada ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM"))}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Tampilkan daftar tugas (hanya untuk tasksForDay)
        if (uiState.tasksForDay.isEmpty()) {
            Text(
                text = "Tidak ada tugas untuk tanggal ini.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(uiState.tasksForDay) { task ->
                    TaskBlock(task = task, onClick = { onTaskClick(task) })
                }
            }
        }
    }
}

// Composable baru untuk sel hari di tampilan mingguan
@Composable
fun WeeklyDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    // Dapatkan nama hari (Sen, Sel) dan tanggal (18, 19)
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("id"))
    val dayNumber = date.dayOfMonth.toString()

    // Tentukan warna berdasarkan status
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent // Tidak ada background jika tidak dipilih/hari ini
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayName,
                fontSize = 12.sp,
                color = contentColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dayNumber,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}


// --- FUNGSI TAMPILAN BULANAN (TIDAK BERUBAH) ---

@Composable
fun MonthlyCalendarView(
    viewModel: PlannerViewModel,
    uiState: PlannerUiState,
    onTaskClick: (Task) -> Unit
) {
    val tasksByDate = remember(uiState.tasksForMonth) {
        uiState.tasksForMonth.groupBy {
            try {
                LocalDate.parse(it.scheduledDate)
            } catch (e: Exception) {
                null
            }
        }
    }

    val firstDayOfMonth = uiState.selectedDate.with(TemporalAdjusters.firstDayOfMonth())
    val firstDayOfWeekValue = firstDayOfMonth.dayOfWeek.value
    val paddingDays = (firstDayOfWeekValue - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = firstDayOfMonth.lengthOfMonth()

    val dayHeaders = remember {
        DayOfWeek.entries.map {
            it.getDisplayName(TextStyle.SHORT, Locale("id"))
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)) {

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(dayHeaders) { header ->
                CalendarHeaderCell(header = header)
            }

            items(paddingDays) {
                Box(modifier = Modifier
                    .aspectRatio(1f)
                    .padding(2.dp))
            }

            items(daysInMonth) { dayIndex ->
                val dayOfMonth = dayIndex + 1
                val currentDate = firstDayOfMonth.plusDays(dayIndex.toLong())

                val tasksOnThisDay = tasksByDate[currentDate] ?: emptyList()
                val isToday = currentDate.isEqual(LocalDate.now())
                val isSelected = currentDate.isEqual(uiState.selectedDate)

                CalendarDayCell(
                    day = dayOfMonth,
                    tasks = tasksOnThisDay,
                    isToday = isToday,
                    isSelected = isSelected,
                    onClick = {
                        viewModel.selectDate(currentDate)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tugas pada ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM"))}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.tasksForDay.isEmpty()) {
            Text(
                text = "Tidak ada tugas untuk tanggal ini.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(uiState.tasksForDay) { task ->
                    TaskBlock(task = task, onClick = { onTaskClick(task) })
                }
            }
        }
    }
}

@Composable
fun CalendarHeaderCell(header: String) {
    Box(
        modifier = Modifier
            .aspectRatio(1.5f)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = header,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    tasks: List<Task>,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isToday -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else Color.Transparent

    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.toString(),
                fontSize = 14.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
            ) {
                tasks.take(4).forEach { task ->
                    TaskIndicator(isCommitment = task.isCommitment)
                }
            }
        }
    }
}

@Composable
fun TaskIndicator(isCommitment: Boolean) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(
                if (isCommitment) DeepTeal
                else MaterialTheme.colorScheme.secondary
            )
    )
}

// --- FUNGSI YANG ADA (TIDAK BERUBAH) ---

@Composable
fun TimelineHour(
    hour: Int,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 80.dp)
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .wrapContentHeight(),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = String.format("%02d:00", hour),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

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
    task: Task,
    onClick: () -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCommitment) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
                Text(
                    task.scheduledTime ?: "Sepanjang hari",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    initialTitle: String = "",
    initialDescription: String = "",
    onDismiss: () -> Unit,
    onAddTask: (title: String, time: String, isCommitment: Boolean, twoMinAction: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
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
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
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
                    onAddTask(title, time, isCommitment, twoMinAction, description)
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

@Composable
fun FocusModeScreen(
    task: Task,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    var timeRemaining by remember { mutableStateOf(60 * 25) }
    var paperclipsLeft by remember { mutableStateOf(10) }
    var paperclipsMoved by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, timeRemaining) {
        if (isRunning && timeRemaining > 0) {
            kotlinx.coroutines.delay(1000)
            timeRemaining--
        } else if (timeRemaining == 0) {
            isRunning = false
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
                TextButton(onClick = onComplete) {
                    Text("Selesai")
                }
            }

            Spacer(Modifier.height(32.dp))

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
            IconButton(onClick = { isRunning = !isRunning }) {
                Icon(
                    if (isRunning) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = if (isRunning) "Pause" else "Play",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }


            Spacer(Modifier.height(48.dp))

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

            Button(
                onClick = {
                    if (paperclipsLeft > 0) {
                        paperclipsLeft--
                        paperclipsMoved++
                        if (!isRunning) isRunning = true
                    }
                    if (paperclipsLeft == 0) {
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = paperclipsLeft > 0
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
        Box(
            modifier = Modifier
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