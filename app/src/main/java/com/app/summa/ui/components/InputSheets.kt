package com.app.summa.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.app.summa.data.model.Account
import com.app.summa.data.model.Identity
import com.app.summa.data.model.TransactionType
import com.app.summa.ui.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

// --- 1. HABIT INPUT SHEET (VALIDASI & LAYOUT) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitInputSheet(
    initialName: String = "",
    initialIcon: String = "🎯",
    initialTarget: Int = 1,
    initialIdentityId: Long? = null,
    initialCue: String = "",
    initialReminders: String = "",

    identities: List<Identity>,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, target: Int, identityId: Long?, cue: String, reminder: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(initialIcon) }
    var targetStr by remember { mutableStateOf(initialTarget.toString()) }
    var selectedIdentity by remember {
        mutableStateOf(identities.find { it.id == initialIdentityId })
    }
    var cue by remember { mutableStateOf(initialCue) }

    val reminders = remember {
        mutableStateListOf<String>().apply {
            if (initialReminders.isNotBlank()) {
                addAll(initialReminders.split(","))
            } else {
                add("")
            }
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var editingReminderIndex by remember { mutableStateOf(-1) }
    val timePickerState = rememberTimePickerState(is24Hour = true)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val isEditMode = initialName.isNotBlank()

    // Validasi Sederhana
    val isValid = name.isNotBlank() && (targetStr.toIntOrNull() ?: 0) > 0

    LaunchedEffect(targetStr) {
        val targetCount = targetStr.toIntOrNull() ?: 1
        if (targetCount > reminders.size) {
            repeat(targetCount - reminders.size) { reminders.add("") }
        }
    }

    val scrollState = rememberScrollState()
    val emojis = listOf("🎯", "💪", "📚", "🧘", "💧", "🏃", "🎨", "✍️", "🍎", "😴", "🎸", "💻", "🧹", "🙏", "💊")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding() // PERBAIKAN: Menangani Keyboard agar tidak menutupi input
                .verticalScroll(scrollState)
                .padding(bottom = 48.dp)
        ) {
            Text(
                if(isEditMode) "Edit Kebiasaan" else "Buat Kebiasaan Baru",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))

            // SECTION 1: IDENTITAS
            InputSectionTitle("Identitas", Icons.Default.Person)
            Text(
                "Kebiasaan ini membangun identitas siapa?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedIdentity == null,
                        onClick = { selectedIdentity = null },
                        label = { Text("Umum") }
                    )
                }
                items(identities) { identity ->
                    FilterChip(
                        selected = selectedIdentity == identity,
                        onClick = { selectedIdentity = identity },
                        label = { Text(identity.name) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldAccent)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // SECTION 2: DETAIL
            InputSectionTitle("Detail Kebiasaan", Icons.Default.Edit)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Saya akan... *") }, // Tanda wajib
                placeholder = { Text("Contoh: Membaca 10 halaman") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                isError = name.isBlank() // Visual Error
            )
            Spacer(Modifier.height(12.dp))
            Text("Pilih Ikon:", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp).selectableGroup()) {
                items(emojis) { emoji ->
                    val isSelected = icon == emoji
                    val backgroundColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, label = "bgColor")
                    val scale by animateFloatAsState(if (isSelected) 1.1f else 1f, label = "scale")

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(backgroundColor)
                            .selectable(
                                selected = isSelected,
                                onClick = { icon = emoji },
                                role = Role.RadioButton
                            )
                    ) {
                        Text(emoji, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // SECTION 3: ATOMIC RULE
            InputSectionTitle("Pemicu (Cue)", Icons.Default.Lightbulb)
            OutlinedTextField(
                value = cue,
                onValueChange = { cue = it },
                label = { Text("Setelah saya...") },
                placeholder = { Text("Contoh: Menutup laptop kerja") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
            )

            Spacer(Modifier.height(24.dp))

            // SECTION 4: TARGET & JADWAL
            InputSectionTitle("Target & Jadwal", Icons.Default.TrackChanges)

            OutlinedTextField(
                value = targetStr,
                onValueChange = { if (it.all { c -> c.isDigit() }) targetStr = it },
                label = { Text("Target Harian (Kali)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            if (reminders.isNotEmpty()) {
                Text(
                    "Jam Pengingat (${reminders.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reminders.forEachIndexed { index, time ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = time,
                                onValueChange = { },
                                placeholder = { Text("00:00") },
                                label = { Text("Sesi ${index + 1}") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                readOnly = true,
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Alarm,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        editingReminderIndex = index
                                        showTimePicker = true
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isValid) {
                        val combinedReminders = reminders.filter { it.isNotBlank() }.joinToString(",")
                        onSave(name, icon, targetStr.toIntOrNull() ?: 1, selectedIdentity?.id, cue, combinedReminders)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = isValid // PERBAIKAN: Tombol mati jika tidak valid
            ) {
                Text(if(isEditMode) "Simpan Perubahan" else "Mulai Kebiasaan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    if (editingReminderIndex in reminders.indices) {
                        reminders[editingReminderIndex] = formatter.format(cal.time)
                    }
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Batal") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

// ... TaskInputSheet & TransactionInputSheet (Bisa diterapkan pola validasi serupa) ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskInputSheet(
    identities: List<Identity>,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, time: String, isCommitment: Boolean, identityId: Long?, twoMin: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var time by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var isCommitment by remember { mutableStateOf(true) }
    var selectedIdentity by remember { mutableStateOf<Identity?>(null) }
    var twoMinAction by remember { mutableStateOf("") }

    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = LocalTime.now().hour,
        initialMinute = LocalTime.now().minute,
        is24Hour = true
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 48.dp)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Apa tugas utama Anda?", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                textStyle = MaterialTheme.typography.headlineSmall,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { showTimePicker = true },
                    label = { Text(time) },
                    leadingIcon = { Icon(Icons.Default.Schedule, null, Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = null
                )

                Spacer(Modifier.width(8.dp))

                FilterChip(
                    selected = isCommitment,
                    onClick = { isCommitment = !isCommitment },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isCommitment) "🔥 Komitmen" else "🌱 Aspirasi")
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepTeal,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = null
                )
            }

            Spacer(Modifier.height(24.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Hubungkan ke Identitas:", style = MaterialTheme.typography.titleSmall)
            }

            LazyRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedIdentity == null,
                        onClick = { selectedIdentity = null },
                        label = { Text("Umum") }
                    )
                }
                items(identities) { identity ->
                    FilterChip(
                        selected = selectedIdentity == identity,
                        onClick = { selectedIdentity = if (selectedIdentity == identity) null else identity },
                        label = { Text(identity.name) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldAccent)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Catatan / Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Aturan 2 Menit", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = twoMinAction,
                        onValueChange = { twoMinAction = it },
                        placeholder = { Text("Langkah pertama yang bisa selesai < 2 menit...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { if(title.isNotBlank()) onSave(title, description, time, isCommitment, selectedIdentity?.id, twoMinAction) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = title.isNotBlank() // PERBAIKAN: Validasi Judul
            ) {
                Text("Simpan Tugas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    time = formatter.format(cal.time)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Batal") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

// ... TransactionInputSheet (Sama, tambahkan enabled pada Button) ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionInputSheet(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (accountId: Long, type: TransactionType, amount: Double, category: String, note: String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val categories = if (type == TransactionType.EXPENSE)
        listOf("Makan", "Transport", "Belanja", "Tagihan", "Hiburan", "Lainnya")
    else listOf("Gaji", "Bonus", "Investasi", "Hadiah", "Jual Barang")

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(4.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TransactionType.values().filter { it != TransactionType.TRANSFER }.forEach { t ->
                    val isSelected = type == t
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected)
                            if (t == TransactionType.INCOME) SuccessGreen else ErrorRed
                        else Color.Transparent,
                        label = "bgColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(backgroundColor)
                            .selectable(
                                selected = isSelected,
                                onClick = { type = t },
                                role = Role.RadioButton
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if(t == TransactionType.INCOME) "Pemasukan" else "Pengeluaran",
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Jumlah", style = MaterialTheme.typography.labelMedium)
            TextField(
                value = amountStr,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountStr = it },
                textStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                prefix = { Text("Rp ", style = MaterialTheme.typography.headlineMedium) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            Divider()
            Spacer(Modifier.height(24.dp))

            InputSectionTitle("Akun", Icons.Default.AccountBalanceWallet)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts) { account ->
                    FilterChip(
                        selected = selectedAccount == account,
                        onClick = { selectedAccount = account },
                        label = { Text(account.name) },
                        leadingIcon = { Text(account.icon) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            InputSectionTitle("Kategori", Icons.Default.Category)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (amountStr.isNotBlank() && selectedAccount != null) {
                        onSave(selectedAccount!!.id, type, amountStr.toDoubleOrNull() ?: 0.0, category.ifBlank { "Umum" }, note)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == TransactionType.INCOME) SuccessGreen else ErrorRed
                ),
                enabled = amountStr.isNotBlank() && selectedAccount != null // PERBAIKAN: Validasi
            ) {
                Text("Simpan Transaksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InputSectionTitle(text: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}