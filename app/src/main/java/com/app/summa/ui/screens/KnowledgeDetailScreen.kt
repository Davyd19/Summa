package com.app.summa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.ui.viewmodel.KnowledgeViewModel
import kotlinx.coroutines.launch

// === KNOWLEDGE DETAIL SCREEN ===

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
}