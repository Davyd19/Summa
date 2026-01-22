package com.app.summa.ui.screens

import com.app.summa.ui.components.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.Identity
import com.app.summa.ui.components.CoinExplosionAnimation
import com.app.summa.ui.components.BrutalTopAppBar
import com.app.summa.ui.components.brutalBorder
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.DailySummary
import com.app.summa.ui.viewmodel.ReflectionViewModel
import com.app.summa.ui.viewmodel.VoteSuggestion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReflectionScreen(
    viewModel: ReflectionViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            BrutalTopAppBar(
                title = "",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBack,
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (pagerState.currentPage == index) 10.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(20.dp)) {
                if (pagerState.currentPage < 3) {
                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .brutalBorder(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Lanjut", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.completeReflection()
                            onBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .brutalBorder(),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Selesai & Simpan", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Check, null)
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    userScrollEnabled = true
                ) { page ->
                    when (page) {
                        0 -> RitualStepOne(uiState.summary)
                        1 -> RitualStepTwo(uiState.suggestions) { i, p, n -> viewModel.addVote(i, p, n) }
                        2 -> RitualStepThree(uiState.identities) { i, p, n -> viewModel.addVote(i, p, n) }
                        3 -> RitualStepFour(uiState.reflectionText) { viewModel.updateReflectionText(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun BrutalistStatBox(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    BrutalistCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BrutalistWinCard(text: String, icon: String, type: String) {
    BrutalistCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Update usages in RitualStepOne
@Composable
fun RitualStepOne(summary: DailySummary?) {
    if (summary == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Data aktivitas hari ini belum tersedia.", color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
        }
        return
    }

    val score = summary.dailyScore
    val grade = summary.dailyGrade
    val isGreatDay = score >= 80

    if (isGreatDay) {
        CoinExplosionAnimation(trigger = true) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))
        BrutalistHeader(title = "TINJAUAN HARI", subtitle = "Seberapa konsisten Anda hari ini?")

        Spacer(Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(colors = listOf(if (isGreatDay) GoldAccent.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant, Color.Transparent))
                    )
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .brutalBorder(radius = 100.dp, strokeWidth = 4.dp, color = if (isGreatDay) GoldAccent else MaterialTheme.colorScheme.outline)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = grade, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = if (isGreatDay) GoldDark else MaterialTheme.colorScheme.onSurface)
                    Text(text = "$score/100", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BrutalistStatBox(label = "KOMITMEN", value = "${summary.completedCommitments}/${summary.totalCommitments}", icon = Icons.Default.CheckCircle, color = DeepTeal, modifier = Modifier.weight(1f))
            BrutalistStatBox(label = "KEBIASAAN", value = "${summary.completedHabits.size}", icon = Icons.Default.EmojiEvents, color = StreakOrange, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        Text("KEMENANGAN KECIL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(12.dp))

        if (summary.completedTasks.isEmpty() && summary.completedHabits.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                Text("Belum ada aktivitas tercatat hari ini.", fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                summary.completedHabits.forEach { habit -> BrutalistWinCard(text = habit.name, icon = habit.icon, type = "KEBIASAAN") }
                summary.completedTasks.forEach { task -> BrutalistWinCard(text = task.title, icon = if (task.isCommitment) "🔥" else "✅", type = if (task.isCommitment) "KOMITMEN" else "TUGAS") }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

// Fix SuggestionVoteCard -> BrutalistVoteCard
@Composable
fun BrutalistVoteCard(suggestion: VoteSuggestion, onVote: (Identity, Int, String) -> Unit) {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(400)
            onVote(suggestion.identity, suggestion.points, suggestion.reason)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
    ) {
        BrutalistCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("VOTE UNTUK:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    Text(suggestion.identity.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(suggestion.reason, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                }
                Button(
                    onClick = { isVisible = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.brutalBorder(radius=4.dp)
                ) {
                    Text("+${suggestion.points} XP", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Update RitualStepTwo to use BrutalistVoteCard
@Composable
fun RitualStepTwo(suggestions: List<VoteSuggestion>, onVote: (Identity, Int, String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🗳️", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        BrutalistHeader(title = "BUKTI IDENTITAS", subtitle = "Aktivitas Anda membuktikan siapa Anda.")

        Spacer(Modifier.height(32.dp))

        if (suggestions.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Tidak ada saran otomatis.\nLanjut ke manual vote.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = suggestions, key = { "${it.identity.id}_${it.reason.hashCode()}" }) { suggestion ->
                    BrutalistVoteCard(suggestion, onVote)
                }
            }
        }
    }
}

// Update CompactIdentityVoteCard to use BrutalistCard
@Composable
fun CompactIdentityVoteCard(identity: Identity, onVote: (Identity, Int, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    val level = identity.progress / 100

    BrutalistCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(identity.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Lvl $level", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Icon(if (expanded) Icons.Default.Close else Icons.Default.ThumbUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("Alasan (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Button(
                        onClick = {
                            onVote(identity, 10, note)
                            expanded = false
                            note = ""
                        },
                        modifier = Modifier.fillMaxWidth().brutalBorder(radius=4.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("BERI SUARA (+10 XP)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
