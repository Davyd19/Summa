package com.app.summa.ui.screens

import com.app.summa.ui.components.*

import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.*
import com.app.summa.ui.components.BrutalFab
import com.app.summa.ui.components.BrutalIconAction
import com.app.summa.ui.components.BrutalTopAppBar
import com.app.summa.ui.components.brutalBorder
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.MoneyViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    viewModel: MoneyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTransactionSheet by remember { mutableStateOf(false) } // Hanya ini yang utama
    var showAddAccountDialog by remember { mutableStateOf(false) } // Akun jarang dibuat, dialog masih oke, atau bisa diganti sheet juga nanti

    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.showRewardAnimation) {
        if (uiState.showRewardAnimation) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Keuangan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                actions = {
                    BrutalIconAction(
                        icon = Icons.Default.Add,
                        contentDescription = "Tambah Akun",
                        onClick = { showAddAccountDialog = true }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            BrutalFab(
                onClick = { showTransactionSheet = true },
                icon = Icons.Default.Add,
                contentDescription = "Tambah Transaksi"
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.showRewardAnimation) {
                CoinExplosionAnimation(
                    modifier = Modifier.fillMaxSize().zIndex(10f),
                    trigger = true,
                    onFinished = { viewModel.dismissRewardAnimation() }
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 110.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item { BrutalistNetWorthCard(totalNetWorth = uiState.totalNetWorth, modifier = Modifier.padding(horizontal = 16.dp)) }
                    item {
                        Text("Akun Anda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                            items(uiState.accounts) { account -> BrutalistAccountCard(account = account) }
                        }
                    }
                    item {
                        Text("Transaksi Terakhir", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
                        ) {
                            Column {
                                uiState.recentTransactions.forEachIndexed { index, transaction ->
                                    BrutalistTransactionItem(transaction = transaction)
                                    if (index < uiState.recentTransactions.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onAdd = { name, type, balance, isInvestment, color ->
                viewModel.addAccount(name, type, balance, isInvestment, color)
                showAddAccountDialog = false
            }
        )
    }

    // INPUT SHEET TRANSAKSI BARU (Menggantikan Dialog Transaksi dan Transfer)
    if (showTransactionSheet) {
        TransactionInputSheet(
            accounts = uiState.accounts,
            onDismiss = { showTransactionSheet = false },
            onSave = { accountId, type, amount, category, note ->
                viewModel.addTransaction(accountId, type, amount, category, note)
                showTransactionSheet = false
            }
        )
    }
}


@Composable
fun BrutalistNetWorthCard(
    totalNetWorth: Double,
    modifier: Modifier = Modifier
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    BrutalistCard(
        modifier = modifier
            .fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Box {
            // Background decoration
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = 240.dp, y = (-20).dp)
                    .brutalBorder(cornerRadius = 100.dp, strokeWidth = 1.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            "NET WORTH",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            formatter.format(totalNetWorth),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "+5.2% bulan ini",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun BrutalistQuickActions(
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onTransferClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrutalistActionButton(
            icon = Icons.Default.ArrowDownward,
            label = "MASUK",
            color = SuccessGreen,
            onClick = onIncomeClick,
            modifier = Modifier.weight(1f)
        )
        BrutalistActionButton(
            icon = Icons.Default.ArrowUpward,
            label = "KELUAR",
            color = ErrorRed,
            onClick = onExpenseClick,
            modifier = Modifier.weight(1f)
        )
        BrutalistActionButton(
            icon = Icons.Default.SwapHoriz,
            label = "TRANSFER",
            color = MaterialTheme.colorScheme.primary,
            onClick = onTransferClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun BrutalistActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BrutalistCard(
        modifier = modifier.height(72.dp).clickable(onClick = onClick),
        containerColor = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun BrutalistAccountCard(account: Account) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val baseColor = try {
        Color(account.color.removePrefix("#").toLong(16) or 0x00000000FF000000)
    } catch (e: Exception) {
        DeepTeal
    }

    BrutalistCard(
        modifier = Modifier
            .width(260.dp)
            .height(140.dp),
        containerColor = baseColor
    ) {
        Box {
            // Background decoration
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .offset(x = 180.dp, y = (-20).dp)
                    .brutalBorder(cornerRadius =100.dp, strokeWidth=1.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            account.type.name.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    if (account.isInvestment) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GoldAccent.copy(alpha = 0.3f),
                            modifier = Modifier.brutalBorder(strokeWidth = 1.dp, cornerRadius = 4.dp)
                        ) {
                            Text(
                                "💎",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Text(
                    formatter.format(account.balance),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun BrutalistTransactionItem(transaction: Transaction) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val isExpense = transaction.type == TransactionType.EXPENSE
    val color = if (isExpense) ErrorRed else SuccessGreen

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
             Box(
                modifier = Modifier
                    .size(40.dp)
                    .brutalBorder(cornerRadius =100.dp, strokeWidth=2.dp, color=color)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    transaction.note.ifBlank { transaction.category },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                     "${transaction.date} • ${transaction.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
       
        Text(
            formatter.format(transaction.amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun BrutalistRewardAnimation(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        visible = false
        kotlinx.coroutines.delay(300)
        onDismiss()
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = GoldContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "reward")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Text(
                        "💎",
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.scale(scale)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "INVESTMENT UP!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = GoldDark
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Smart move for a brighter future.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Dialogs tetap sama seperti sebelumnya (AddAccountDialog, AddTransactionDialog, TransferDialog)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, type: AccountType, balance: Double, isInvestment: Boolean, color: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.BANK) }
    var isInvestment by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf("#00796B") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AKUN BARU", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("NAMA AKUN") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("SALDO AWAL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isInvestment,
                        onCheckedChange = { isInvestment = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Akun Investasi", fontWeight=FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(name, type, balance.toDoubleOrNull() ?: 0.0, isInvestment, color)
                },
                shape = RoundedCornerShape(4.dp), // Brutalist shape
                modifier = Modifier.brutalBorder()
            ) {
                Text("TAMBAH", fontWeight=FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("BATAL", fontWeight=FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onAdd: (Account, TransactionType, Double, String, String) -> Unit
) {
    // ... (Keep implementation but update styling if needed, essentially same as above)
    // For brevity assuming standard Dialog is fine, but styled buttons are better.
     var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TRANSAKSI", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("MASUK") }
                    )
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("KELUAR") }
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("JUMLAH") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("KATEGORI") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("CATATAN") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedAccount != null) {
                        onAdd(selectedAccount!!, type, amount.toDoubleOrNull() ?: 0.0, category, note)
                    }
                },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.brutalBorder()
            ) {
                Text("SIMPAN", fontWeight=FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("BATAL", fontWeight=FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (Account, Account, Double) -> Unit
) {
    var fromAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var toAccount by remember { mutableStateOf(accounts.lastOrNull()) }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TRANSFER", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DARI: ${fromAccount?.name}", fontWeight=FontWeight.Bold)
                Text("KE: ${toAccount?.name}", fontWeight=FontWeight.Bold)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("JUMLAH") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fromAccount != null && toAccount != null) {
                        onConfirm(fromAccount!!, toAccount!!, amount.toDoubleOrNull() ?: 0.0)
                    }
                },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.brutalBorder()
            ) {
                Text("TRANSFER", fontWeight=FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("BATAL", fontWeight=FontWeight.Bold)
            }
        }
    )
}
