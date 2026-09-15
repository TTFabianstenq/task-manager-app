package com.taskmanager.app.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.taskmanager.app.process.AppProcess
import com.taskmanager.app.process.ProcessFilter
import com.taskmanager.app.process.ProcessViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessListScreen(viewModel: ProcessViewModel) {
    val state by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingEnd by remember { mutableStateOf<AppProcess?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val now = System.currentTimeMillis()
    val filtered = state.apps.filter { app ->
        val q = state.query.trim()
        val matchesQuery = q.isEmpty() ||
            app.label.contains(q, ignoreCase = true) ||
            app.packageName.contains(q, ignoreCase = true)
        val matchesSystem = state.showSystem || !app.isSystem
        val matchesFilter = when (state.filter) {
            ProcessFilter.ALL -> true
            ProcessFilter.RECENT -> app.pid != null || (app.lastUsedMs > 0 && now - app.lastUsedMs < 15 * 60 * 1000)
        }
        matchesQuery && matchesSystem && matchesFilter
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Task Manager") },
                    actions = {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                state.memory?.let { mem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("RAM", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${mem.usedMb} MB used / ${mem.totalMb} MB",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            @Suppress("DEPRECATION")
                            LinearProgressIndicator(
                                progress = (mem.usedPercent / 100f).coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "${mem.availMb} MB free · ${mem.usedPercent}% used",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (!state.hasUsageAccess) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Usage access needed",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Android hides other apps unless you grant Usage access.",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Open settings")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.filter == ProcessFilter.ALL,
                        onClick = { viewModel.setFilter(ProcessFilter.ALL) },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = state.filter == ProcessFilter.RECENT,
                        onClick = { viewModel.setFilter(ProcessFilter.RECENT) },
                        label = { Text("Active") }
                    )
                    FilterChip(
                        selected = state.showSystem,
                        onClick = { viewModel.toggleSystem() },
                        label = { Text("System") }
                    )
                    Text("${filtered.size}", style = MaterialTheme.typography.bodySmall)
                }

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        ProcessRow(
                            app = app,
                            onEnd = { pendingEnd = app }
                        )
                    }
                }
            }
        }

        Text(
            text = "made by fabianstenq",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .alpha(0.45f),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    pendingEnd?.let { app ->
        AlertDialog(
            onDismissRequest = { pendingEnd = null },
            title = { Text("End process?") },
            text = {
                Text(
                    "${app.label} (${app.packageName})\n\nThis only stops background processes. Android will not force-stop a foreground app without root."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.endProcess(app)
                        pendingEnd = null
                    }
                ) { Text("End") }
            },
            dismissButton = {
                TextButton(onClick = { pendingEnd = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProcessRow(
    app: AppProcess,
    onEnd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(app.label, fontWeight = FontWeight.SemiBold)
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val extra = buildString {
                    append(app.importance)
                    if (app.pid != null) append(" · PID ${app.pid}")
                    if (app.memoryMb != null) append(" · ${app.memoryMb} MB")
                    if (app.lastUsedMs > 0) {
                        append(" · ")
                        append(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(app.lastUsedMs)))
                    }
                    if (app.isSystem) append(" · system")
                }
                Text(
                    extra,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedButton(
                onClick = onEnd,
                enabled = app.canEnd
            ) {
                Text("End")
            }
        }
    }
}
