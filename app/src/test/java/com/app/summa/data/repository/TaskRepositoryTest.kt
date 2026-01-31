package com.app.summa.data.repository

import com.app.summa.data.local.TaskDao
import com.app.summa.data.model.Task
import com.app.summa.util.NotificationScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class TaskRepositoryTest {

    private val taskDao: TaskDao = mockk(relaxed = true)
    private val identityRepository: IdentityRepository = mockk(relaxed = true)
    private val notificationScheduler: NotificationScheduler = mockk(relaxed = true)

    private val repository = TaskRepositoryImpl(
        taskDao = taskDao,
        identityRepository = identityRepository,
        notificationScheduler = notificationScheduler
    )

    @Test
    fun `processDailyWrapUp should update tasks in batch`() = runTest {
        // Given
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val tasks = listOf(
            Task(id = 1, title = "Task 1", scheduledDate = yesterday.toString(), scheduledTime = null, isCommitment = false),
            Task(id = 2, title = "Task 2", scheduledDate = yesterday.toString(), scheduledTime = null, isCommitment = false),
            Task(id = 3, title = "Task 3", scheduledDate = yesterday.toString(), scheduledTime = null, isCommitment = false)
        )

        coEvery { taskDao.getAllTasksSync() } returns tasks

        // When
        repository.processDailyWrapUp()

        // Then
        // Expect updateTasksScheduledDate to be called once with all 3 IDs
        coVerify(exactly = 1) {
            taskDao.updateTasksScheduledDate(
                match { ids -> ids.containsAll(listOf(1L, 2L, 3L)) && ids.size == 3 },
                today.toString()
            )
        }

        // Ensure individual updates are NOT called
        coVerify(exactly = 0) { taskDao.updateTask(any()) }
    }

    @Test
    fun `processDailyWrapUp should update commitment tasks in batch and process penalties individually`() = runTest {
        // Given
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val tasks = listOf(
            Task(id = 1, title = "Task 1", scheduledDate = yesterday.toString(), scheduledTime = null, isCommitment = true, relatedIdentityId = 10L),
            Task(id = 2, title = "Task 2", scheduledDate = yesterday.toString(), scheduledTime = null, isCommitment = true, relatedIdentityId = 10L)
        )

        coEvery { taskDao.getAllTasksSync() } returns tasks
        coEvery { identityRepository.addVoteToIdentity(any(), any(), any()) } returns true

        // When
        repository.processDailyWrapUp()

        // Then
        // Expect updateTasksScheduledDate to be called once with all 2 IDs
        coVerify(exactly = 1) {
            taskDao.updateTasksScheduledDate(
                match { ids -> ids.containsAll(listOf(1L, 2L)) && ids.size == 2 },
                today.toString()
            )
        }

        // Expect addVoteToIdentity to be called 2 times (once for each commitment)
        coVerify(exactly = 2) { identityRepository.addVoteToIdentity(eq(10L), any(), any()) }

        // Ensure individual task updates are NOT called
        coVerify(exactly = 0) { taskDao.updateTask(any()) }
    }
}
