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
import com.taskmanager.app.process.ProcessRepository
import com.taskmanager.app.process.ProcessViewModel
import com.taskmanager.app.process.ProcessViewModelFactory
import com.taskmanager.app.ui.screens.ProcessListScreen
import com.taskmanager.app.ui.theme.TaskManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = ProcessRepository(applicationContext)
        val factory = ProcessViewModelFactory(repository)

        setContent {
            TaskManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ProcessViewModel = viewModel(factory = factory)
                    ProcessListScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }
}
