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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.android.architecture.blueprints.todoapp.addedittask

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.util.AddEditTaskTopAppBar

@Composable
fun AddEditTaskScreen(
    @StringRes topBarTitle: Int,
    onTaskUpdate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditTaskViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AddEditTaskTopAppBar(topBarTitle, onBack) },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = viewModel::saveTask) {
                Icon(Icons.Filled.Done, stringResource(id = R.string.cd_save_task))
            }
        }
    ) { paddingValues ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        AddEditTaskContent(
            loading = uiState.isLoading,
            title = uiState.title,
            description = uiState.description,
            conductivity = uiState.conductivity,
            radius = uiState.radius,
            depth = uiState.depth,
            geometryFile = uiState.geometryFile,                     // Pass new field
            surfaceTempFile = uiState.surfaceTempFile,
            onTitleChanged = viewModel::updateTitle,
            onDescriptionChanged = viewModel::updateDescription,
            onConductivityChanged = viewModel::updateConductivity,
            onGeometryFileChanged = viewModel::updateGeometryFile,   // Pass new update function
            onSurfaceTempFileChanged = viewModel::updateSurfaceTempFile, // Pass new update function
            modifier = Modifier.padding(paddingValues),
            onRadiusChanged = viewModel::updateRadius,
            onDepthChanged = viewModel::updateDepth
        )

        // Check if the task is saved and call onTaskUpdate event
        LaunchedEffect(uiState.isTaskSaved) {
            if (uiState.isTaskSaved) {
                onTaskUpdate()
            }
        }

        // Check for user messages to display on the screen
        uiState.userMessage?.let { userMessage ->
            val snackbarText = stringResource(userMessage)
            LaunchedEffect(snackbarHostState, viewModel, userMessage, snackbarText) {
                snackbarHostState.showSnackbar(snackbarText)
                viewModel.snackbarMessageShown()
            }
        }
    }
}

@Composable
private fun AddEditTaskContent(
    loading: Boolean,
    title: String,
    description: String,
    conductivity: Double,
    radius: Double,
    depth: Double,// New parameter
    geometryFile: String?,                     // New parameter
    surfaceTempFile: String?,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onConductivityChanged: (Double) -> Unit,
    onRadiusChanged: (Double) -> Unit,
    onDepthChanged: (Double) -> Unit,// New parameter
    onGeometryFileChanged: (String?) -> Unit,  // New parameter
    onSurfaceTempFileChanged: (String?) -> Unit, // New parameter
    modifier: Modifier = Modifier
) {
    // File picker launchers
    val geometryFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        onGeometryFileChanged(uri?.toString())
    }
    val surfaceTempFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        onSurfaceTempFileChanged(uri?.toString())
    }
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshingState = rememberPullToRefreshState()
    if (loading) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = refreshingState,
            onRefresh = { /* DO NOTHING */ },
            content = { }
        )
    } else {
        Column(
            modifier
                .fillMaxWidth()
                .padding(all = dimensionResource(id = R.dimen.horizontal_margin))
                .verticalScroll(rememberScrollState())
        ) {
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onSecondary
            )
            OutlinedTextField(
                value = title,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onTitleChanged,
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.title_hint),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall
                    .copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                colors = textFieldColors
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChanged,
                placeholder = { Text(stringResource(id = R.string.description_hint)) },
                modifier = Modifier
                    .height(100.dp)
                    .fillMaxWidth(),
                colors = textFieldColors
            )
            OutlinedTextField(
                value = conductivity.toString(),
                onValueChange = { newValue ->
                    val doubleValue = newValue.toDoubleOrNull() ?: 0.0
                    onConductivityChanged(doubleValue)
                },
                label = { Text("Conductivity") },
                placeholder = { Text(stringResource(id = R.string.conductivity_hint))  },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = textFieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Radius Field
            OutlinedTextField(
                value = radius.toString(),
                onValueChange = { newValue ->
                    val doubleValue = newValue.toDoubleOrNull() ?: 0.0
                    onRadiusChanged(doubleValue)
                },
                label = { Text("Radius") },
                placeholder = { Text(stringResource(id = R.string.radius_hint))  },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = textFieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Radius Field
            OutlinedTextField(
                value = depth.toString(),
                onValueChange = { newValue ->
                    val doubleValue = newValue.toDoubleOrNull() ?: 0.0
                    onDepthChanged(doubleValue)
                },
                label = { Text("Depth") },
                placeholder = { Text(stringResource(id = R.string.depth_hint))  },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = textFieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Geometry File Field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.geometry_file_label),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { geometryFilePicker.launch("*/*") }, // Allow any file type
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(if (geometryFile != null) "Change File" else "Select File")
                }
            }
            if (geometryFile != null) {
                Text(
                    text = "Selected: ${Uri.parse(geometryFile).lastPathSegment ?: "Unknown file"}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Surface Temp File Field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.surface_temp_file_label),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { surfaceTempFilePicker.launch("*/*") }, // Allow any file type
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(if (surfaceTempFile != null) "Change File" else "Select File")
                }
            }
            if (surfaceTempFile != null) {
                Text(
                    text = "Selected: ${Uri.parse(surfaceTempFile).lastPathSegment ?: "Unknown file"}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
