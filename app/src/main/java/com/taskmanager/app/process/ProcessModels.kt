package com.taskmanager.app.process

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
    val canEnd: Boolean
)

data class ProcessUiState(
    val memory: DeviceMemory? = null,
    val apps: List<AppProcess> = emptyList(),
    val hasUsageAccess: Boolean = false,
    val query: String = "",
    val showSystem: Boolean = false,
    val message: String? = null,
    val isLoading: Boolean = true
)
