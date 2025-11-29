package com.app.summa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels // Tambahkan ini
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState // Tambahkan ini
import androidx.compose.runtime.getValue // Tambahkan ini
import androidx.compose.ui.Modifier
import com.app.summa.navigation.SummaApp
import com.app.summa.ui.theme.SummaTheme
import com.app.summa.ui.viewmodel.MainViewModel // Import VM
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Ambil ViewModel di level Activity agar bisa mengontrol Tema global
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Observe state mode dari ViewModel
            val mainUiState by mainViewModel.uiState.collectAsState()

            // Pass 'currentMode' ke SummaTheme
            SummaTheme(appMode = mainUiState.currentMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Kita tidak perlu pass VM lagi ke dalam karena SummaApp akan mengambil instance yang sama (Singleton/ActivityScoped)
                    // atau membuat baru jika Scoped berbeda, tapi untuk konsistensi navigasi biarkan Hilt mengurusnya di dalam SummaApp.
                    // Namun, agar sinkron, SummaApp di dalam navigasi akan menggunakan instance yang disuntikkan Hilt.
                    SummaApp()
                }
            }
        }
    }
}