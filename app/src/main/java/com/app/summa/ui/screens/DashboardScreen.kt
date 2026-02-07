package com.app.summa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.BrutalBlack
import com.app.summa.ui.theme.BrutalBlue
import com.app.summa.ui.theme.BrutalWhite
import com.app.summa.ui.theme.SuccessGreen
import com.app.summa.ui.viewmodel.DashboardViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToHabitDetail: (com.app.summa.data.model.HabitItem) -> Unit = {},
    onNavigateToMoney: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToReflections: () -> Unit = {},
    onNavigateToIdentityProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToHabits: () -> Unit = {},
    onNavigateToAddTask: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {},
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToAddIdentity: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val today = remember { LocalDate.now() }
    val dayLabel = today.dayOfMonth.toString()
    val monthLabel = today.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()

    var showTransactionSheet by remember { mutableStateOf(false) }

    // Main Container
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // 1. TOP CONTROL BAR
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrutalistHeaderBadge(text = "SUMMA OS v1.0")

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // MODE TOGGLE
                    Row(
                        modifier = Modifier
                            .selectableGroup()
                            .brutalBorder(strokeWidth = 2.dp, cornerRadius = 24.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val modes = listOf("Normal", "Fokus")
                        modes.forEach { mode ->
                            val isSelected = currentMode == mode
                            Box(
                                modifier = Modifier
                                    .selectable(
                                        selected = isSelected,
                                        onClick = { onModeSelected(mode) },
                                        role = Role.RadioButton
                                    )
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // SETTINGS BUTTON
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(36.dp) // Match height of neighbor roughly (24+padding)
                            .brutalBorder(strokeWidth = 2.dp, cornerRadius = 36.dp) // Circular
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, "Pengaturan", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // 2. GIANT HEADER
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Black,
                            fontSize = 64.sp,
                            lineHeight = 64.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Black,
                            fontSize = 64.sp,
                            lineHeight = 64.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    BrutalistDigitalClock()
                }
            }
        }

        // 3. DIVIDER
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.onBackground)
            )
        }

        // 4. SYSTEM STATUS CARD
        item {
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("SYSTEM_STATUS", fontWeight = FontWeight.Bold)
                        }
                        Text("${(uiState.todayProgress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // BLOCK PROGRESS BAR
                    // Assume 10 blocks max for visual balance
                    val maxBlocks = 10
                    val currentBlocks = (uiState.todayProgress * maxBlocks).toInt()
                    BrutalistBlockProgressBar(current = currentBlocks, max = maxBlocks)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("DAILY_QUOTA", style = MaterialTheme.typography.labelSmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        Text("${uiState.completedHabits}/${uiState.todayHabits.size} TASKS", style = MaterialTheme.typography.labelSmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }

        // 5. GRID LAYOUT
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // ROW 1
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // SUMMA POINTS CARD (Green)
                    BrutalistCard(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        containerColor = MaterialTheme.colorScheme.primary, // Using primary (Teal/Green) like original
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Summa Points", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)

                            Text(
                                text = "${uiState.summaPoints}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            LinearProgressIndicator(
                                progress = (uiState.summaPoints % 1000) / 1000f,
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }

                    // FOCUS CLIP / SESSION CARD (Blue)
                    BrutalistCard(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onNavigateToFocus() }, // Navigate to Focus Mode
                        containerColor = BrutalBlue,
                        contentColor = BrutalWhite
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.Timer, null, modifier = Modifier.size(32.dp))

                            Column {
                                Text("KLIP FOKUS", style = MaterialTheme.typography.labelSmall, color = BrutalWhite.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                                Text("${uiState.totalPaperclips}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                // ROW 2
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // START DAY / ADD TASK CARD
                    BrutalistCard(
                        modifier = Modifier
                            .weight(2f) // Wide card
                            .height(100.dp)
                            .clickable { onNavigateToAddTask() },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("Mulai Hari", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Tambah tugas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                        }
                    }
                }
            }
        }

        // 6. HABITS WIDGET (RESTORATION)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("KEBIASAAN HARI INI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "${uiState.completedHabits}/${uiState.todayHabits.size}",
                        modifier = Modifier.padding(horizontal=8.dp, vertical=2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (uiState.todayHabits.isEmpty()) {
                BrutalistCard(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada kebiasaan terjadwal", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.todayHabits.forEach { habit ->
                        // Simple Brutalist Habit Item row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .brutalBorder(strokeWidth = 2.dp, cornerRadius = 8.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .clickable { onNavigateToHabits() } // Or navigate to toggle
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(habit.icon, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(12.dp))
                                Text(habit.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${habit.currentCount}/${habit.targetCount}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                // Checkbox visualization
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(2.dp, if(habit.currentCount >= habit.targetCount) SuccessGreen else MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                        .background(if(habit.currentCount >= habit.targetCount) SuccessGreen else Color.Transparent, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if(habit.currentCount >= habit.targetCount) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. QUICK ACTIONS
        item {
            Text("AKSES CEPAT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            BrutalistCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable(onClick = onNavigateToAddTransaction), // Clickable moved to Card
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AttachMoney, null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(8.dp))
                        Text("CATAT TRANSAKSI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))

            // NEW: Add Identity Action
            BrutalistCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable(onClick = onNavigateToAddIdentity),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                 Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(8.dp))
                        Text("IDENTITAS BARU", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // 6. SYSTEM FOOTER
        item {
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        val level = (uiState.summaPoints / 1000) + 1
                        Text("LEVEL $level", fontWeight=FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusText = if (uiState.todayProgress >= 0.8f) "OPTIMAL" else if (uiState.todayProgress >= 0.5f) "NORMAL" else "LOW POWER"
                        val statusColor = if (uiState.todayProgress >= 0.8f) SuccessGreen else if (uiState.todayProgress >= 0.5f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                        Text("STATUS: $statusText", fontWeight=FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color=statusColor)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(8.dp).background(statusColor, CircleShape))
                    }
                }
            }
        }

        // Spacer for Bottom Nav
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
