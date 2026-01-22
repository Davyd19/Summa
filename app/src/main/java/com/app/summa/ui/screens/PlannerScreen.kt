package com.app.summa.ui.screens

import com.app.summa.ui.components.*

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
                BrutalistCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        "week" -> BrutalistWeeklyView(
                            viewModel = viewModel,
                            uiState = uiState,
                            onTaskClick = { selectedTask = it },
                            currentMode = currentMode,
                            // Pass tasks for day logic manual
                            dailyTasks = filteredTasksForDay
                        )
                        "month" -> BrutalistMonthlyView(
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(24) { hour ->
                Box(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        dropZones[hour] = coordinates.boundsInWindow()
                    }
                ) {
                    BrutalistTimeSlot(
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
                BrutalistTaskCard(
                    task = draggingTask!!,
                    identity = identities.find { it.id == draggingTask!!.relatedIdentityId },
                    onClick = {},
                    modifier = Modifier
                        .offset { IntOffset(dragOffset.x.roundToInt() - 50, dragOffset.y.roundToInt() - 50) }
                        .width(300.dp)
                        .shadow(16.dp, RoundedCornerShape(8.dp))
                        .alpha(0.9f)
                        .zIndex(10f),
                    isDragging = true
                )
            }
        }
    }
}

@Composable
fun BrutalistTimeSlot(
    hour: Int,
    tasks: List<Task>,
    identities: List<Identity>,
    isDropTarget: Boolean,
    onTaskClick: (Task) -> Unit,
    onDragStart: (Task, Offset) -> Unit,
    hiddenTask: Task?
) {
    val animatedScale by animateFloatAsState(if (isDropTarget) 1.02f else 1f, label = "scale")
    
    // Brutalist: Solid border for time slots
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .scale(animatedScale)
            .brutalBorder(strokeWidth = if(isDropTarget) 4.dp else 2.dp, color = if(isDropTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            .background(if(isDropTarget) MaterialTheme.colorScheme.primary.copy(alpha=0.05f) else Color.Transparent)
            .padding(8.dp)
    ) {
        // Time Column
        Column(
            modifier = Modifier.width(50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                String.format("%02d:00", hour),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if(isDropTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Vertical Divider
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        )
        
        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (tasks.isEmpty() && isDropTarget) {
                Text("Drop Here", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
            } else if (tasks.isEmpty()) {
                Text("Free Slot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.padding(top = 4.dp))
            }

            tasks.forEach { task ->
                if (task.id != hiddenTask?.id) {
                    BrutalistTaskCard(
                        task = task,
                        identity = identities.find { it.id == task.relatedIdentityId },
                        onClick = { onTaskClick(task) },
                        onLongClick = { offset -> onDragStart(task, offset) }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(50.dp).brutalBorder(strokeWidth=1.dp).background(Color.Gray.copy(alpha=0.1f)))
                }
            }
        }
    }
}

// --- TASK CARD ---
@Composable
fun BrutalistTaskCard(
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
        task.isCompleted -> SuccessGreenBg
        task.isCommitment -> CommitmentContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        task.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        task.isCommitment -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val borderColor = if (task.isCommitment) Color.Black else MaterialTheme.colorScheme.onBackground

    Surface(
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
            .brutalBorder(strokeWidth = if(task.isCommitment) 4.dp else 2.dp, color = borderColor)
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = contentColor,
                    textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )

                // Meta Row
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    if (task.scheduledTime != null && !isDragging) {
                        BrutalistTag(text = task.scheduledTime, color = contentColor.copy(alpha=0.7f))
                        Spacer(Modifier.width(6.dp))
                    }
                    
                    BrutalistTag(
                         text = if (task.isCommitment) "COMMITMENT" else "ASPIRATION",
                         color = if (task.isCommitment) GoldAccent else contentColor.copy(alpha=0.7f)
                    )

                    if (identity != null) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = contentColor.copy(alpha=0.7f))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            identity.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha=0.7f),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))
            Icon(
                if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.ArrowForward, 
                null, 
                tint = if (task.isCompleted) SuccessGreen else contentColor, 
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// --- WEEKLY & MONTHLY VIEW ---

@Composable
fun BrutalistWeeklyView(
    viewModel: PlannerViewModel,
    uiState: PlannerUiState,
    onTaskClick: (Task) -> Unit,
    currentMode: String,
    dailyTasks: List<Task>
) {
    val selectedDate = uiState.selectedDate
    val firstDayOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDays = remember(firstDayOfWeek) { (0..6).map { firstDayOfWeek.plusDays(it.toLong()) } }

    Column(modifier = Modifier.fillMaxSize()) {
        // Navigation Header
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BrutalIconAction(icon = Icons.Default.ChevronLeft, contentDescription = "Prev", onClick = { viewModel.selectDate(firstDayOfWeek.minusWeeks(1)) })
            Text(
                "${firstDayOfWeek.format(DateTimeFormatter.ofPattern("d MMM"))} - ${weekDays.last().format(DateTimeFormatter.ofPattern("d MMM"))}", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold
            )
            BrutalIconAction(icon = Icons.Default.ChevronRight, contentDescription = "Next", onClick = { viewModel.selectDate(firstDayOfWeek.plusWeeks(1)) })
        }

        LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(weekDays.size) { index ->
                BrutalistDayCell(date = weekDays[index], isSelected = weekDays[index].isEqual(selectedDate), isToday = weekDays[index].isEqual(LocalDate.now()), onClick = { viewModel.selectDate(weekDays[index]) })
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 2.dp)

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (dailyTasks.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) { Text(if(currentMode == "Fokus") "No Commitments" else "No Tasks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) } }
            } else {
                items(dailyTasks) { task ->
                    BrutalistTaskCard(
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
fun BrutalistMonthlyView(
    viewModel: PlannerViewModel,
    uiState: PlannerUiState,
    onTaskClick: (Task) -> Unit,
    currentMode: String,
    dailyTasks: List<Task>
) {
    val tasksByDate = remember(uiState.tasks) {
        uiState.tasks.groupBy { try { LocalDate.parse(it.scheduledDate) } catch (e: Exception) { null } }
    }

    val firstDayOfMonth = uiState.selectedDate.with(TemporalAdjusters.firstDayOfMonth())
    val paddingDays = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = firstDayOfMonth.lengthOfMonth()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BrutalIconAction(icon = Icons.Default.ChevronLeft, contentDescription = "Prev", onClick = { viewModel.selectDate(firstDayOfMonth.minusMonths(1)) })
            Text(firstDayOfMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id"))), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            BrutalIconAction(icon = Icons.Default.ChevronRight, contentDescription = "Next", onClick = { viewModel.selectDate(firstDayOfMonth.plusMonths(1)) })
        }

        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(7) { index -> Text(DayOfWeek.of(index + 1).getDisplayName(TextStyle.SHORT, Locale("id")), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold) }
            items(paddingDays) { Box(modifier = Modifier.aspectRatio(1f)) }
            items(daysInMonth) { dayIndex ->
                val currentDate = firstDayOfMonth.plusDays(dayIndex.toLong())
                val tasksOnDay = tasksByDate[currentDate] ?: emptyList()

                BrutalistCalendarCell(
                    day = dayIndex + 1,
                    taskCount = tasksOnDay.size,
                    isToday = currentDate.isEqual(LocalDate.now()),
                    isSelected = currentDate.isEqual(uiState.selectedDate),
                    onClick = { viewModel.selectDate(currentDate) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 2.dp)
        Text("TASKS FOR ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM", Locale("id"))).uppercase()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (dailyTasks.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) { Text(if(currentMode=="Fokus") "No commitments" else "No tasks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) } }
            } else {
                items(dailyTasks) { task ->
                    BrutalistTaskCard(
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
fun BrutalistDayCell(date: LocalDate, isSelected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("id"))
    val dayNumber = date.dayOfMonth.toString()
    
    Surface(
        onClick = onClick,
        modifier = Modifier.width(62.dp).brutalBorder(strokeWidth = if(isSelected) 3.dp else 2.dp, color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha=0.1f) else MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(dayName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
            Text(dayNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun BrutalistCalendarCell(day: Int, taskCount: Int, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.aspectRatio(1f).brutalBorder(strokeWidth = if(isSelected) 3.dp else 1.dp, color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha=0.1f) else if(isToday) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (taskCount > 0) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(minOf(taskCount, 3)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .brutalBorder(strokeWidth = 1.dp)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else DeepTeal)
                        )
                    }
                }
            }
        }
    }
}
