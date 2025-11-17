package com.app.summa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Update TextField saat selectedNote dari ViewModel dimuat
    LaunchedEffect(uiState.selectedNote) {
        uiState.selectedNote?.let {
            title = it.title
            content = it.content
        }
    }

    // Simpan otomatis saat keluar
    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                // Simpan hanya jika ada perubahan dan konten tidak kosong
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
                title = { }, // Judul kosong, fokus pada konten
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // Tombol Simpan (manual, jika diperlukan)
                    TextButton(
                        onClick = {
                            viewModel.saveNote(title, content)
                            onBack()
                        },
                        enabled = content.isNotBlank()
                    ) {
                        Text("Simpan & Keluar")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    // Tombol Konversi ke Tugas
                    IconButton(onClick = {
                        onConvertToTask(
                            uiState.selectedNote?.title ?: title,
                            uiState.selectedNote?.content ?: content
                        )
                    }) {
                        Icon(Icons.Default.TaskAlt, contentDescription = "Konversi ke Tugas")
                    }

                    Spacer(Modifier.weight(1f))

                    // Tombol Arsipkan/Permanenkan
                    val isPermanent = uiState.selectedNote?.isPermanent == true
                    IconButton(
                        onClick = {
                            if (!isPermanent) {
                                viewModel.convertToPermanent()
                                onBack() // Keluar setelah diarsipkan
                            }
                        },
                        enabled = !isPermanent // Nonaktif jika sudah permanen
                    ) {
                        Icon(
                            if (isPermanent) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (isPermanent) "Di Pustaka" else "Arsipkan ke Pustaka"
                        )
                    }

                    // Tombol Hapus
                    IconButton(onClick = {
                        viewModel.deleteNote()
                        onBack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus")
                    }
                }
            )
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
                label = { Text("Judul (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Tulis ide Anda...") },
                modifier = Modifier.fillMaxSize(),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )
        }
    }
}