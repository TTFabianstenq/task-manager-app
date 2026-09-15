package com.taskmanager.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskmanager.app.data.Priority
import com.taskmanager.app.data.Task
import com.taskmanager.app.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskFilter {
    ALL, ACTIVE, COMPLETED
}

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<TaskUiState> = combine(
        repository.getAllTasks(),
        _filter,
        _searchQuery
    ) { allTasks, filter, query ->
        val filtered = when (filter) {
            TaskFilter.ALL -> allTasks
            TaskFilter.ACTIVE -> allTasks.filter { !it.isCompleted }
            TaskFilter.COMPLETED -> allTasks.filter { it.isCompleted }
        }
        val searched = if (query.isBlank()) {
            filtered
        } else {
            filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
            }
        }
        TaskUiState(
            tasks = searched,
            filter = filter,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskUiState()
    )

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addTask(
        title: String,
        description: String = "",
        priority: Priority = Priority.MEDIUM,
        dueDate: Long? = null
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insert(
                Task(
                    title = title.trim(),
                    description = description.trim(),
                    priority = priority,
                    dueDate = dueDate
                )
            )
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.update(task)
        }
    }

    fun toggleCompleted(task: Task) {
        viewModelScope.launch {
            repository.update(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }

    fun deleteCompleted() {
        viewModelScope.launch {
            repository.deleteCompleted()
        }
    }
}

class TaskViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
