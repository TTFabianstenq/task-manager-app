package com.taskmanager.app.process

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProcessViewModel(
    private val repository: ProcessRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(ProcessUiState())
    val ui: StateFlow<ProcessUiState> = _ui

    init {
        refresh()
    }

    fun setQuery(query: String) {
        _ui.update { it.copy(query = query) }
    }

    fun toggleSystem() {
        _ui.update { it.copy(showSystem = !it.showSystem) }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.Default) {
            _ui.update { it.copy(isLoading = true, message = null) }
            val memory = repository.deviceMemory()
            val hasAccess = repository.hasUsageAccess()
            val apps = repository.listApps()
            _ui.update {
                it.copy(
                    memory = memory,
                    apps = apps,
                    hasUsageAccess = hasAccess,
                    isLoading = false
                )
            }
        }
    }

    fun endProcess(app: AppProcess) {
        viewModelScope.launch(Dispatchers.Default) {
            val result = repository.endProcess(app.packageName)
            val apps = repository.listApps()
            val memory = repository.deviceMemory()
            _ui.update {
                it.copy(
                    message = result,
                    apps = apps,
                    memory = memory
                )
            }
        }
    }

    fun clearMessage() {
        _ui.update { it.copy(message = null) }
    }
}

class ProcessViewModelFactory(
    private val repository: ProcessRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProcessViewModel::class.java)) {
            return ProcessViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
