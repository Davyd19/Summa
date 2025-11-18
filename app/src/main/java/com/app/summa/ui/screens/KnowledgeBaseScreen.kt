package com.app.summa.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.ui.components.SummaCard
import com.app.summa.ui.viewmodel.KnowledgeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KnowledgeBaseScreen(
    viewModel: KnowledgeViewModel = hiltViewModel(),
    onNoteClick: (Long) -> Unit,
    onAddNoteClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabTitles = listOf("Inbox", "Pustaka")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pustaka",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNoteClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Catatan")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // PERBAIKAN: Menggunakan PrimaryTabRow untuk style modern
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title, fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> NoteList(
                        notes = uiState.inboxNotes,
                        onNoteClick = onNoteClick,
                        emptyText = "Inbox kosong. Tekan '+' untuk mencatat ide."
                    )
                    1 -> NoteList(
                        notes = uiState.permanentNotes,
                        onNoteClick = onNoteClick,
                        emptyText = "Pustaka kosong. Arsipkan catatan dari Inbox."
                    )
                }
            }
        }
    }
}

@Composable
fun NoteList(
    notes: List<KnowledgeNote>,
    onNoteClick: (Long) -> Unit,
    emptyText: String
) {
    if (notes.isEmpty()) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), contentAlignment = Alignment.Center) {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        // PERBAIKAN: Menggunakan Column ber-border, bukan Card per item
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp) // Beri jarak 1dp untuk divider
        ) {
            item {
                // Kelompokkan dalam satu list visual
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.shapes.large
                        )
                ) {
                    notes.forEachIndexed { index, note ->
                        NoteListItem(
                            note = note,
                            onClick = { onNoteClick(note.id) }
                        )
                        if (index < notes.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

// PERBAIKAN: Mengganti NoteItem dengan ListItem
@Composable
fun NoteListItem(
    note: KnowledgeNote,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = if (note.title.isNotBlank()) note.title else "Tanpa Judul",
                fontWeight = if (note.title.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                style = if (note.title.isNotBlank()) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                // Beri warna abu-abu jika tanpa judul
                color = if (note.title.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        supportingContent = {
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 3, // Tampilkan lebih banyak
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}