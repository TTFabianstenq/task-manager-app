package com.taskmanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskmanager.app.data.TaskDatabase
import com.taskmanager.app.data.TaskRepository
import com.taskmanager.app.ui.TaskViewModel
import com.taskmanager.app.ui.TaskViewModelFactory
import com.taskmanager.app.ui.screens.TaskListScreen
import com.taskmanager.app.ui.theme.TaskManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = TaskDatabase.getInstance(applicationContext)
        val repository = TaskRepository(database.taskDao())
        val factory = TaskViewModelFactory(repository)

        setContent {
            TaskManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: TaskViewModel = viewModel(factory = factory)
                    TaskListScreen(viewModel = viewModel)
                }
            }
        }
    }
}
