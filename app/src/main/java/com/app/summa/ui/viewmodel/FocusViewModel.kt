package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class FocusUiState(
    val timeRemaining: Int = 0,
    val isRunning: Boolean = false,
    val paperclipsMoved: Int = 0,
    val paperclipsLeft: Int = 10,
    val startTime: Long = 0L,
    val sessionStartTimeStamp: Long = 0L,
    val targetClips: Int = 10,
    val selectedHabitId: Long? = null, // New: Linked Habit
    val availableHabits: List<com.app.summa.data.model.HabitItem> = emptyList(), // New: Dropdown data
    val isClipMode: Boolean = false // New: Toggle for optional clips
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val focusRepository: com.app.summa.data.repository.FocusRepository,
    private val habitRepository: com.app.summa.data.repository.HabitRepository,
    private val timeProvider: com.app.summa.util.TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadHabits()
    }

    private fun loadHabits() {
        viewModelScope.launch {
            // Simplified loading for dropdown
            val habits = habitRepository.getAllHabits().first()
            val logs = habitRepository.getLogsForDate(java.time.LocalDate.now()).first()
            
            val habitItems = habits.map { habit ->
                val todayLog = logs.find { it.habitId == habit.id }
                com.app.summa.data.model.HabitItem(
                    id = habit.id,
                    name = habit.name,
                    icon = habit.icon,
                    currentCount = todayLog?.count ?: 0,
                    targetCount = habit.targetCount,
                    totalSum = 0, // Not needed
                    currentStreak = 0, // Not needed
                    perfectStreak = 0, // Not needed
                    originalModel = habit
                )
            }
            _uiState.update { it.copy(availableHabits = habitItems) }
        }
    }

    fun initializeSession(initialTarget: Int = 25, isClipMode: Boolean = false) {
        if (_uiState.value.startTime == 0L) {
            _uiState.update {
                it.copy(
                    targetClips = initialTarget, // reused as minutes if !isClipMode
                    paperclipsLeft = if (isClipMode) initialTarget else 0,
                    timeRemaining = if (isClipMode) 0 else initialTarget * 60, // Minutes to Seconds
                    startTime = 0L,
                    isClipMode = isClipMode
                )
            }
        }
    }
    
    fun selectHabit(habitId: Long?) {
         _uiState.update { it.copy(selectedHabitId = habitId) }
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return

        // Set start time stamp logic
        val currentTime = timeProvider.currentTimeMillis()
        
        // Timer Logic
        val currentRemaining = _uiState.value.timeRemaining
        
        // Logic timestamp untuk menghitung durasi yang berlalu
        // sessionStartTimeStamp acts as "Resume Time"
        val sessionStart = currentTime

        _uiState.update {
            it.copy(
                isRunning = true,
                sessionStartTimeStamp = sessionStart,
                startTime = if (it.startTime == 0L) currentTime else it.startTime
            )
        }

        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                // Update UI setiap 200ms agar responsif, tapi hitungan tetap akurat
                delay(200)

                val now = timeProvider.currentTimeMillis()
                
                if (_uiState.value.isClipMode) {
                     // In Clip Mode, we count UP
                     val elapsed = (now - _uiState.value.startTime) / 1000
                     _uiState.update { it.copy(timeRemaining = elapsed.toInt()) }
                } else {
                     // In Timer Mode, we count DOWN
                     // Calculate elapsed time since this session segment started (resumed)
                     val elapsedSinceResume = (now - sessionStart) / 1000
                     val newRemaining = currentRemaining - elapsedSinceResume.toInt()

                     if (newRemaining <= 0) {
                          _uiState.update { it.copy(timeRemaining = 0, isRunning = false) }
                          break
                     } else {
                          // Update remaining time, but DO NOT update sessionStartTimeStamp
                          _uiState.update { it.copy(timeRemaining = newRemaining) }
                     }
                }
            }
        }
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isRunning = false) }
        timerJob?.cancel()
    }

    fun movePaperclip() {
        if (!_uiState.value.isClipMode) return // Disable if not in clip mode
        
        _uiState.update {
            val newLeft = (it.paperclipsLeft - 1).coerceAtLeast(0)
            it.copy(
                paperclipsLeft = newLeft,
                paperclipsMoved = it.paperclipsMoved + 1
            )
        }

        val currentState = _uiState.value

        // Logika Baru: Auto-Start saat klip pertama dipindah
        if (!currentState.isRunning && currentState.paperclipsMoved > 0) {
            startTimer()
        }

        // Logika Baru: Auto-Stop saat target selesai
        if (currentState.paperclipsLeft == 0) {
            // pauseTimer() // Don't auto pause, allow completion manually
        }
    }
    
    fun completeSession() {
        viewModelScope.launch {
            val state = _uiState.value
            pauseTimer()
            
            // 1. Save Session
            val session = com.app.summa.data.model.FocusSession(
                habitId = state.selectedHabitId ?: 0L, // 0 if generic
                startTime = state.startTime,
                endTime = timeProvider.currentTimeMillis(),
                paperclipsCollected = if(state.isClipMode) state.paperclipsMoved else 0,
                createdAt = timeProvider.currentTimeMillis()
            )
            focusRepository.saveSession(session)
            
            // 2. Update Habit if selected
            if (state.selectedHabitId != null) {
                // Fetch current habit to get target
                 val habit = habitRepository.getAllHabits().first().find { it.id == state.selectedHabitId }
                 if (habit != null) {
                     // Increment logic (simple +1 for now, or based on clips?)
                     // Request says: "habit itu dalam hari itu langsung terhitung sudah di kerjakan"
                     // So we might want to set it to target count? Or just +1?
                     // "Langsung terhitung sudah dikerjakan" implies Completion.
                     // Let's set it to Target if not reached? Or just increment.
                     // Safest: Increment by 1 session equivalent.
                     // But user said "Finish focus mode -> Habit done". 
                     // Let's set count = targetCount to be sure it marks as done.
                     
    // Fix save logic
    /*
                     val currentLog = habitRepository.getLogsForDate(java.time.LocalDate.now()).first().find { it.habitId == habit.id }
                     val current = currentLog?.count ?: 0
                     if (current < habit.targetCount) {
                         // Update logic
                         // We need to call a repository method that accepts ID + Count, or update the Habit object?
                         // Assuming repository has updateHabitCount(habit: Habit, newCount: Int)
                         // But 'habit' here is HabitItem? No, 'habit' from find() is Habit entity.
                         habitRepository.updateHabitCount(habit, habit.targetCount)
                     }
    */
                // Simplified safe logic:
                 val habitEntity = habitRepository.getAllHabits().first().find { it.id == state.selectedHabitId }
                 if (habitEntity != null) {
                      habitRepository.updateHabitCount(habitEntity, habitEntity.targetCount)
                 }
                 }
            }
        }
    }

    fun reset() {
        timerJob?.cancel()
        _uiState.value = FocusUiState()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}