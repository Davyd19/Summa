package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.Habit as HabitModel
import com.app.summa.data.model.HabitLog as HabitLogModel
import com.app.summa.data.repository.HabitRepository
// PENAMBAHAN: Import HabitItem dari layar UI
import com.app.summa.ui.model.HabitItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HabitUiState(
    // PERBAIKAN: Menggunakan HabitItem (model UI)
    val habits: List<HabitItem> = emptyList(),
    // PERBAIKAN: Menggunakan HabitItem (model UI)
    val selectedHabit: HabitItem? = null,
    val habitLogs: List<HabitLogModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitUiState())
    val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()

    // Mengambil log untuk hari ini
    private val todayLogs = habitRepository.getLogsForDate(LocalDate.now())

    init {
        loadHabits()
    }

    private fun loadHabits() {
        viewModelScope.launch {
            // GABUNGKAN (Combine) data habit dengan data log hari ini
            habitRepository.getAllHabits().combine(todayLogs) { habits, logs ->
                // Ubah data database (HabitModel) menjadi data UI (HabitItem)
                habits.map { habit ->
                    val todayLog = logs.find { it.habitId == habit.id }
                    HabitItem(
                        id = habit.id,
                        name = habit.name,
                        icon = habit.icon,
                        // Ambil count dari log hari ini, atau 0 jika tidak ada
                        currentCount = todayLog?.count ?: 0,
                        targetCount = habit.targetCount,
                        totalSum = habit.totalSum,
                        currentStreak = habit.currentStreak,
                        perfectStreak = habit.perfectStreak,
                        // Simpan model asli untuk referensi
                        originalModel = habit
                    )
                }
            }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
                .collect { mappedHabits ->
                    _uiState.value = _uiState.value.copy(
                        habits = mappedHabits,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun selectHabit(habitItem: HabitItem) {
        viewModelScope.launch {
            habitRepository.getHabitLogs(habitItem.id)
                .collect { logs ->
                    _uiState.value = _uiState.value.copy(
                        selectedHabit = habitItem,
                        habitLogs = logs
                    )
                }
        }
    }

    // IMPLEMENTASI: Fungsi untuk menambah hitungan
    fun incrementHabit(habitItem: HabitItem) {
        viewModelScope.launch {
            val newCount = habitItem.currentCount + 1
            // Panggil repository dengan model database asli
            habitRepository.updateHabitCount(
                habit = habitItem.originalModel,
                newCount = newCount
            )
        }
    }

    // IMPLEMENTASI: Fungsi untuk mengurangi hitungan
    fun decrementHabit(habitItem: HabitItem) {
        if (habitItem.currentCount > 0) {
            viewModelScope.launch {
                val newCount = habitItem.currentCount - 1
                // Panggil repository dengan model database asli
                habitRepository.updateHabitCount(
                    habit = habitItem.originalModel,
                    newCount = newCount
                )
            }
        }
    }

    fun addHabit(name: String, icon: String, targetCount: Int) {
        viewModelScope.launch {
            val newHabit = HabitModel(
                name = name,
                icon = icon,
                targetCount = targetCount,
                createdAt = System.currentTimeMillis() // Set waktu pembuatan
            )
            habitRepository.insertHabit(newHabit)
        }
    }

    fun updateHabit(habitItem: HabitItem) {
        viewModelScope.launch {
            // PERBAIKAN: Pastikan Anda mengupdate originalModel
            habitRepository.updateHabit(habitItem.originalModel)
        }
    }

    fun deleteHabit(habitItem: HabitItem) {
        viewModelScope.launch {
            // PERBAIKAN: Pastikan Anda menghapus originalModel
            habitRepository.deleteHabit(habitItem.originalModel)
        }
    }

    // Fungsi untuk menutup layar detail
    fun onBackFromDetail() {
        _uiState.value = _uiState.value.copy(selectedHabit = null)
    }
}