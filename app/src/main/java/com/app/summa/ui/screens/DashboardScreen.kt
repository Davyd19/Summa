package com.app.summa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    onNavigateToAddTransaction: () -> Unit = {}
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
                BrutalistHeaderBadge(text = "SUMMA OS v1.0")
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // MODE TOGGLE
                    Row(
                        modifier = Modifier
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
                                    .clickable { onModeSelected(mode) }
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

                    // SETTINGS BUTTON (Moved to Top)
                    BrutalIconAction(
                        icon = Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(40.dp) // Slightly smaller than standard fab, fits header
                    )
                }
            }
        }
// ... (Lines 108-363 unchanged)
        // 7. QUICK ACTIONS
        item {
             Text("AKSES CEPAT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
             Spacer(Modifier.height(12.dp))
             Button(
                 onClick = onNavigateToAddTransaction,
                 shape = RoundedCornerShape(8.dp),
                 modifier = Modifier.fillMaxWidth().height(56.dp).brutalBorder(),
                 colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
             ) {
                 Icon(Icons.Default.AttachMoney, null, tint = MaterialTheme.colorScheme.onSurface)
                 Spacer(Modifier.width(8.dp))
                 Text("CATAT TRANSAKSI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
