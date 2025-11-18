package com.app.summa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.Identity
import com.app.summa.ui.components.SummaCard
import com.app.summa.ui.theme.GoldAccent
import com.app.summa.ui.viewmodel.ReflectionUiState
import com.app.summa.ui.viewmodel.ReflectionViewModel
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflectionScreen(
    viewModel: ReflectionViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tinjauan Harian",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // PERBAIKAN: Tombol "Selesai" dibuat lebih menonjol
                    Button(onClick = { onBack() }, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Selesai")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- LANGKAH 1: RINGKASAN OTOMATIS ---
                item {
                    Text(
                        "Langkah 1: Ringkasan Hari Ini",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                item {
                    // PERBAIKAN: Gunakan SummaCard baru
                    SummaCard(padding = 20.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Bagian Tugas
                            Column {
                                Text(
                                    "Tugas Selesai:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(8.dp))
                                if (uiState.summary?.completedTasks?.isEmpty() != false) {
                                    Text("Tidak ada tugas selesai hari ini.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                uiState.summary?.completedTasks?.forEach { task ->
                                    Text("✅ ${task.title}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Bagian Kebiasaan
                            Column {
                                Text(
                                    "Kebiasaan Selesai:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(8.dp))
                                if (uiState.summary?.completedHabits?.isEmpty() != false) {
                                    Text("Tidak ada kebiasaan selesai hari ini.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                uiState.summary?.completedHabits?.forEach { habit ->
                                    Text("✅ ${habit.name}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                // --- LANGKAH 2: PEMBERIAN SUARA IDENTITAS ---
                item {
                    Text(
                        "Langkah 2: Beri Suara Identitas",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                items(uiState.identities) { identity ->
                    IdentityVoteCard(
                        identity = identity,
                        onVote = { note ->
                            // Beri 10 poin untuk setiap "suara"
                            viewModel.addVote(identity, 10, note)
                        }
                    )
                }

                // --- LANGKAH 3: JURNAL UMUM ---
                item {
                    Text(
                        "Langkah 3: Jurnal Umum",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                item {
                    // PERBAIKAN: TextField dibuat lebih modern
                    OutlinedTextField(
                        value = uiState.reflectionText,
                        onValueChange = { viewModel.saveReflection(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 150.dp),
                        label = { Text("Apa yang Anda pelajari/syukuri hari ini?") },
                        shape = MaterialTheme.shapes.large,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

// PERBAIKAN: Desain ulang IdentityVoteCard
@Composable
fun IdentityVoteCard(
    identity: Identity,
    onVote: (String) -> Unit
) {
    var showNoteField by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    // Asumsi progres adalah 0-1000 untuk lebih granular
    val progress = (identity.progress % 1000) / 1000f
    val level = (identity.progress / 1000) + 1

    SummaCard(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(identity.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Level $level",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(16.dp))
                // PERBAIKAN: Tombol "Vote" menggunakan warna Aksen
                Button(
                    onClick = { showNoteField = !showNoteField },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Beri Suara", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Beri Suara")
                }
            }

            // PERBAIKAN: Progress bar lebih tebal dan informatif
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer
            )

            // Fitur Jurnal Mikro
            if (showNoteField) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Catatan singkat untuk ${identity.name} (Opsional)") },
                    shape = MaterialTheme.shapes.large,
                    trailingIcon = {
                        IconButton(onClick = {
                            onVote(note)
                            note = ""
                            showNoteField = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Simpan Suara")
                        }
                    }
                )
            }
        }
    }
}