package com.taskmanager.app.process

enum class ProcessFilter { ALL, RECENT }

data class DeviceMemory(
    val totalMb: Long,
    val availMb: Long,
    val usedMb: Long,
    val usedPercent: Int
)

data class AppProcess(
    val packageName: String,
    val label: String,
    val pid: Int?,
    val lastUsedMs: Long,
    val importance: String,
    val isSystem: Boolean,
    val canEnd: Boolean,
    val memoryMb: Int? = null
)

data class ProcessUiState(
    val memory: DeviceMemory? = null,
    val apps: List<AppProcess> = emptyList(),
    val hasUsageAccess: Boolean = false,
    val query: String = "",
    val showSystem: Boolean = false,
    val filter: ProcessFilter = ProcessFilter.ALL,
    val message: String? = null,
    val isLoading: Boolean = true
)
