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

    private val repository = TaskRepositoryImpl(taskDao, identityRepository, notificationScheduler)

    @Test
    fun verifyDailyWrapUpUsesOptimizedQuery() = runTest {
        // Setup
        val tasks = listOf<Task>()
        coEvery { taskDao.getActiveTasksSync() } returns tasks

        // Act
        repository.processDailyWrapUp()

        // Assert
        // Verify that we use the optimized query (WHERE isCompleted = 0)
        // instead of fetching all tasks and filtering in memory.
        coVerify(exactly = 1) { taskDao.getActiveTasksSync() }
        coVerify(exactly = 0) { taskDao.getAllTasksSync() }
    }
}
