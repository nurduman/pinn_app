/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.taskdetail

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.util.LoadingContent
import com.example.android.architecture.blueprints.todoapp.util.TaskDetailTopAppBar

@Composable
fun TaskDetailScreen(
    onEditTask: (String) -> Unit,
    onBack: () -> Unit,
    onDeleteTask: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskDetailViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TaskDetailTopAppBar(onBack = onBack, onDelete = viewModel::deleteTask) },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = { onEditTask(viewModel.taskId) }) {
                Icon(Icons.Filled.Edit, stringResource(id = R.string.edit_task))
            }
        }
    ) { paddingValues ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        EditTaskContent(
            loading = uiState.isLoading,
            empty = uiState.task == null && !uiState.isLoading,
            task = uiState.task,
            onRefresh = viewModel::refresh,
            onTaskCheck = viewModel::setCompleted,
            onSendToPinn = viewModel::sendToPinn,
            modifier = Modifier.padding(paddingValues)
        )

        // Check for user messages to display on the screen
        uiState.userMessage?.let { userMessage ->
            val snackbarText = stringResource(userMessage)
            LaunchedEffect(snackbarHostState, viewModel, userMessage, snackbarText) {
                snackbarHostState.showSnackbar(snackbarText)
                viewModel.snackbarMessageShown()
            }
        }

        // Check if the task is deleted and call onDeleteTask
        LaunchedEffect(uiState.isTaskDeleted) {
            if (uiState.isTaskDeleted) {
                onDeleteTask()
            }
        }

        // Handle "Send to PINN" trigger (placeholder for now)
        LaunchedEffect(uiState.sendToPinnTriggered) {
            if (uiState.sendToPinnTriggered) {
                // Simulate loading state for PINN processing
                viewModel.setLoading(true)
                // Future implementation: Call PinnApiClient here
                // For now, reset after a delay (simulating API call)
                // This will be replaced with actual API logic
                // viewModel.setLoading(false) // Uncomment and adjust when adding API
            }
        }
    }
}

@Composable
private fun EditTaskContent(
    loading: Boolean,
    empty: Boolean,
    task: Task?,
    onTaskCheck: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onSendToPinn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenPadding = Modifier.padding(
        horizontal = dimensionResource(id = R.dimen.horizontal_margin),
        vertical = dimensionResource(id = R.dimen.vertical_margin),
    )
    val commonModifier = modifier
        .fillMaxWidth()
        .then(screenPadding)

    LoadingContent(
        loading = loading,
        empty = empty,
        emptyContent = {
            Text(
                text = stringResource(id = R.string.no_data),
                modifier = commonModifier
            )
        },
        onRefresh = onRefresh
    ) {
        Column(commonModifier.verticalScroll(rememberScrollState())) {
            if (task != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(screenPadding),
                ) {
                    Checkbox(task.isCompleted, onTaskCheck)
                    Column {
                        Text(text = task.title, style = MaterialTheme.typography.headlineSmall)
                        Text(text = task.description, style = MaterialTheme.typography.bodySmall)
                        Text(text = "Conductivity: ${task.conductivity}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Radius: ${task.radius}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Depth: ${task.depth}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Geometry File: ${task.geometryFile?.let { Uri.parse(it).lastPathSegment } ?: "Not set"}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Surface Temp File: ${task.surfaceTempFile?.let { Uri.parse(it).lastPathSegment } ?: "Not set"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                // Placeholder "Send to PINN" button
                Button(
                    onClick = onSendToPinn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Training")
                }
                Button(
                    onClick = onSendToPinn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Training+Optimization")
                }
                Button(
                    onClick = onSendToPinn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Optimization")
                }

                // Optimized fields (initially empty, to be updated by API)
                Text(
                    text = "Optimized Radius: N/A",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "Optimized Depth: N/A",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Optimized Conductivity: N/A",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Loading indicator while PINN is running
                if (loading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp))
                        Text(text = "Processing with PINN...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun EditTaskContentPreview() {
    Surface {
        EditTaskContent(
            loading = false,
            empty = false,
            Task(
                title = "Title",
                description = "Description",
                isCompleted = false,
                id = "ID",
                conductivity = 2.0,
                radius = 2.0,
                depth = 2.0,
                geometryFile = "geometry_edit",
                surfaceTempFile = "surface_temp_edit"
            ),
            onTaskCheck = { },
            onRefresh = { },
            onSendToPinn = {}
        )
    }
}

@Preview
@Composable
private fun EditTaskContentTaskCompletedPreview() {
    Surface {
        EditTaskContent(
            loading = false,
            empty = false,
            Task(
                title = "Title",
                description = "Description",
                isCompleted = true,
                id = "ID",
                conductivity = 3.0,
                radius = 2.0,
                depth = 2.0,
                geometryFile = "geometry_edit",
                surfaceTempFile = "surface_temp_edit"
            ),
            onTaskCheck = { },
            onRefresh = { },
            onSendToPinn = {}
        )
    }
}

@Preview
@Composable
private fun EditTaskContentEmptyPreview() {
    Surface {
        EditTaskContent(
            loading = false,
            empty = true,
            Task(
                title = "Title",
                description = "Description",
                isCompleted = false,
                id = "ID",
                conductivity = 0.0,
                radius = 2.0,
                depth = 2.0,
                geometryFile = "geometry",
                surfaceTempFile = "surface"
            ),
            onTaskCheck = { },
            onRefresh = { },
            onSendToPinn = {}
        )
    }
}

@Preview
@Composable
private fun EditTaskContentLoadingPreview() {
    Surface {
        EditTaskContent(
            loading = true,
            empty = false,
            Task(
                title = "Title",
                description = "Description",
                isCompleted = false,
                id = "ID",
                conductivity = 2.0,
                radius = 2.0,
                depth = 2.0,
                geometryFile = "geometry_edit",
                surfaceTempFile = "surface_temp_edit"
            ),
            onTaskCheck = { },
            onRefresh = { },
            onSendToPinn = {}
        )
    }
}