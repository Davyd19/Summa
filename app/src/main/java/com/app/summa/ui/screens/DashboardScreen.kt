package com.app.summa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.BrutalBlack
import com.app.summa.ui.theme.BrutalBlue
import com.app.summa.ui.theme.BrutalWhite
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
    onNavigateToHabits: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val today = remember { LocalDate.now() }
    val dayLabel = today.dayOfMonth.toString()
    val monthLabel = today.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()

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
                BrutalistHeaderBadge(text = "CONTROL_PANEL_V1.0")
                // Status Dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFFFF6B6B), CircleShape)
                        .brutalBorder(strokeWidth = 1.dp, cornerRadius = 10.dp)
                )
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
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
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

        // 5. GRID LAYOUT (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // ROW 1
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // TASKS CARD
                    BrutalistCard(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onNavigateToPlanner() },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.Black)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, null, tint = Color.White)
                                }
                                Text("HIGH", style = MaterialTheme.typography.labelSmall, modifier = Modifier.background(Color.Black).padding(horizontal=4.dp, vertical=2.dp), color = Color.White)
                            }
                            
                            Column {
                                Text("PENDING: ${uiState.activeTasks}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text("TASKS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    // SESSION CARD (BLUE)
                    BrutalistCard(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onModeSelected("Fokus") }, // Trigger Focus
                        containerColor = BrutalBlue,
                        contentColor = BrutalWhite
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.Timer, null, modifier = Modifier.size(32.dp))
                            
                            Column {
                                Text("SESSION", style = MaterialTheme.typography.labelSmall, color = BrutalWhite.copy(alpha = 0.7f))
                                Text("00:25:00", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                        }
                    }
                }

                // ROW 2
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // MEETING / PLANNER CARD
                    BrutalistCard(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onNavigateToPlanner() }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(32.dp))
                            
                            Column {
                                Text("NEXT: 2PM", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("MEETING", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                                Text("Prod. Sync", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }

                    // NOTES CARD
                    BrutalistCard(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onNavigateToNotes() }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(32.dp))
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
                            }
                            
                            Column {
                                Text("QUICK ENTRY", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("NOTES", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        // 6. SYSTEM FOOTER
        item {
            BrutalistSystemFooter()
        }
        
        // Spacer for Bottom Nav
        item {
             Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
