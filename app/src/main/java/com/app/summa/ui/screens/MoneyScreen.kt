package com.app.summa.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

data class AccountCard(
    val id: Long,
    val name: String,
    val type: String,
    val balance: Double,
    val icon: String,
    val gradient: List<Color>,
    val isInvestment: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen() {
    var showTransferDialog by remember { mutableStateOf(false) }
    var showRewardAnimation by remember { mutableStateOf(false) }

    val accounts = remember {
        mutableStateListOf(
            AccountCard(1, "Bank Jago", "Bank", 25000000.0, "🏦",
                listOf(Color(0xFF00D4AA), Color(0xFF00B89C)), false),
            AccountCard(2, "Bank Blu", "Bank", 12000000.0, "💳",
                listOf(Color(0xFF4A90E2), Color(0xFF357ABD)), false),
            AccountCard(3, "Stockbit", "Investment", 860800.0, "📈",
                listOf(Color(0xFFFF6B6B), Color(0xFFEE5A6F)), true),
            AccountCard(4, "Tunai", "Cash", 13400000.0, "💵",
                listOf(Color(0xFF4CAF50), Color(0xFF45A049)), false),
        )
    }

    val totalNetWorth = accounts.sumOf { it.balance }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Keuangan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Kelola aset Anda",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Total Net Worth
            item {
                NetWorthCard(totalNetWorth = totalNetWorth)
            }

            // Account Cards Carousel
            item {
                Text(
                    "Akun Anda",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(accounts) { account ->
                        AccountCardItem(account = account)
                    }
                }
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionButton(
                        icon = Icons.Default.Add,
                        label = "Masuk",
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { /* Income */ }
                    )
                    ActionButton(
                        icon = Icons.Default.Remove,
                        label = "Keluar",
                        color = ErrorRed,
                        modifier = Modifier.weight(1f),
                        onClick = { /* Expense */ }
                    )
                    ActionButton(
                        icon = Icons.Default.SwapHoriz,
                        label = "Transfer",
                        color = DeepTeal,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showTransferDialog = true
                        }
                    )
                }
            }

            // Recent Transactions
            item {
                Text(
                    "Transaksi Terakhir",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(5) { index ->
                TransactionItem(
                    title = if (index % 2 == 0) "Transfer ke Investasi" else "Belanja Bulanan",
                    amount = if (index % 2 == 0) -500000.0 else -250000.0,
                    date = "15 Nov 2025",
                    category = if (index % 2 == 0) "Investment" else "Shopping"
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showTransferDialog) {
        TransferDialog(
            accounts = accounts,
            onDismiss = { showTransferDialog = false },
            onConfirm = { fromAccount, toAccount, amount ->
                showTransferDialog = false
                if (toAccount.isInvestment) {
                    showRewardAnimation = true
                }
            }
        )
    }

    if (showRewardAnimation) {
        InvestmentRewardAnimation(
            onDismiss = { showRewardAnimation = false }
        )
    }
}

@Composable
fun NetWorthCard(totalNetWorth: Double) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(DeepTeal, DeepTealLight)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Total Kekayaan Bersih",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    formatter.format(totalNetWorth),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "+5.2% dari bulan lalu",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountCardItem(account: AccountCard) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Card(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = account.gradient
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            account.type,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        account.icon,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Column {
                    Text(
                        "Saldo",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        formatter.format(account.balance),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        )
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
fun TransactionItem(
    title: String,
    amount: Double,
    date: String,
    category: String
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    SummaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (amount < 0) ErrorRed.copy(alpha = 0.2f)
                        else SuccessGreen.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (amount < 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (amount < 0) ErrorRed else SuccessGreen
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "$date • $category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Text(
                formatter.format(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (amount < 0) ErrorRed else SuccessGreen
            )
        }
    }
}

@Composable
fun TransferDialog(
    accounts: List<AccountCard>,
    onDismiss: () -> Unit,
    onConfirm: (AccountCard, AccountCard, Double) -> Unit
) {
    var fromAccount by remember { mutableStateOf(accounts[0]) }
    var toAccount by remember { mutableStateOf(accounts[2]) }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Dana") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // From Account
                Column {
                    Text("Dari", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        onClick = { /* Show picker */ },
                        shape = MaterialTheme.shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(fromAccount.icon)
                            Spacer(Modifier.width(8.dp))
                            Text(fromAccount.name, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                }

                // To Account
                Column {
                    Text("Ke", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        onClick = { /* Show picker */ },
                        shape = MaterialTheme.shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(toAccount.icon)
                            Spacer(Modifier.width(8.dp))
                            Text(toAccount.name, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Jumlah") },
                    placeholder = { Text("Rp 0") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    onConfirm(fromAccount, toAccount, amountValue)
                }
            ) {
                Text("Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun InvestmentRewardAnimation(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        visible = false
        kotlinx.coroutines.delay(300)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated coin
                    val infiniteTransition = rememberInfiniteTransition()
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Text(
                        "🪙",
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.scale(scale)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Aset Masa Depan Bertambah!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Selamat! Anda telah menambah investasi untuk masa depan yang lebih baik.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}