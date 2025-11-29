package com.app.summa.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.Identity
import com.app.summa.data.model.Task
import com.app.summa.ui.components.TaskInputSheet
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
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    LaunchedEffect(uiState.initialTaskTitle) {
        if (uiState.initialTaskTitle != null) showAddTaskSheet = true
    }

    if (selectedTask != null) {
        UniversalFocusModeScreen(
            title = selectedTask!!.title,
            initialTarget = 10,
            onComplete = { paperclips, startTime ->
                viewModel.completeTask(selectedTask!!.id)
                viewModel.saveFocusSession(selectedTask!!.id, paperclips, startTime)
                selectedTask = null
            },
            onCancel = { selectedTask = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Planner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM", Locale("id"))), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.Today, contentDescription = "Pilih Tanggal") }
                        IconButton(onClick = { showAddTaskSheet = true }) { Icon(Icons.Default.Add, contentDescription = "Tambah Tugas") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // View Mode Switcher
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("day" to "Harian", "week" to "Mingguan", "month" to "Bulanan").forEach { (mode, label) ->
                            FilterChip(selected = viewMode == mode, onClick = { viewMode = mode }, label = { Text(label) }, modifier = Modifier.weight(1f), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White))
                        }
                    }
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    when (viewMode) {
                        "day" -> InteractiveDailyView(
                            tasks = uiState.tasksForDay,
                            onTaskClick = { selectedTask = it },
                            onTaskMoved = { task, newHour -> viewModel.moveTaskToTime(task, newHour) }
                        )
                        "week" -> CleanWeeklyView(viewModel = viewModel, uiState = uiState, onTaskClick = { selectedTask = it })
                        "month" -> CleanMonthlyView(viewModel = viewModel, uiState = uiState, onTaskClick = { selectedTask = it })
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { millis -> viewModel.selectDate(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showAddTaskSheet) {
        TaskInputSheet(
            identities = uiState.availableIdentities,
            onDismiss = {
                showAddTaskSheet = false
                viewModel.clearInitialTask()
            },
            onSave = { title, desc, time, isCommitment, identityId, twoMin ->
                viewModel.addTask(title, desc, time, isCommitment, twoMin, identityId)
                showAddTaskSheet = false
                viewModel.clearInitialTask()
            }
        )
    }
}

// --- INTERACTIVE DRAG & DROP IMPLEMENTATION ---

@Composable
fun InteractiveDailyView(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onTaskMoved: (Task, Int) -> Unit
) {
    // Group tasks by hour
    val tasksByHour = remember(tasks) {
        tasks.groupBy { try { LocalTime.parse(it.scheduledTime ?: "00:00").hour } catch (e: Exception) { 0 } }
    }

    // Drag & Drop State
    var draggingTask by remember { mutableStateOf<Task?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }

    // Map untuk menyimpan koordinat setiap slot waktu (Hour -> Rect Bounds)
    val dropZones = remember { mutableStateMapOf<Int, Rect>() }
    var activeDropZone by remember { mutableStateOf<Int?>(null) }

    val haptic = LocalHapticFeedback.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable List of Hours
        // Menggunakan Column + verticalScroll agar semua slot jam dirender dan punya koordinat
        // LazyColumn akan me-recycle item sehingga drop zone yang off-screen tidak terdeteksi
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            repeat(24) { hour ->
                // Drop Target Container
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            // Simpan koordinat slot ini
                            dropZones[hour] = coordinates.boundsInWindow()
                        }
                ) {
                    CleanTimeSlot(
                        hour = hour,
                        tasks = tasksByHour[hour] ?: emptyList(),
                        isDropTarget = activeDropZone == hour,
                        onTaskClick = onTaskClick,
                        onDragStart = { task, offset ->
                            draggingTask = task
                            dragStartOffset = offset
                            dragOffset = offset
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        // Sembunyikan task asli saat sedang di-drag
                        hiddenTask = draggingTask
                    )
                }
            }
            // Spacer extra di bawah agar mudah drag ke jam malam
            Spacer(Modifier.height(100.dp))
        }

        // --- DRAG OVERLAY LAYER ---
        if (draggingTask != null) {
            // Deteksi Drag global di atas seluruh layar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount

                                // Cek drop zone mana yang aktif
                                val currentWindowPos = change.position
                                // Konversi local position ke window position itu tricky di overlay
                                // Simplifikasi: gunakan posisi absolut change.position + offset awal relatif
                                // Namun detectDragGestures change.position adalah relatif ke Box overlay ini (fullscreen).
                                // Jadi change.position kurang lebih == window position jika Box fullscreen.

                                val pointerWindowPos = change.position
                                activeDropZone = dropZones.entries.firstOrNull { (_, rect) ->
                                    rect.contains(pointerWindowPos)
                                }?.key
                            },
                            onDragEnd = {
                                if (activeDropZone != null) {
                                    onTaskMoved(draggingTask!!, activeDropZone!!)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                draggingTask = null
                                activeDropZone = null
                            },
                            onDragCancel = {
                                draggingTask = null
                                activeDropZone = null
                            }
                        )
                    }
            ) {
                // Gambar "Ghost Task" yang mengikuti jari
                CleanTaskCard(
                    task = draggingTask!!,
                    onClick = {},
                    modifier = Modifier
                        .offset { IntOffset(dragOffset.x.roundToInt() - 50, dragOffset.y.roundToInt() - 50) } // Offset agar jari ada di tengah
                        .width(300.dp) // Fixed width saat dragging
                        .shadow(16.dp, RoundedCornerShape(12.dp))
                        .alpha(0.9f)
                        .zIndex(10f),
                    isDragging = true
                )
            }
        }
    }
}

@Composable
fun CleanTimeSlot(
    hour: Int,
    tasks: List<Task>,
    isDropTarget: Boolean,
    onTaskClick: (Task) -> Unit,
    onDragStart: (Task, Offset) -> Unit,
    hiddenTask: Task?
) {
    val animatedScale by animateFloatAsState(if (isDropTarget) 1.02f else 1f, label = "scale")
    val animatedColor = if (isDropTarget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp) // Lebih tinggi agar mudah di-drop
            .scale(animatedScale)
            .background(animatedColor, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp)
    ) {
        // Kolom Jam
        Box(modifier = Modifier.width(60.dp).padding(top = 8.dp)) {
            Text(
                String.format("%02d:00", hour),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if(isDropTarget) FontWeight.Bold else FontWeight.Normal,
                color = if(isDropTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // Area Tugas
        Column(
            modifier = Modifier
                .weight(1f)
                .border(
                    1.dp,
                    if(isDropTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (tasks.isEmpty() && isDropTarget) {
                Text(
                    "Lepaskan di sini",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp)
                )
            }

            tasks.forEach { task ->
                if (task.id != hiddenTask?.id) {
                    CleanTaskCard(
                        task = task,
                        onClick = { onTaskClick(task) },
                        onLongClick = { offset -> onDragStart(task, offset) }
                    )
                } else {
                    // Placeholder space untuk task yang sedang dipindahkan
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun CleanTaskCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: ((Offset) -> Unit)? = null, // Tambahan untuk deteksi drag
    isDragging: Boolean = false
) {
    var itemPosition by remember { mutableStateOf(Offset.Zero) }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                itemPosition = coordinates.positionInRoot()
            }
            .pointerInput(Unit) {
                if (onLongClick != null) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            // Kirim posisi global kartu saat ini sebagai titik awal drag
                            onLongClick(itemPosition)
                        },
                        onDrag = { _, _ -> }, // Handled by parent
                        onDragEnd = { },
                        onDragCancel = { }
                    )
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) MaterialTheme.colorScheme.primaryContainer
            else if (task.isCommitment) DeepTeal.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            if (isDragging) 2.dp else 1.dp,
            if (isDragging) MaterialTheme.colorScheme.primary
            else if (task.isCommitment) DeepTeal.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Drag Handle Indication
            if (!task.isCompleted) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                )
            }

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
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                if (task.scheduledTime != null && !isDragging) {
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

// ... (Rest of the file: PhysicalFocusModeScreen, CleanWeeklyView, CleanMonthlyView, CleanCalendarCell, etc. remain unchanged)
// Sisa kode di bawah ini adalah salinan dari kode sebelumnya agar file tetap lengkap dan bisa dicompile
// Saya sertakan PhysicalFocusModeScreen dll.

@Composable
fun PhysicalFocusModeScreen(
    task: Task,
    onComplete: (Int, Long) -> Unit,
    onCancel: () -> Unit
) {
    // ... (Keep existing implementation)
    var timeRemaining by rememberSaveable { mutableIntStateOf(60 * 25) }
    var paperclipsLeft by rememberSaveable { mutableIntStateOf(10) }
    var paperclipsMoved by rememberSaveable { mutableIntStateOf(0) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    val startTime by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

    // ... (Simplified for brevity, assume implementation is same as before)
    // To ensure compilation, I will just provide the scaffold, but in real update I would paste full code.
    // Since I must provide FULL code for replacement:

    // ... [RE-PASTING PHYSICAL FOCUS MODE FOR COMPLETENESS] ...

    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    val animatedOffsetX = remember { Animatable(0f) }
    val animatedOffsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isRunning, timeRemaining) {
        if (isRunning && timeRemaining > 0) {
            kotlinx.coroutines.delay(1000)
            timeRemaining--
        } else if (timeRemaining == 0) {
            isRunning = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = "Tutup") }
            Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1)
            TextButton(onClick = { onComplete(paperclipsMoved, startTime) }) { Text("Selesai") }
        }

        Spacer(Modifier.height(32.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp).border(4.dp, DeepTeal.copy(alpha = 0.2f), CircleShape)
        ) {
            Text(
                String.format("%02d:%02d", timeRemaining / 60, timeRemaining % 60),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = DeepTeal
            )
        }

        Spacer(Modifier.height(24.dp))

        IconButton(onClick = { isRunning = !isRunning }, modifier = Modifier.size(64.dp)) {
            Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.weight(1f))
        Text("Tarik Klip ke Kanan untuk Fokus ->", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
            ) {
                if (paperclipsLeft > 0) {
                    Text(
                        "📎",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier
                            .offset { IntOffset(animatedOffsetX.value.roundToInt() + dragOffset.x.roundToInt(), animatedOffsetY.value.roundToInt() + dragOffset.y.roundToInt()) }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if(!isRunning) isRunning = true
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        if (dragOffset.x > 150) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            paperclipsLeft--
                                            paperclipsMoved++
                                            scope.launch {
                                                animatedOffsetX.snapTo(0f)
                                                animatedOffsetY.snapTo(0f)
                                            }
                                            dragOffset = Offset.Zero
                                            if(paperclipsLeft == 0) onComplete(paperclipsMoved, startTime)
                                        } else {
                                            scope.launch {
                                                val endX = dragOffset.x
                                                val endY = dragOffset.y
                                                animatedOffsetX.snapTo(endX)
                                                animatedOffsetY.snapTo(endY)
                                                dragOffset = Offset.Zero
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
                Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                    Surface(color = MaterialTheme.colorScheme.surface, shape = CircleShape, shadowElevation = 2.dp) {
                        Text("$paperclipsLeft", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp).background(SuccessGreen.copy(alpha = 0.1f), RoundedCornerShape(24.dp)).border(2.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📎", style = MaterialTheme.typography.displayMedium, color = SuccessGreen.copy(alpha = 0.5f))
                    Text("$paperclipsMoved", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SuccessGreen)
                }
            }
        }
        Spacer(Modifier.height(48.dp))
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
    val weekDays = remember(firstDayOfWeek) { (0..6).map { firstDayOfWeek.plusDays(it.toLong()) } }

    Column(modifier = Modifier.fillMaxSize()) {
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

@Composable
fun CleanMonthlyView(
    viewModel: PlannerViewModel,
    uiState: PlannerUiState,
    onTaskClick: (Task) -> Unit
) {
    val tasksByDate = remember(uiState.tasksForMonth) {
        uiState.tasksForMonth.groupBy { try { LocalDate.parse(it.scheduledDate) } catch (e: Exception) { null } }
    }

    val firstDayOfMonth = uiState.selectedDate.with(TemporalAdjusters.firstDayOfMonth())
    val paddingDays = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = firstDayOfMonth.lengthOfMonth()

    Column(modifier = Modifier.fillMaxSize()) {
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(7) { index ->
                Text(
                    DayOfWeek.of(index + 1).getDisplayName(TextStyle.SHORT, Locale("id")),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            items(paddingDays) { Box(modifier = Modifier.aspectRatio(1f)) }
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

        Text(
            "Tugas pada ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM", Locale("id")))}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

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
