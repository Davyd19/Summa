package com.app.summa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.Identity
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.HabitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    viewModel: HabitViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🎯") }
    var target by remember { mutableStateOf("1") }
    var selectedIdentity by remember { mutableStateOf<Identity?>(null) }
    var cue by remember { mutableStateOf("") }
    var cue by remember { mutableStateOf("") }
    var reminder by remember { mutableStateOf("") }
    var twoMinuteRuleText by remember { mutableStateOf("") } // NEW: 2 Minute Rule Input
    
    // Dropdown state
    var identityExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BrutalTopAppBar(
                title = "HABIT BARU",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBack
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .brutalBorder(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.onBackground)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                BrutalButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addHabit(
                                name = name,
                                icon = icon,
                                targetCount = target.toIntOrNull() ?: 1,
                                relatedIdentityId = selectedIdentity?.id,
                                cue = cue,
                                reminderTime = reminder
                            )
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    text = "MULAI PROTOKOL"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .brutalBorder(cornerRadius = 100.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .clickable { /* TODO: Emoji Picker */ },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 32.sp)
                    // Hint indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(16.dp))
                    }
                }
                
                Column {
                    Text(
                        "Identitas Visual",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "Pilih Ikon",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Form Fields
            BrutalistTextField(
                value = name,
                onValueChange = { name = it },
                label = "NAMA KEBIASAAN",
                placeholder = "Contoh: Lari Pagi 5km"
            )

            BrutalistTextField(
                value = cue,
                onValueChange = { cue = it },
                label = "PEMICU (CUE)",
                placeholder = "Saatalarm berbunyi..."
            )

            // NEW: 2 Minute Rule Input
            BrutalistTextField(
                value = twoMinuteRuleText,
                onValueChange = { twoMinuteRuleText = it },
                label = "ATURAN 2 MENIT",
                placeholder = "Apa versi 2 menit dari kebiasaan ini?"
            )

            // Target & Reminder Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BrutalistTextField(
                    value = target,
                    onValueChange = { if (it.all { char -> char.isDigit() }) target = it },
                    label = "TARGET HARIAN",
                    placeholder = "1",
                    modifier = Modifier.weight(1f)
                )
                
                 BrutalistTextField(
                    value = reminder,
                    onValueChange = { reminder = it },
                    label = "PENGINGAT (JAM)",
                    placeholder = "06:00",
                    modifier = Modifier.weight(1f)
                )
            }

            // Identity Dropdown
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "RELASI IDENTITAS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                ExposedDropdownMenuBox(
                    expanded = identityExpanded,
                    onExpandedChange = { identityExpanded = !identityExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedIdentity?.name ?: "Pilih Identitas Terkait",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = identityExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                            .fillMaxWidth()
                            .brutalBorder(cornerRadius = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = brutalTextFieldColors(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = identityExpanded,
                        onDismissRequest = { identityExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tidak ada") },
                            onClick = { 
                                selectedIdentity = null
                                identityExpanded = false 
                            }
                        )
                        uiState.availableIdentities.forEach { identity ->
                            DropdownMenuItem(
                                text = { Text(identity.name) },
                                onClick = { 
                                    selectedIdentity = identity
                                    identityExpanded = false 
                                }
                            )
                        }
                    }
                }
                Text(
                    "Menghubungkan kebiasaan dengan identitas memperkuat motivasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun BrutalistTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
            modifier = Modifier
                .fillMaxWidth()
                .brutalBorder(cornerRadius = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = brutalTextFieldColors(),
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
fun BrutalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
     Button(
        onClick = onClick,
        modifier = modifier.brutalBorder(cornerRadius = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
    }
}
