package com.app.summa.ui.viewmodel

import com.app.summa.data.repository.FocusRepository
import com.app.summa.data.repository.HabitRepository
import com.app.summa.util.TimeProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModelTest {

    private lateinit var viewModel: FocusViewModel
    private val focusRepository: FocusRepository = mockk()
    private val habitRepository: HabitRepository = mockk()
    private val timeProvider: FakeTimeProvider = FakeTimeProvider()

    private val testDispatcher = StandardTestDispatcher()

    class FakeTimeProvider : TimeProvider {
        var currentTime: Long = 100000L
        override fun currentTimeMillis(): Long = currentTime
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { habitRepository.getAllHabits() } returns flowOf(emptyList())
        every { habitRepository.getLogsForDate(any()) } returns flowOf(emptyList())

        viewModel = FocusViewModel(focusRepository, habitRepository, timeProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `timer decreases correctly`() = runTest {
        // Initialize session with 60 seconds (1 minute)
        viewModel.initializeSession(initialTarget = 1, isClipMode = false)

        // Start timer
        viewModel.startTimer()
        runCurrent()

        // Initial state
        assertEquals(60, viewModel.uiState.value.timeRemaining)
        assertTrue(viewModel.uiState.value.isRunning)

        // Simulate 1 second passing
        timeProvider.currentTime += 1000
        advanceTimeBy(1000)

        // After 1 second, time remaining should be 59
        assertEquals(59, viewModel.uiState.value.timeRemaining)
    }
}
