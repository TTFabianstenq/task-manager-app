package com.taskmanager.app.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()
    fun getActiveTasks(): Flow<List<Task>> = taskDao.getActiveTasks()
    fun getCompletedTasks(): Flow<List<Task>> = taskDao.getCompletedTasks()

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    suspend fun insert(task: Task): Long = taskDao.insert(task)

    suspend fun update(task: Task) = taskDao.update(task)

    suspend fun delete(task: Task) = taskDao.delete(task)

    suspend fun deleteCompleted() = taskDao.deleteCompleted()
}
