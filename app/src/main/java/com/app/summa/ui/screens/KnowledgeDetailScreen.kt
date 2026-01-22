package com.app.summa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items // PENTING: Import ini sering terlupakan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // Mengimpor getValue, setValue, collectAsState, dll.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.ui.components.BrutalIconAction
import com.app.summa.ui.components.BrutalTopAppBar
import com.app.summa.ui.components.brutalBorder
import com.app.summa.ui.components.brutalTextFieldColors
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
    // Menggunakan by collectAsState() memerlukan import androidx.compose.runtime.getValue
    val uiState by viewModel.uiState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    // State Autocomplete
    val linkSuggestions by viewModel.linkSuggestions.collectAsState()

    var title by remember { mutableStateOf("") }
    // Gunakan TextFieldValue untuk kontrol kursor saat insert [[ ]]
    var contentState by remember { mutableStateOf(TextFieldValue("")) }
    var showLinkDialog by remember { mutableStateOf(false) }

    // Logic untuk posisi Autocomplete
    var autocompleteQuery by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Init data saat load
    LaunchedEffect(uiState.selectedNote) {
        uiState.selectedNote?.let {
            if (title.isEmpty()) title = it.title
            if (contentState.text.isEmpty()) contentState = TextFieldValue(it.content)
        }
    }

    // Auto-save saat keluar
    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                if (contentState.text.isNotBlank()) {
                    viewModel.saveNote(title, contentState.text)
                }
            }
        }
    }

    // --- LOGIKA DETEKSI AUTCOMPLETE ---
    fun checkAutocomplete(value: TextFieldValue) {
        val text = value.text
        val cursor = value.selection.start

        val lastOpenBracket = text.lastIndexOf("[[", cursor - 1)

        if (lastOpenBracket != -1) {
            val textSegment = text.substring(lastOpenBracket + 2, cursor)
            if (!textSegment.contains("]]") && !textSegment.contains("\n")) {
                autocompleteQuery = textSegment
                viewModel.searchForAutocomplete(textSegment)
                return
            }
        }

        autocompleteQuery = null
        viewModel.clearLinkSuggestions()
    }

    fun insertLink(selectedNoteTitle: String) {
        val currentText = contentState.text
        val cursor = contentState.selection.start
        val lastOpenBracket = currentText.lastIndexOf("[[", cursor - 1)

        if (lastOpenBracket != -1) {
            val prefix = currentText.substring(0, lastOpenBracket + 2)
            val suffix = currentText.substring(cursor)

            val newText = "$prefix$selectedNoteTitle]]$suffix"
            val newCursor = prefix.length + selectedNoteTitle.length + 2

            contentState = TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )

            autocompleteQuery = null
            viewModel.clearLinkSuggestions()
        }
    }

    Scaffold(
        topBar = {
        topBar = {
            BrutalTopAppBar(
                title = "",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBack,
                actions = {
                    BrutalIconAction(
                        icon = Icons.Default.DataArray,
                        contentDescription = "Insert Link"
                    ) {
                        val currentText = contentState.text
                        val selection = contentState.selection
                        val newText = StringBuilder(currentText)
                            .insert(selection.max, "]]")
                            .insert(selection.min, "[[")
                            .toString()
                        val newCursorPos = selection.min + 2
                        contentState = TextFieldValue(text = newText, selection = TextRange(newCursorPos))
                    }
                    BrutalIconAction(
                        icon = Icons.Default.Link,
                        contentDescription = "Cari Link"
                    ) { showLinkDialog = true }

                    BrutalTextButton(
                        text = "SIMPAN",
                        onClick = { viewModel.saveNote(title, contentState.text); onBack() },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            )
        }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.brutalBorder(strokeWidth = 3.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BrutalIconAction(
                        onClick = { onConvertToTask(uiState.selectedNote?.title ?: title, uiState.selectedNote?.content ?: contentState.text) },
                        icon = Icons.Default.TaskAlt,
                        contentDescription = "Jadikan Tugas"
                    )
                    val isPermanent = uiState.selectedNote?.isPermanent == true
                    BrutalIconAction(
                        onClick = { if (!isPermanent) { viewModel.convertToPermanent(); onBack() } },
                        icon = if (isPermanent) Icons.Default.Inventory else Icons.Default.Archive,
                        contentDescription = "Arsipkan"
                    )
                    BrutalIconAction(
                        onClick = { viewModel.deleteNote(); onBack() },
                        icon = Icons.Default.Delete,
                        contentDescription = "Hapus"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Judul (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    colors = brutalTextFieldColors().copy(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    )
                )

                // Backlinks & Forward Links Display
                // if (uiState.forwardLinks.isNotEmpty()) -> Menggunakan import kotlin.collections untuk List
                if (uiState.forwardLinks.isNotEmpty()) {
                    Text("Terhubung ke:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        items(uiState.forwardLinks) { link ->
                            NoteLinkChip(note = link, isBacklink = false, onRemove = { viewModel.removeLink(link) })
                        }
                    }
                }
                if (uiState.backlinks.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Dihubungkan oleh (Backlinks):", style = MaterialTheme.typography.labelSmall, color = PurpleAccent, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        items(uiState.backlinks) { link ->
                            NoteLinkChip(note = link, isBacklink = true, onRemove = null)
                        }
                    }
                }

                Divider(Modifier.padding(vertical = 8.dp).alpha(0.5f))

                // Editor Utama
                OutlinedTextField(
                    value = contentState,
                    onValueChange = {
                        contentState = it
                        checkAutocomplete(it)
                    },
                    placeholder = { Text("Mulai menulis... Ketik [[ untuk menghubungkan.") },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    )
                )
            }

            // Overlay Autocomplete
            if (autocompleteQuery != null && linkSuggestions.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .align(Alignment.BottomCenter)
                        .shadow(0.dp, RoundedCornerShape(8.dp))
                        .brutalBorder(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = null
                ) {
                    LazyColumn {
                        item {
                            Text(
                                "Saran Tautan:",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider()
                        }
                        items(linkSuggestions) { note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { insertLink(note.title) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    note.title.ifBlank { "Tanpa Judul" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }

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
                        onValueChange = { searchQuery = it; viewModel.searchNotesForLinking(it) },
                        placeholder = { Text("Cari catatan lain...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (searchResults.isEmpty() && searchQuery.isNotBlank()) item {
                            Text("Tidak ditemukan", style = MaterialTheme.typography.bodySmall)
                        }
                        items(searchResults) { note ->
                            BrutalistCard(
                                onClick = { viewModel.addLink(note); showLinkDialog = false },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.addLink(note); showLinkDialog = false }
                            ) {
                                Column(Modifier.padding(12.dp).fillMaxWidth()) {
                                    Text(note.title.ifBlank { "Tanpa Judul" }, fontWeight = FontWeight.Bold)
                                    Text(note.content.take(50).replace("\n", " "), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLinkDialog = false }) { Text("Tutup") } }
        )
    }
}

@Composable
fun NoteLinkChip(note: KnowledgeNote, isBacklink: Boolean, onRemove: (() -> Unit)?) {
    InputChip(
        selected = true,
        onClick = { },
        label = { Text(note.title.ifBlank { "Tanpa Judul" }) },
        leadingIcon = {
            // Menggunakan Icons.AutoMirrored.Filled untuk support RTL
            Icon(
                if (isBacklink) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                null,
                Modifier.size(12.dp)
            )
        },
        trailingIcon = if (onRemove != null) {
            { Icon(Icons.Default.Close, null, Modifier.size(16.dp).clickable { onRemove() }) }
        } else null,
        colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = if (isBacklink) PurpleAccent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = if (isBacklink) PurpleAccent else MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = if (isBacklink) PurpleAccent else MaterialTheme.colorScheme.primary
        ),
        border = null
    )
}