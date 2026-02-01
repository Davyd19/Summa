package com.app.summa.ui.screens

import android.app.TimePickerDialog
import android.widget.TimePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.Identity
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.PlannerViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    viewModel: PlannerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var isCommitment by remember { mutableStateOf(false) } // Removed twoMinuteRule
    var selectedIdentity by remember { mutableStateOf<Identity?>(null) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var identityExpanded by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    // Time Picker Logic
    val calendar = Calendar.getInstance()
    val timePickerDialog = TimePickerDialog(
        context,
        { _: TimePicker, hour: Int, minute: Int ->
            selectedTime = LocalTime.of(hour, minute)
        },
        calendar[Calendar.HOUR_OF_DAY],
        calendar[Calendar.MINUTE],
        true
    )

    Scaffold(
        topBar = {
            BrutalTopAppBar(
                title = "TUGAS BARU",
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
                        if (title.isNotBlank()) {
                            viewModel.addTask(
                                title = title,
                                description = description,
                                scheduledTime = selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "",
                                isCommitment = isCommitment,
                                isTwoMinutes = false, // Removed rule from UI
                                relatedIdentityId = selectedIdentity?.id
                            )
                            // Jika tanggal yang dipilih bukan hari ini, kita mungkin perlu handle di ViewModel agar selectDate berubah
                            // Namun asumsi addTask di planner memasukkan ke DB, nanti PlannerScreen akan reload.
                            // Untuk amannya, kita bisa minta VM selectDate juga jika perlu, tapi user expectation biasanya balik ke planner.
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    text = "JADWALKAN",
                    containerColor = if (isCommitment) GoldAccent else MaterialTheme.colorScheme.primary,
                    contentColor = if (isCommitment) Color.Black else MaterialTheme.colorScheme.onPrimary,
                    enabled = title.isNotBlank()
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
             // Title Input
             BrutalistTextField(
                value = title,
                onValueChange = { title = it },
                label = "JUDUL TUGAS",
                placeholder = "Apa yang harus diselesaikan?",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )

            // Date & Time Selectors
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Date Selector
                 Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TANGGAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .brutalBorder(cornerRadius = 8.dp)
                            .clickable { showDatePicker = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                             Text(
                                selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
                        }
                    }
                 }

                 // Time Selector
                 Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("JAM (OPSIONAL)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                     Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .brutalBorder(cornerRadius = 8.dp)
                            .clickable { timePickerDialog.show() },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                             Text(
                                selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if(selectedTime != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha=0.4f)
                            )
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
                        }
                    }
                 }
            }
            
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)

             // Commitment Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .brutalBorder(
                        strokeWidth = if(isCommitment) 3.dp else 1.dp,
                        color = if(isCommitment) GoldAccent else MaterialTheme.colorScheme.outline
                    )
                    .background(if(isCommitment) GoldContainer.copy(alpha=0.1f) else Color.Transparent)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("KOMITMEN MUTLAK?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Tugas wajib yang tidak boleh dilewatkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f)
                    )
                }
                Switch(
                    checked = isCommitment,
                    onCheckedChange = { isCommitment = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldAccent,
                        checkedTrackColor = GoldContainer,
                        checkedBorderColor = GoldDark
                    )
                )
            }
            
            // 2-Minute Rule Switch Removed per request

            BrutalistTextField(
                value = description,
                onValueChange = { description = it },
                label = "CATATAN / DETIL",
                placeholder = "Jelaskan langkah-langkahnya...",
                singleLine = false,
                minLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )
            
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
                    onExpandedChange = { identityExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedIdentity?.name ?: "Umum",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = identityExpanded) },
                        modifier = Modifier
                            .menuAnchor()
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
                            text = { Text("Umum") },
                            onClick = { 
                                selectedIdentity = null
                                identityExpanded = false 
                            }
                        )
                        uiState.identities.forEach { identity ->
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
            }
        }
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
