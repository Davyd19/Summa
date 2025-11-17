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
                title = { Text("Tinjauan Harian", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    Button(onClick = { onBack() }) {
                        Text("Selesai")
                    }
                }
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
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                // --- LANGKAH 1: RINGKASAN OTOMATIS ---
                item {
                    Text(
                        "Langkah 1: Ringkasan Hari Ini",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    SummaCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Tugas Selesai:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            uiState.summary?.completedTasks?.forEach { task ->
                                Text("✅ ${task.title}")
                            } ?: Text("Tidak ada tugas selesai hari ini.")

                            Spacer(Modifier.height(16.dp))

                            Text(
                                "Kebiasaan Selesai:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            uiState.summary?.completedHabits?.forEach { habit ->
                                Text("✅ ${habit.name}")
                            } ?: Text("Tidak ada kebiasaan selesai hari ini.")
                        }
                    }
                }

                // --- LANGKAH 2: PEMBERIAN SUARA IDENTITAS ---
                item {
                    Text(
                        "Langkah 2: Beri Suara Identitas Anda",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.reflectionText,
                        onValueChange = { viewModel.saveReflection(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 150.dp),
                        label = { Text("Apa yang Anda pelajari/syukuri hari ini?") }
                    )
                }
            }
        }
    }
}

@Composable
fun IdentityVoteCard(
    identity: Identity,
    onVote: (String) -> Unit
) {
    var showNoteField by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    SummaCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(identity.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(progress = identity.progress / 100f ) // Asumsi progres 0-100
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = { showNoteField = !showNoteField },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Beri Suara")
                    Text("Beri Suara")
                }
            }

            // Fitur Jurnal Mikro
            if (showNoteField) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Catatan singkat untuk ${identity.name} (Opsional)") },
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