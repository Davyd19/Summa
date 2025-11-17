package com.app.summa.data.repository

import com.app.summa.data.local.TaskDao
import com.app.summa.data.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

interface TaskRepository {
    fun getActiveTasks(): Flow<List<Task>>
    fun getTasksByDate(date: LocalDate): Flow<List<Task>>
    suspend fun insertTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun completeTask(taskId: Long)
}

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getActiveTasks(): Flow<List<Task>> {
        return taskDao.getActiveTasks()
    }

    override fun getTasksByDate(date: LocalDate): Flow<List<Task>> {
        return taskDao.getTasksByDate(date.toString())
    }

    override suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    override suspend fun completeTask(taskId: Long) {
        // TODO: Implement task completion logic
    }
}