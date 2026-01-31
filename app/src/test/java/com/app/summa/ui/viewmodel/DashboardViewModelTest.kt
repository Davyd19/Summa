package com.app.summa.ui.viewmodel

import com.app.summa.data.model.*
import com.app.summa.data.repository.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var viewModel: DashboardViewModel
    private val taskRepository: TaskRepository = mockk()
    private val habitRepository: HabitRepository = mockk()
    private val identityRepository: IdentityRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val focusRepository: FocusRepository = mockk()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock default returns
        every { habitRepository.getLogsForDate(any()) } returns flowOf(emptyList())
        every { habitRepository.getAllHabits() } returns flowOf(emptyList())
        every { taskRepository.getActiveTasks() } returns flowOf(emptyList())
        every { identityRepository.getAllIdentities() } returns flowOf(emptyList())
        every { accountRepository.getTotalNetWorth() } returns flowOf(0.0)
        every { focusRepository.getTotalPaperclips() } returns flowOf(0)
        every { identityRepository.levelUpEvents } returns MutableSharedFlow()
        every { accountRepository.getAllAccounts() } returns flowOf(emptyList())

        // Mock checkMorningBriefing
        coEvery { taskRepository.processDailyWrapUp() } returns null

        viewModel = DashboardViewModel(
            taskRepository,
            habitRepository,
            identityRepository,
            accountRepository,
            focusRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState emits initial state correctly`() = runTest {
        // Collect one value
        val state = viewModel.uiState.value

        assertNotNull(state)
        assertEquals("Normal", state.currentMode)
    }

    @Test
    fun `uiState updates when repository emits data`() = runTest {
        // Prepare data
        val habit = Habit(id = 1, name = "Test Habit", targetCount = 1)
        val habitLog = HabitLog(habitId = 1, date = LocalDate.now().toString(), count = 1)
        val identity = Identity(id = 1, name = "Test Identity", progress = 100)

        every { habitRepository.getAllHabits() } returns flowOf(listOf(habit))
        every { habitRepository.getLogsForDate(any()) } returns flowOf(listOf(habitLog))
        every { identityRepository.getAllIdentities() } returns flowOf(listOf(identity))

        // Re-init viewmodel to pick up new mocks
        viewModel = DashboardViewModel(
            taskRepository,
            habitRepository,
            identityRepository,
            accountRepository,
            focusRepository
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle() // Wait for flows to combine

        val state = viewModel.uiState.value

        // Check if data is mapped correctly
        assertEquals(1, state.todayHabits.size)
        assertEquals(1, state.todayHabits[0].currentCount)
        assertEquals(100, state.summaPoints)
    }
}