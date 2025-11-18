package com.app.summa.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.Task
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.PlannerUiState
import com.app.summa.ui.viewmodel.PlannerViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

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

    // Cek apakah ada task awal dari navigasi (misal dari Knowledge)
    LaunchedEffect(uiState.initialTaskTitle) {
        if (uiState.initialTaskTitle != null) {
            showAddTaskDialog = true
        }
    }

    if (showFocusMode && selectedTask != null) {
        PhysicalFocusModeScreen(
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

// --- PHYSICAL PAPERCLIP MODE WITH DRAG & DROP ---

@Composable
fun PhysicalFocusModeScreen(
    task: Task,
    onComplete: (Int, Long) -> Unit,
    onCancel: () -> Unit
) {
    var timeRemaining by rememberSaveable { mutableIntStateOf(60 * 25) }
    var paperclipsLeft by rememberSaveable { mutableIntStateOf(10) } // Target 10 klip per sesi
    var paperclipsMoved by rememberSaveable { mutableIntStateOf(0) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    val startTime by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

    // Drag State
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    // Animatable offset untuk efek snap-back
    val animatedOffsetX = remember { Animatable(0f) }
    val animatedOffsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

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
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            TextButton(onClick = { onComplete(paperclipsMoved, startTime) }) {
                Text("Selesai")
            }
        }

        Spacer(Modifier.height(32.dp))

        // Timer
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(220.dp)
                .border(4.dp, DeepTeal.copy(alpha = 0.2f), CircleShape)
        ) {
            Text(
                String.format("%02d:%02d", timeRemaining / 60, timeRemaining % 60),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = DeepTeal
            )
        }

        Spacer(Modifier.height(24.dp))

        IconButton(
            onClick = { isRunning = !isRunning },
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Play",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            "Tarik Klip ke Kanan untuk Fokus ->",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SOURCE PILE (Draggable Area)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
            ) {
                if (paperclipsLeft > 0) {
                    // Draggable Paperclip (Emoji or Icon)
                    Text(
                        "📎",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    animatedOffsetX.value.roundToInt() + dragOffset.x.roundToInt(),
                                    animatedOffsetY.value.roundToInt() + dragOffset.y.roundToInt()
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                        if(!isRunning) isRunning = true // Otomatis mulai timer
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        // LOGIKA DROP: Jika ditarik cukup jauh ke kanan (> 150px)
                                        if (dragOffset.x > 150) {
                                            paperclipsLeft--
                                            paperclipsMoved++
                                            // Reset posisi seketika untuk klip berikutnya
                                            scope.launch {
                                                animatedOffsetX.snapTo(0f)
                                                animatedOffsetY.snapTo(0f)
                                            }
                                            dragOffset = Offset.Zero

                                            if(paperclipsLeft == 0) {
                                                onComplete(paperclipsMoved, startTime)
                                            }
                                        } else {
                                            // ANIMASI SNAP-BACK (Kembali ke asal jika gagal drop)
                                            scope.launch {
                                                val endX = dragOffset.x
                                                val endY = dragOffset.y
                                                // Pindahkan beban offset ke Animatable
                                                animatedOffsetX.snapTo(endX)
                                                animatedOffsetY.snapTo(endY)
                                                // Reset dragOffset manual
                                                dragOffset = Offset.Zero
                                                // Animate back to 0
                                                launch { animatedOffsetX.animateTo(0f) }
                                                launch { animatedOffsetY.animateTo(0f) }
                                            }
                                        }
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount
                                }
                            }
                    )
                } else {
                    Text("Selesai!", style = MaterialTheme.typography.labelMedium)
                }

                // Counter Badge
                Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            "$paperclipsLeft",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )

            // TARGET PILE (Drop Zone)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(SuccessGreen.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .border(2.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📎", style = MaterialTheme.typography.displayMedium, color = SuccessGreen.copy(alpha = 0.5f))
                    Text(
                        "$paperclipsMoved",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}

// --- VIEWS IMPLEMENTATION (FULL LOGIC) ---

@Composable
fun CleanDailyView(tasks: List<Task>, onTaskClick: (Task) -> Unit) {
    val tasksByHour = remember(tasks) {
        tasks.groupBy { try { LocalTime.parse(it.scheduledTime ?: "00:00").hour } catch (e: Exception) { 0 } }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        items(24) { hour -> CleanTimeSlot(hour, tasksByHour[hour] ?: emptyList(), onTaskClick) }
    }
}

@Composable
fun CleanTimeSlot(hour: Int, tasks: List<Task>, onTaskClick: (Task) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
        Box(modifier = Modifier.width(56.dp).padding(top = 4.dp)) {
            Text(
                String.format("%02d:00", hour),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tasks.forEach { task -> CleanTaskCard(task, onClick = { onTaskClick(task) }) }
        }
    }
}

@Composable
fun CleanTaskCard(task: Task, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCommitment) DeepTeal.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            if (task.isCommitment) DeepTeal.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
                Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (task.scheduledTime != null) {
                    Text(
                        task.scheduledTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Icon(
                if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                null,
                tint = if (task.isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// --- FULLY IMPLEMENTED WEEKLY VIEW ---

@Composable
fun CleanWeeklyView(
    viewModel: PlannerViewModel,
    uiState: PlannerUiState,
    onTaskClick: (Task) -> Unit
) {
    val selectedDate = uiState.selectedDate
    // Tentukan hari pertama dalam minggu (Senin)
    val firstDayOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    // Buat list 7 hari ke depan
    val weekDays = remember(firstDayOfWeek) { (0..6).map { firstDayOfWeek.plusDays(it.toLong()) } }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Navigasi Minggu
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.selectDate(firstDayOfWeek.minusWeeks(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Minggu Sebelumnya")
            }
            Text(
                "${firstDayOfWeek.format(DateTimeFormatter.ofPattern("d MMM"))} - ${weekDays.last().format(DateTimeFormatter.ofPattern("d MMM"))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { viewModel.selectDate(firstDayOfWeek.plusWeeks(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Minggu Berikutnya")
            }
        }

        // Baris Hari (Horizontal Scroll)
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
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Daftar Tugas untuk Hari yang DIPILIH
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.tasksForDay.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tidak ada tugas untuk tanggal ini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
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
fun CleanDayCell(date: LocalDate, isSelected: Boolean, isToday: Boolean, onClick: () -> Unit) {
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
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(dayName, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(dayNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
        }
    }
}

// --- FULLY IMPLEMENTED MONTHLY VIEW ---

@Composable
fun CleanMonthlyView(
    viewModel: PlannerViewModel,
    uiState: PlannerUiState,
    onTaskClick: (Task) -> Unit
) {
    // Kelompokkan tugas bulan ini berdasarkan tanggal untuk indikator titik
    val tasksByDate = remember(uiState.tasksForMonth) {
        uiState.tasksForMonth.groupBy { try { LocalDate.parse(it.scheduledDate) } catch (e: Exception) { null } }
    }

    val firstDayOfMonth = uiState.selectedDate.with(TemporalAdjusters.firstDayOfMonth())
    // Hitung padding hari (misal tgl 1 hari Rabu, berarti Senin & Selasa kosong)
    val paddingDays = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = firstDayOfMonth.lengthOfMonth()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Navigasi Bulan
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.selectDate(firstDayOfMonth.minusMonths(1)) }) {
                Icon(Icons.Default.ChevronLeft, "Bulan Sebelumnya")
            }
            Text(
                firstDayOfMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id"))),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.selectDate(firstDayOfMonth.plusMonths(1)) }) {
                Icon(Icons.Default.ChevronRight, "Bulan Berikutnya")
            }
        }

        // Grid Kalender
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header Nama Hari
            items(7) { index ->
                Text(
                    DayOfWeek.of(index + 1).getDisplayName(TextStyle.SHORT, Locale("id")),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Sel Kosong (Padding)
            items(paddingDays) { Box(modifier = Modifier.aspectRatio(1f)) }

            // Sel Tanggal
            items(daysInMonth) { dayIndex ->
                val currentDate = firstDayOfMonth.plusDays(dayIndex.toLong())
                val tasksOnDay = tasksByDate[currentDate] ?: emptyList()

                CleanCalendarCell(
                    day = dayIndex + 1,
                    taskCount = tasksOnDay.size,
                    isToday = currentDate.isEqual(LocalDate.now()),
                    isSelected = currentDate.isEqual(uiState.selectedDate),
                    onClick = { viewModel.selectDate(currentDate) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Judul Section Daftar Tugas
        Text(
            "Tugas pada ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM", Locale("id")))}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Daftar Tugas di bawah Kalender
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.tasksForDay.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tidak ada tugas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
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
fun CleanCalendarCell(day: Int, taskCount: Int, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
            else if (isToday) MaterialTheme.colorScheme.primaryContainer
            else Color.Transparent
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )

            // Indikator Titik jika ada tugas
            if (taskCount > 0) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(minOf(taskCount, 3)) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else DeepTeal)
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