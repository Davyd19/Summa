package com.app.summa.ui.screens

import com.app.summa.ui.components.*

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.repository.BackupRepository
import com.app.summa.ui.components.BrutalTopAppBar
import com.app.summa.ui.components.brutalBorder
import com.app.summa.ui.theme.DeepTeal
import com.app.summa.ui.theme.ErrorRed
import com.app.summa.ui.theme.SuccessGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

// --- VIEWMODEL ---
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository
) : ViewModel() {

    // Menyimpan backup ke file (Create Document)
    fun createBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = backupRepository.createBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                Toast.makeText(context, "Backup berhasil disimpan!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Gagal membuat backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Restore backup dari file (Open Document)
    fun restoreBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }

                val jsonString = stringBuilder.toString()
                val success = backupRepository.restoreBackupFromJson(jsonString)

                if (success) {
                    Toast.makeText(context, "Data berhasil dipulihkan! Silakan restart aplikasi.", Toast.LENGTH_LONG).show()
                    // Opsional: Trigger restart intent
                } else {
                    Toast.makeText(context, "File backup rusak atau tidak valid.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Gagal memulihkan: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

// --- UI SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Launcher untuk MENYIMPAN file (Create)
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.createBackup(context, it) }
    }

    // Launcher untuk MEMBUKA file (Open)
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restoreBackup(context, it) }
    }

    // Logic Cek Izin Alarm (Khusus Android 12+)
    fun checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Izin Alarm sudah aktif", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Tidak diperlukan di versi Android ini", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            BrutalTopAppBar(
                title = "Pengaturan",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
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
            // BAGIAN 1: DATA
            item {
                SettingsSectionTitle("Data & Penyimpanan")
            }
            item {
                SettingsCard(
                    title = "Backup Data",
                    subtitle = "Simpan seluruh data aplikasi ke file JSON",
                    icon = Icons.Default.CloudUpload,
                    color = DeepTeal,
                    onClick = {
                        val fileName = "summa_backup_${System.currentTimeMillis()}.json"
                        createBackupLauncher.launch(fileName)
                    }
                )
            }
            item {
                SettingsCard(
                    title = "Restore Data",
                    subtitle = "Kembalikan data dari file backup JSON",
                    icon = Icons.Default.CloudDownload,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        restoreBackupLauncher.launch(arrayOf("application/json"))
                    }
                )
            }
            item {
                BrutalistCard(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Hati-hati: Restore akan menghapus data saat ini dan menggantinya dengan data dari file backup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // BAGIAN 2: IZIN & SISTEM
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionTitle("Sistem & Izin")
            }
            item {
                SettingsCard(
                    title = "Izin Notifikasi & Alarm",
                    subtitle = "Pastikan pengingat berjalan tepat waktu",
                    icon = Icons.Default.NotificationsActive,
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { checkAlarmPermission() }
                )
            }

            // BAGIAN 3: TENTANG
            item {
                Spacer(Modifier.height(8.dp))
                SettingsSectionTitle("Tentang")
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Summa", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Versi 1.0.0 (Beta)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Life Operating System", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
    )
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    BrutalistCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp).brutalBorder(radius=4.dp, strokeWidth=1.dp, color=color)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}