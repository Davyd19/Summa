package com.app.summa.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.Task
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.PlannerUiState
import com.app.summa.ui.viewmodel.PlannerViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
    var viewMode by remember { mutableStateOf("day") }
    var showFocusMode by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // State untuk Date Picker
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.selectedDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )

    LaunchedEffect(uiState.initialTaskTitle) {
        if (uiState.initialTaskTitle != null) {
            showAddTaskDialog = true
        }
    }

    if (showFocusMode && selectedTask != null) {
        FocusModeScreen(
            task = selectedTask!!,
            onComplete = { paperclips, startTime ->
                viewModel.completeTask(selectedTask!!.id)
                viewModel.saveFocusSession(selectedTask!!.id, paperclips, startTime)
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
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM", Locale("id"))),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    actions = {
                        // Tombol Kalender sekarang memicu Dialog
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.Today, contentDescription = "Pilih Tanggal")
                        }
                        IconButton(onClick = { showAddTaskDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah Tugas")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // View Mode Selector
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "day" to "Harian",
                            "week" to "Mingguan",
                            "month" to "Bulanan"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = viewMode == mode,
                                onClick = { viewMode = mode },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    when (viewMode) {
                        "day" -> CleanDailyView(
                            tasks = uiState.tasksForDay,
                            onTaskClick = {
                                selectedTask = it
                                showFocusMode = true
                            }
                        )
                        "week" -> CleanWeeklyView(
                            viewModel = viewModel,
                            uiState = uiState,
                            onTaskClick = {
                                selectedTask = it
                                showFocusMode = true
                            }
                        )
                        "month" -> CleanMonthlyView(
                            viewModel = viewModel,
                            uiState = uiState,
                            onTaskClick = {
                                selectedTask = it
                                showFocusMode = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog Date Picker
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            viewModel.selectDate(selectedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAddTaskDialog) {
        CleanAddTaskDialog(
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
fun CleanDailyView(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit
) {
    val tasksByHour = remember(tasks) {
        tasks.groupBy {
            try {
                LocalTime.parse(it.scheduledTime ?: "00:00").hour
            } catch (e: Exception) {
                0
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(24) { hour ->
            CleanTimeSlot(
                hour = hour,
                tasks = tasksByHour[hour] ?: emptyList(),
                onTaskClick = onTaskClick
            )
        }
    }
}

@Composable
fun CleanTimeSlot(
    hour: Int,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
    ) {
        // Time Label
        Box(
            modifier = Modifier
                .width(56.dp)
                .padding(top = 4.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = String.format("%02d:00", hour),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Task Area
        Column(
            modifier = Modifier
                .weight(1f)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tasks.forEach { task ->
                CleanTaskCard(
                    task = task,
                    onClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
fun CleanTaskCard(
    task: Task,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCommitment)
                DeepTeal.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            if (task.isCommitment) DeepTeal.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (task.isCompleted) SuccessGreen
                        else if (task.isCommitment) DeepTeal
                        else MaterialTheme.colorScheme.outline
                    )
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (task.scheduledTime != null) {
                    Text(
                        task.scheduledTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (task.isCompleted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selesai",
                    tint = SuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Mulai",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CleanWeeklyView(
    viewModel: PlannerViewModel,
    uiState: PlannerUiState,
    onTaskClick: (Task) -> Unit
) {
    val selectedDate = uiState.selectedDate
    val firstDayOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDays = remember(firstDayOfWeek) {
        (0..6).map { firstDayOfWeek.plusDays(it.toLong()) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Week Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.selectDate(firstDayOfWeek.minusWeeks(1))
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Minggu Sebelumnya")
            }

            Text(
                "${firstDayOfWeek.format(DateTimeFormatter.ofPattern("d MMM"))} - ${weekDays.last().format(DateTimeFormatter.ofPattern("d MMM"))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            IconButton(onClick = {
                viewModel.selectDate(firstDayOfWeek.plusWeeks(1))
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Minggu Berikutnya")
            }
        }

        // Days Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(weekDays.size) { index ->
                CleanDayCell(
                    date = weekDays[index],
                    isSelected = weekDays[index].isEqual(selectedDate),
                    isToday = weekDays[index].isEqual(LocalDate.now()),
                    onClick = { viewModel.selectDate(weekDays[index]) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Tasks for Selected Day
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.tasksForDay.isEmpty()) {
                item {
                    Text(
                        "Tidak ada tugas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(uiState.tasksForDay) { task ->
                    CleanTaskCard(task = task, onClick = { onTaskClick(task) })
                }
            }
        }
    }
}

@Composable
fun CleanDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("id"))
    val dayNumber = date.dayOfMonth.toString()

    Card(
        onClick = onClick,
        modifier = Modifier.width(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                dayName,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                dayNumber,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CleanMonthlyView(
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

    Column(modifier = Modifier.fillMaxSize()) {
        // Month Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.selectDate(firstDayOfMonth.minusMonths(1))
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Bulan Sebelumnya")
            }

            Text(
                firstDayOfMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id"))),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = {
                viewModel.selectDate(firstDayOfMonth.plusMonths(1))
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Bulan Berikutnya")
            }
        }

        // Calendar Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Day Headers
            items(7) { index ->
                val dayName = DayOfWeek.of(index + 1).getDisplayName(TextStyle.SHORT, Locale("id"))
                Text(
                    dayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Empty cells
            items(paddingDays) {
                Box(modifier = Modifier.aspectRatio(1f))
            }

            // Date cells
            items(daysInMonth) { dayIndex ->
                val dayOfMonth = dayIndex + 1
                val currentDate = firstDayOfMonth.plusDays(dayIndex.toLong())
                val tasksOnDay = tasksByDate[currentDate] ?: emptyList()
                val isToday = currentDate.isEqual(LocalDate.now())
                val isSelected = currentDate.isEqual(uiState.selectedDate)

                CleanCalendarCell(
                    day = dayOfMonth,
                    taskCount = tasksOnDay.size,
                    isToday = isToday,
                    isSelected = isSelected,
                    onClick = { viewModel.selectDate(currentDate) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Tasks for Selected Date
        Text(
            "Tugas pada ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM", Locale("id")))}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.tasksForDay.isEmpty()) {
                item {
                    Text(
                        "Tidak ada tugas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                items(uiState.tasksForDay) { task ->
                    CleanTaskCard(task = task, onClick = { onTaskClick(task) })
                }
            }
        }
    }
}

@Composable
fun CleanCalendarCell(
    day: Int,
    taskCount: Int,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.Transparent
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White
                else MaterialTheme.colorScheme.onSurface
            )
            if (taskCount > 0) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(minOf(taskCount, 3)) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White
                                    else DeepTeal
                                )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanAddTaskDialog(
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
        title = { Text("Tugas Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Tugas") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Waktu (HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = twoMinAction,
                    onValueChange = { twoMinAction = it },
                    label = { Text("Langkah 2 Menit (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isCommitment,
                        onCheckedChange = { isCommitment = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            if (isCommitment) "Blok Komitmen" else "Blok Aspirasi",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (isCommitment) "Tugas prioritas" else "Tugas fleksibel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddTask(title, time, isCommitment, twoMinAction, description) },
                shape = RoundedCornerShape(12.dp)
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
    onComplete: (Int, Long) -> Unit, // (paperclips, startTime)
    onCancel: () -> Unit
) {
    // State bertahan saat rotasi layar
    var timeRemaining by rememberSaveable { mutableIntStateOf(60 * 25) }
    var paperclipsLeft by rememberSaveable { mutableIntStateOf(10) }
    var paperclipsMoved by rememberSaveable { mutableIntStateOf(0) }
    var isRunning by rememberSaveable { mutableStateOf(false) }

    // Catat waktu mulai (hanya sekali saat pertama dibuka)
    val startTime by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isRunning, timeRemaining) {
        if (isRunning && timeRemaining > 0) {
            kotlinx.coroutines.delay(1000)
            timeRemaining--
        } else if (timeRemaining == 0) {
            isRunning = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Tutup")
            }
            Text(
                task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = {
                // Kirim data saat selesai manual
                onComplete(paperclipsMoved, startTime)
            }) {
                Text("Selesai")
            }
        }

        Spacer(Modifier.height(48.dp))

        // Timer
        Text(
            String.format("%02d:%02d", timeRemaining / 60, timeRemaining % 60),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = DeepTeal
        )

        Spacer(Modifier.height(16.dp))

        IconButton(
            onClick = { isRunning = !isRunning },
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Play",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.weight(1f))

        // Paperclip Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PaperclipCounter("Tersisa", paperclipsLeft, DeepTeal)
            PaperclipCounter("Selesai", paperclipsMoved, SuccessGreen)
        }

        Spacer(Modifier.height(32.dp))

        // Move Button
        Button(
            onClick = {
                if (paperclipsLeft > 0) {
                    paperclipsLeft--
                    paperclipsMoved++
                    if (!isRunning) isRunning = true
                }
                if (paperclipsLeft == 0) {
                    // Otomatis selesai jika klip habis
                    onComplete(paperclipsMoved, startTime)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = paperclipsLeft > 0
        ) {
            Text(
                "PINDAHKAN 1 KLIP 📎",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PaperclipCounter(label: String, count: Int, color: Color) {
    Card(
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "📎",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}