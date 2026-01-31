package com.app.summa.ui.viewmodel

import com.app.summa.data.model.FocusSession
import com.app.summa.data.model.Habit
import com.app.summa.data.model.HabitLog
import com.app.summa.data.repository.FocusRepository
import com.app.summa.data.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModelTest {

    private lateinit var viewModel: FocusViewModel
    private val focusRepository: FocusRepository = mockk(relaxed = true)
    private val habitRepository: HabitRepository = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Default mocks to avoid initialization errors
        every { habitRepository.getAllHabits() } returns flowOf(emptyList())
        every { habitRepository.getLogsForDate(any()) } returns flowOf(emptyList())

        viewModel = FocusViewModel(focusRepository, habitRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `completeSession updates to target count if below`() = runTest {
        // Arrange
        val habitId = 1L
        val targetCount = 5
        val currentCount = 2
        val habit = Habit(id = habitId, name = "Test Habit", targetCount = targetCount)
        val habitLog = HabitLog(habitId = habitId, date = LocalDate.now().toString(), count = currentCount)

        every { habitRepository.getAllHabits() } returns flowOf(listOf(habit))
        every { habitRepository.getLogsForDate(any()) } returns flowOf(listOf(habitLog))

        // We need to re-init viewModel or update state to select the habit
        viewModel.selectHabit(habitId)

        // Act
        viewModel.completeSession()
        advanceUntilIdle()

        // Assert
        val slotCount = slot<Int>()
        coVerify { habitRepository.updateHabitCount(eq(habit), capture(slotCount), any()) }

        // Expect target count (5) because 2 < 5
        assertEquals(targetCount, slotCount.captured)
    }

    @Test
    fun `completeSession increments count if at or above target`() = runTest {
        // Arrange
        val habitId = 1L
        val targetCount = 5
        val currentCount = 5
        val habit = Habit(id = habitId, name = "Test Habit", targetCount = targetCount)
        val habitLog = HabitLog(habitId = habitId, date = LocalDate.now().toString(), count = currentCount)

        every { habitRepository.getAllHabits() } returns flowOf(listOf(habit))
        every { habitRepository.getLogsForDate(any()) } returns flowOf(listOf(habitLog))

        viewModel.selectHabit(habitId)

        // Act
        viewModel.completeSession()
        advanceUntilIdle()

        // Assert
        val slotCount = slot<Int>()
        coVerify { habitRepository.updateHabitCount(eq(habit), capture(slotCount), any()) }

        // Expect current + 1 (6) because 5 >= 5
        assertEquals(currentCount + 1, slotCount.captured)
    }
}
