package com.app.summa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.ui.theme.PurpleAccent
import com.app.summa.ui.viewmodel.KnowledgeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeDetailScreen(
    viewModel: KnowledgeViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onConvertToTask: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var showLinkDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.selectedNote) {
        uiState.selectedNote?.let {
            title = it.title
            content = it.content
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                if (content.isNotBlank() &&
                    (title != uiState.selectedNote?.title || content != uiState.selectedNote?.content)
                ) {
                    viewModel.saveNote(title, content)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // Tombol Link Zettelkasten
                    IconButton(onClick = { showLinkDialog = true }) {
                        Icon(Icons.Default.Link, contentDescription = "Hubungkan Catatan", tint = PurpleAccent)
                    }
                    TextButton(
                        onClick = {
                            viewModel.saveNote(title, content)
                            onBack()
                        },
                        enabled = content.isNotBlank()
                    ) {
                        Text("Simpan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        onConvertToTask(
                            uiState.selectedNote?.title ?: title,
                            uiState.selectedNote?.content ?: content
                        )
                    }) {
                        Icon(Icons.Default.TaskAlt, contentDescription = "Jadikan Tugas")
                    }

                    val isPermanent = uiState.selectedNote?.isPermanent == true
                    IconButton(
                        onClick = {
                            if (!isPermanent) {
                                viewModel.convertToPermanent()
                                onBack()
                            }
                        },
                        enabled = !isPermanent
                    ) {
                        Icon(
                            if (isPermanent) Icons.Default.Inventory else Icons.Default.Archive,
                            contentDescription = "Arsipkan"
                        )
                    }

                    IconButton(onClick = {
                        viewModel.deleteNote()
                        onBack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Judul (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )

            // Visualisasi Link (Connections)
            if (uiState.linkedNotes.isNotEmpty()) {
                Text(
                    "Terhubung dengan:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(uiState.linkedNotes) { linkedNote ->
                        InputChip(
                            selected = true,
                            onClick = { /* Future: Navigate to this note */ },
                            label = { Text(linkedNote.title.ifBlank { "Tanpa Judul" }) },
                            leadingIcon = { Icon(Icons.Default.Link, null, Modifier.size(16.dp)) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    Modifier.size(16.dp).clickable { viewModel.removeLink(linkedNote) }
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = PurpleAccent.copy(alpha = 0.1f),
                                selectedLabelColor = PurpleAccent,
                                selectedLeadingIconColor = PurpleAccent
                            ),
                            border = null
                        )
                    }
                }
            }

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Tulis ide Anda...") },
                modifier = Modifier.fillMaxSize(),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )
        }
    }

    // Dialog Pencarian untuk Linking
    if (showLinkDialog) {
        var searchQuery by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Hubungkan Catatan") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.searchNotesForLinking(it)
                        },
                        placeholder = { Text("Cari catatan lain...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                            item { Text("Tidak ditemukan", style = MaterialTheme.typography.bodySmall) }
                        }
                        items(searchResults) { note ->
                            Card(
                                onClick = {
                                    viewModel.addLink(note)
                                    showLinkDialog = false
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(Modifier.padding(12.dp).fillMaxWidth()) {
                                    Text(note.title.ifBlank { "Tanpa Judul" }, fontWeight = FontWeight.Bold)
                                    Text(
                                        note.content.take(50).replace("\n", " "),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}