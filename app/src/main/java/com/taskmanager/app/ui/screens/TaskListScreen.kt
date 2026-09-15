package com.taskmanager.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taskmanager.app.data.Priority
import com.taskmanager.app.data.Task
import com.taskmanager.app.ui.TaskFilter
import com.taskmanager.app.ui.TaskViewModel
import com.taskmanager.app.ui.components.AddEditTaskDialog
import com.taskmanager.app.ui.components.TaskItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(viewModel: TaskViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showDeleteCompletedDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Manager") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showDeleteCompletedDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear completed")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search tasks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filter == TaskFilter.ALL,
                    onClick = { viewModel.setFilter(TaskFilter.ALL) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = uiState.filter == TaskFilter.ACTIVE,
                    onClick = { viewModel.setFilter(TaskFilter.ACTIVE) },
                    label = { Text("Active") }
                )
                FilterChip(
                    selected = uiState.filter == TaskFilter.COMPLETED,
                    onClick = { viewModel.setFilter(TaskFilter.COMPLETED) },
                    label = { Text("Completed") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.tasks.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            uiState.searchQuery.isNotBlank() -> "No tasks match your search"
                            uiState.filter == TaskFilter.COMPLETED -> "No completed tasks yet"
                            uiState.filter == TaskFilter.ACTIVE -> "No active tasks. Add one!"
                            else -> "No tasks yet.\nTap + to create your first task."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.tasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onToggleComplete = { viewModel.toggleCompleted(task) },
                            onEdit = { taskToEdit = task },
                            onDelete = {
                                viewModel.deleteTask(task)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Task deleted")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditTaskDialog(
            task = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, description, priority, dueDate ->
                viewModel.addTask(title, description, priority, dueDate)
                showAddDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Task added")
                }
            }
        )
    }

    taskToEdit?.let { task ->
        AddEditTaskDialog(
            task = task,
            onDismiss = { taskToEdit = null },
            onConfirm = { title, description, priority, dueDate ->
                viewModel.updateTask(
                    task.copy(
                        title = title,
                        description = description,
                        priority = priority,
                        dueDate = dueDate
                    )
                )
                taskToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("Task updated")
                }
            }
        )
    }

    if (showDeleteCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCompletedDialog = false },
            title = { Text("Clear completed?") },
            text = { Text("This will permanently delete all completed tasks.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCompleted()
                    showDeleteCompletedDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Completed tasks cleared")
                    }
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCompletedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
