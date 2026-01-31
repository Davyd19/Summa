package com.app.summa.ui.screens

import com.app.summa.ui.components.*

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.summa.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun UniversalFocusModeScreen(
    onBack: () -> Unit,
    viewModel: com.app.summa.ui.viewmodel.FocusViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSetupPhase by remember { mutableStateOf(true) }

    if (isSetupPhase) {
        FocusSetupScreen(
            availableHabits = uiState.availableHabits,
            selectedHabitId = uiState.selectedHabitId,
            onHabitSelect = { viewModel.selectHabit(it) },
            onStart = { duration, isClipMode, clips -> 
                viewModel.initializeSession(if(isClipMode) clips else duration, isClipMode)
                viewModel.startTimer() // Optional auto start
                isSetupPhase = false 
            },
            onCancel = onBack
        )
    } else {
        FocusRunningScreen(
            uiState = uiState,
            onAction = { viewModel.movePaperclip() },
            onPauseResume = { if(uiState.isRunning) viewModel.pauseTimer() else viewModel.startTimer() },
            onComplete = { 
                viewModel.completeSession()
                onBack() 
            },
            onCancel = onBack
        )
    }
}

// ... FocusSetupScreen (Tidak berubah, dianggap sama seperti sebelumnya) ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSetupScreen(
    availableHabits: List<com.app.summa.data.model.HabitItem>,
    selectedHabitId: Long?,
    onHabitSelect: (Long?) -> Unit,
    onStart: (Int, Boolean, Int) -> Unit, // Duration/Clips, IsClipMode, ClipCount
    onCancel: () -> Unit
) {
    var durationMinutes by remember { mutableStateOf(25f) }
    var isClipMode by remember { mutableStateOf(false) }
    var targetClips by remember { mutableStateOf(10f) }
    var habitExpanded by remember { mutableStateOf(false) }
    
    val selectedHabitName = availableHabits.find { it.id == selectedHabitId }?.name ?: "Tanpa Kebiasaan (Umum)"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SETUP FOKUS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(32.dp))

        // 1. Habit Selector
        ExposedDropdownMenuBox(
            expanded = habitExpanded,
            onExpandedChange = { habitExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedHabitName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Hubungkan Kebiasaan") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = habitExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = run {
                     val c = MaterialTheme.colorScheme
                     OutlinedTextFieldDefaults.colors(focusedBorderColor = c.primary, unfocusedBorderColor = c.outline)
                }
            )
            ExposedDropdownMenu(
                expanded = habitExpanded,
                onDismissRequest = { habitExpanded = false }
            ) {
                 DropdownMenuItem(text = { Text("Tanpa Kebiasaan") }, onClick = { onHabitSelect(null); habitExpanded = false })
                 availableHabits.forEach { habit ->
                     DropdownMenuItem(text = { Text(habit.name) }, onClick = { onHabitSelect(habit.id); habitExpanded = false })
                 }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // 2. Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth().brutalBorder().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(if(isClipMode) "MODE: KLIP (REPETISI)" else "MODE: TIMER (PODOMORO)", fontWeight = FontWeight.Bold)
                Text(if(isClipMode) "Geser manual untuk hitung" else "Hitung mundur otomatis", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = isClipMode, onCheckedChange = { isClipMode = it })
        }
        
        Spacer(Modifier.height(24.dp))

        // 3. Slider
        if (isClipMode) {
            Text("TARGET KLIP: ${targetClips.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Slider(value = targetClips, onValueChange = { targetClips = it }, valueRange = 1f..100f)
        } else {
             Text("${durationMinutes.toInt()} MENIT", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = com.app.summa.ui.theme.DeepTeal)
             Slider(value = durationMinutes, onValueChange = { durationMinutes = it }, valueRange = 5f..120f, steps = 22) // 5 min steps approach
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = { onStart(durationMinutes.toInt(), isClipMode, targetClips.toInt()) },
            modifier = Modifier.fillMaxWidth().height(56.dp).brutalBorder(cornerRadius=4.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("MULAI FOKUS", fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(16.dp))
        
        TextButton(onClick = onCancel) { Text("KEMBALI") }
    }
}

@Composable
fun FocusRunningScreen(
    uiState: com.app.summa.ui.viewmodel.FocusUiState,
    onAction: () -> Unit,
    onPauseResume: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    // If timer logic is in ViewModel, we just display state
    val formattedTime = String.format("%02d:%02d", uiState.timeRemaining / 60, uiState.timeRemaining % 60)
    
     // Drag Logic reuse (Simplified for brevity, or keep full logic if needed)
     // For this iteration, I'll simplify to just Clickable/Draggable logic calls viewModel.movePaperclip()
     
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
         // Header
         Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
             Icon(Icons.Default.Close, "Batal", modifier = Modifier.clickable { onCancel() })
             Text(if(uiState.isClipMode) "REPETISI" else "FOKUS", fontWeight = FontWeight.Bold)
             Text("SELESAI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onComplete() })
         }
         
         Spacer(Modifier.height(48.dp))
         
         // Timer Ring
         Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp).brutalBorder(cornerRadius = 200.dp, strokeWidth=4.dp)) {
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                 Text(formattedTime, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
                 Text(if(uiState.isRunning) "BERJALAN..." else "JEDA", fontWeight = FontWeight.Bold, color = if(uiState.isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
             }
         }
         
         Spacer(Modifier.height(32.dp))
         
         // Controls
         Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
              Button(onClick = onPauseResume, modifier = Modifier.weight(1f).brutalBorder()) {
                  Text(if(uiState.isRunning) "PAUSE" else "RESUME")
              }
         }
         
         Spacer(Modifier.height(32.dp))
         
         if (uiState.isClipMode) {
             // Paperclip UI
             Text("KLIP: ${uiState.paperclipsMoved} / ${uiState.targetClips}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
             Spacer(Modifier.height(16.dp))
             Button(onClick = onAction, modifier = Modifier.fillMaxWidth().height(64.dp).brutalBorder()) {
                 Text("GESER KLIP (+1)", fontSize = 20.sp)
             }
         } else {
             Text("Stay Focused.", style = MaterialTheme.typography.bodyLarge, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
         }
    }
}
