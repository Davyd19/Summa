package com.app.summa.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items // Pastikan import ini ada untuk Grid
import androidx.compose.foundation.lazy.items // Pastikan import ini ada untuk List
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.Identity
import com.app.summa.data.model.Task
import com.app.summa.ui.components.BrutalIconAction
import com.app.summa.ui.components.BrutalTopAppBar
import com.app.summa.ui.components.brutalBorder
import com.app.summa.ui.components.TaskInputSheet
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    currentMode: String = "Normal",
    noteTitle: String? = null,
    noteContent: String? = null,
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

    // --- LOGIKA FILTERING DATA ---
    // Karena ViewModel memuat data sebulan, kita filter manual untuk view harian
    val tasksForDay = remember(uiState.tasks, uiState.selectedDate) {
        uiState.tasks.filter { it.scheduledDate == uiState.selectedDate.toString() }
            .sortedBy { it.scheduledTime }
    }

    val filteredTasksForDay = remember(tasksForDay, currentMode) {
        if (currentMode == "Fokus") {
            tasksForDay.filter { it.isCommitment }
        } else {
            tasksForDay
        }
    }

    // Menangani argumen navigasi (Create Task from Note)
    LaunchedEffect(noteTitle, noteContent) {
        if (!noteTitle.isNullOrBlank() || !noteContent.isNullOrBlank()) {
            showAddTaskSheet = true
        }
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Planner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                if (currentMode == "Fokus") {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        modifier = Modifier.brutalBorder(strokeWidth = 2.dp, radius = 6.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "MODE FOKUS",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            Text(
                                uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM", Locale("id"))),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    actions = {
                        BrutalIconAction(
                            icon = Icons.Default.Today,
                            contentDescription = "Pilih Tanggal",
                            onClick = { showDatePicker = true }
                        )
                        BrutalIconAction(
                            icon = Icons.Default.Add,
                            contentDescription = "Tambah Tugas",
                            onClick = { showAddTaskSheet = true }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // View Mode Switcher
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("day" to "Harian", "week" to "Mingguan", "month" to "Bulanan").forEach { (mode, label) ->
                            FilterChip(
                                selected = viewMode == mode,
                                onClick = { viewMode = mode },
                                label = { Text(label, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f).brutalBorder(strokeWidth = 2.dp, radius = 6.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                border = null
                            )
                        }
                    }
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    when (viewMode) {
                        "day" -> InteractiveDailyView(
                            tasks = filteredTasksForDay,
                            identities = uiState.identities,
                            onTaskClick = { selectedTask = it },
                            onTaskMoved = { task, newHour -> viewModel.moveTaskToTime(task, newHour) }
                        )
                        "week" -> CleanWeeklyView(
                            viewModel = viewModel,
                            uiState = uiState,
                            onTaskClick = { selectedTask = it },
                            currentMode = currentMode,
                            // Pass tasks for day logic manual
                            dailyTasks = filteredTasksForDay
                        )
                        "month" -> CleanMonthlyView(
                            viewModel = viewModel,
                            uiState = uiState,
                            onTaskClick = { selectedTask = it },
                            currentMode = currentMode,
                            dailyTasks = filteredTasksForDay
                        )
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
        // Prepare initial values from nav args if any
        val initTitle = noteTitle ?: ""
        val initDesc = noteContent ?: ""

        // Note: TaskInputSheet currently handles state internally.
        // For 'Edit', we would need to pass task object.
        // Since TaskInputSheet definition isn't fully flexible in previous snippets,
        // we use it as is for creation.
        TaskInputSheet(
            identities = uiState.identities,
            onDismiss = {
                showAddTaskSheet = false
                viewModel.clearInitialTask()
            },
            onSave = { title, desc, time, isCommitment, identityId, twoMin ->
                // Jika sedang membuat dari Note, gunakan deskripsi dari note
                val finalTitle = if (title.isBlank()) initTitle else title
                val finalDesc = if (desc.isBlank()) initDesc else desc

                viewModel.addTask(finalTitle, finalDesc, time, isCommitment, twoMin, identityId)
                showAddTaskSheet = false
                viewModel.clearInitialTask()
            }
        )
    }
}

// --- INTERACTIVE DAILY VIEW ---

@Composable
fun InteractiveDailyView(
    tasks: List<Task>,
    identities: List<Identity>,
    onTaskClick: (Task) -> Unit,
    onTaskMoved: (Task, Int) -> Unit
) {
    val tasksByHour = remember(tasks) {
        tasks.groupBy { try { LocalTime.parse(it.scheduledTime ?: "00:00").hour } catch (e: Exception) { 0 } }
    }

    var draggingTask by remember { mutableStateOf<Task?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val dropZones = remember { mutableStateMapOf<Int, Rect>() }
    var activeDropZone by remember { mutableStateOf<Int?>(null) }

    val haptic = LocalHapticFeedback.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(24) { hour ->
                Box(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        dropZones[hour] = coordinates.boundsInWindow()
                    }
                ) {
                    CleanTimeSlot(
                        hour = hour,
                        tasks = tasksByHour[hour] ?: emptyList(),
                        identities = identities,
                        isDropTarget = activeDropZone == hour,
                        onTaskClick = onTaskClick,
                        onDragStart = { task, offset ->
                            draggingTask = task
                            dragOffset = offset
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        hiddenTask = draggingTask
                    )
                }
            }
            Spacer(Modifier.height(100.dp))
        }

        if (draggingTask != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
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
                            onDragCancel = { draggingTask = null; activeDropZone = null }
                        )
                    }
            ) {
                CleanTaskCard(
                    task = draggingTask!!,
                    identity = identities.find { it.id == draggingTask!!.relatedIdentityId },
                    onClick = {},
                    modifier = Modifier
                        .offset { IntOffset(dragOffset.x.roundToInt() - 50, dragOffset.y.roundToInt() - 50) }
                        .width(300.dp)
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
    identities: List<Identity>,
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
            .heightIn(min = 72.dp)
            .scale(animatedScale)
            .background(animatedColor, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp)
    ) {
        Box(modifier = Modifier.width(60.dp).padding(top = 8.dp)) {
            Text(
                String.format("%02d:00", hour),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if(isDropTarget) FontWeight.Bold else FontWeight.Normal,
                color = if(isDropTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .drawBehind {
                    drawLine(color = Color.Gray.copy(alpha = 0.1f), start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                }
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (tasks.isEmpty() && isDropTarget) {
                Text("Pindahkan ke sini", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp))
            }

            tasks.forEach { task ->
                if (task.id != hiddenTask?.id) {
                    CleanTaskCard(
                        task = task,
                        identity = identities.find { it.id == task.relatedIdentityId },
                        onClick = { onTaskClick(task) },
                        onLongClick = { offset -> onDragStart(task, offset) }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)))
                }
            }
        }
    }
}

// --- TASK CARD ---
@Composable
fun CleanTaskCard(
    task: Task,
    identity: Identity? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: ((Offset) -> Unit)? = null,
    isDragging: Boolean = false
) {
    var itemPosition by remember { mutableStateOf(Offset.Zero) }

    val containerColor = when {
        isDragging -> MaterialTheme.colorScheme.primaryContainer
        task.isCompleted -> SuccessGreenBg.copy(alpha = 0.5f)
        task.isCommitment -> CommitmentContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        task.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        task.isCommitment -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderModifier = if (!task.isCommitment && !task.isCompleted && !isDragging) {
        Modifier.drawBehind {
            val stroke = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))
            drawRoundRect(color = AspirationColor, style = stroke, cornerRadius = CornerRadius(12.dp.toPx()))
        }
    } else Modifier

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates -> itemPosition = coordinates.positionInRoot() }
            .pointerInput(Unit) {
                if (onLongClick != null) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onLongClick(itemPosition) },
                        onDrag = { _, _ -> },
                        onDragEnd = { },
                        onDragCancel = { }
                    )
                }
            }
            .then(borderModifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else if (task.isCommitment) 4.dp else 0.dp),
        border = null
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!task.isCompleted) {
                Icon(Icons.Default.DragHandle, null, tint = contentColor.copy(alpha = 0.3f), modifier = Modifier.size(16.dp).padding(end = 4.dp))
            }

            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape).background(if (task.isCompleted) SuccessGreen else if (task.isCommitment) GoldAccent else AspirationColor)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (task.isCommitment) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    color = contentColor,
                    textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.scheduledTime != null && !isDragging) {
                        Text(task.scheduledTime, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
                        Spacer(Modifier.width(8.dp))
                    }

                    if (!task.isCompleted) {
                        Surface(
                            color = if(task.isCommitment) Color.Black.copy(alpha=0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(if (task.isCommitment) "KOMITMEN" else "ASPIRASI", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), color = if(task.isCommitment) Color.White.copy(alpha=0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (identity != null) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = if(task.isCommitment) GoldAccent.copy(alpha=0.3f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.5f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(8.dp), tint = if(task.isCommitment) GoldLight else MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    identity.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = if(task.isCommitment) GoldLight else MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Icon(if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.ArrowForward, null, tint = if (task.isCompleted) SuccessGreen else contentColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}

// --- WEEKLY & MONTHLY VIEW ---

@Composable
fun CleanWeeklyView(
    viewModel: PlannerViewModel,
    uiState: PlannerUiState,
    onTaskClick: (Task) -> Unit,
    currentMode: String,
    dailyTasks: List<Task> // Tugas untuk hari yang dipilih
) {
    val selectedDate = uiState.selectedDate
    val firstDayOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDays = remember(firstDayOfWeek) { (0..6).map { firstDayOfWeek.plusDays(it.toLong()) } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.selectDate(firstDayOfWeek.minusWeeks(1)) }) { Icon(Icons.Default.ChevronLeft, "Prev") }
            Text("${firstDayOfWeek.format(DateTimeFormatter.ofPattern("d MMM"))} - ${weekDays.last().format(DateTimeFormatter.ofPattern("d MMM"))}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { viewModel.selectDate(firstDayOfWeek.plusWeeks(1)) }) { Icon(Icons.Default.ChevronRight, "Next") }
        }

        LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(weekDays.size) { index ->
                CleanDayCell(date = weekDays[index], isSelected = weekDays[index].isEqual(selectedDate), isToday = weekDays[index].isEqual(LocalDate.now()), onClick = { viewModel.selectDate(weekDays[index]) })
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (dailyTasks.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) { Text(if(currentMode == "Fokus") "Tidak ada Komitmen hari ini" else "Tidak ada tugas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) } }
            } else {
                items(dailyTasks) { task ->
                    CleanTaskCard(
                        task = task,
                        identity = uiState.identities.find { it.id == task.relatedIdentityId },
                        onClick = { onTaskClick(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun CleanMonthlyView(
    viewModel: PlannerViewModel,
    uiState: PlannerUiState,
    onTaskClick: (Task) -> Unit,
    currentMode: String,
    dailyTasks: List<Task>
) {
    // Kelompokkan tugas bulanan berdasarkan tanggal
    val tasksByDate = remember(uiState.tasks) {
        uiState.tasks.groupBy { try { LocalDate.parse(it.scheduledDate) } catch (e: Exception) { null } }
    }

    val firstDayOfMonth = uiState.selectedDate.with(TemporalAdjusters.firstDayOfMonth())
    val paddingDays = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = firstDayOfMonth.lengthOfMonth()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.selectDate(firstDayOfMonth.minusMonths(1)) }) { Icon(Icons.Default.ChevronLeft, "Prev") }
            Text(firstDayOfMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id"))), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.selectDate(firstDayOfMonth.plusMonths(1)) }) { Icon(Icons.Default.ChevronRight, "Next") }
        }

        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(7) { index -> Text(DayOfWeek.of(index + 1).getDisplayName(TextStyle.SHORT, Locale("id")), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
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
        Text("Tugas pada ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM", Locale("id")))}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (dailyTasks.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) { Text(if(currentMode=="Fokus") "Fokus terjaga. Tidak ada komitmen." else "Tidak ada tugas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) } }
            } else {
                items(dailyTasks) { task ->
                    CleanTaskCard(
                        task = task,
                        identity = uiState.identities.find { it.id == task.relatedIdentityId },
                        onClick = { onTaskClick(task) }
                    )
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