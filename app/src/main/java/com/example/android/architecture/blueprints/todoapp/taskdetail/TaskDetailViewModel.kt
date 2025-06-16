/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.TodoDestinationsArgs
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.TaskRepository
import com.example.android.architecture.blueprints.todoapp.network.PinnApiClient
import com.example.android.architecture.blueprints.todoapp.network.PinnOutput
import com.example.android.architecture.blueprints.todoapp.network.PinnResponse
import com.example.android.architecture.blueprints.todoapp.util.Async
import com.example.android.architecture.blueprints.todoapp.util.WhileUiSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * UiState for the Details screen.
 */
data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = false,
    val userMessage: Int? = null,
    val isTaskDeleted: Boolean = false,
    val sendToPinnTriggered: Boolean = false,
    val optimizedOutput: PinnOutput? = null // New state for optimized values
)

/**
 * ViewModel for the Details screen.
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val taskId: String = savedStateHandle[TodoDestinationsArgs.TASK_ID_ARG]!!

    private val _userMessage: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isTaskDeleted = MutableStateFlow(false)
    private val _sendToPinnTriggered = MutableStateFlow(false)
    private val _optimizedOutput = MutableStateFlow<PinnOutput?>(null) // New state
    private val _taskAsync = taskRepository.getTaskStream(taskId)
        .map { handleTask(it) }
        .catch { emit(Async.Error(R.string.loading_task_error)) }

    val uiState: StateFlow<TaskDetailUiState> = combine(
        _userMessage, _isLoading, _isTaskDeleted, _sendToPinnTriggered, _taskAsync, _optimizedOutput
    ) { flows ->
        val userMessage = flows[0] as Int?
        val isLoading = flows[1] as Boolean
        val isTaskDeleted = flows[2] as Boolean
        val sendToPinnTriggered = flows[3] as Boolean
        val taskAsync = flows[4] as Async<Task?>
        val optimizedOutput = flows[5] as PinnOutput?

        when (taskAsync) {
            Async.Loading -> {
                TaskDetailUiState(isLoading = true)
            }
            is Async.Error -> {
                TaskDetailUiState(
                    userMessage = taskAsync.errorMessage,
                    isTaskDeleted = isTaskDeleted
                )
            }
            is Async.Success -> {
                TaskDetailUiState(
                    task = taskAsync.data,
                    isLoading = isLoading,
                    userMessage = userMessage,
                    isTaskDeleted = isTaskDeleted,
                    sendToPinnTriggered = sendToPinnTriggered,
                    optimizedOutput = optimizedOutput
                )
            }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = WhileUiSubscribed,
            initialValue = TaskDetailUiState(isLoading = true)
        )

    fun deleteTask() = viewModelScope.launch {
        taskRepository.deleteTask(taskId)
        _isTaskDeleted.value = true
    }

    fun setCompleted(completed: Boolean) = viewModelScope.launch {
        val task = uiState.value.task ?: return@launch
        if (completed) {
            taskRepository.completeTask(task.id)
            showSnackbarMessage(R.string.task_marked_complete)
        } else {
            taskRepository.activateTask(task.id)
            showSnackbarMessage(R.string.task_marked_active)
        }
    }

    fun refresh() {
        _isLoading.value = true
        viewModelScope.launch {
            taskRepository.refreshTask(taskId)
            _isLoading.value = false
        }
    }

    fun snackbarMessageShown() {
        _userMessage.value = null
    }

    fun sendToPinn() {
        val task = uiState.value.task ?: return
        _sendToPinnTriggered.value = true
        setLoading(true)

        val geometryFile = task.geometryFile?.let { File(it) } ?: return
        val surfaceTempFile = task.surfaceTempFile?.let { File(it) } ?: return

        viewModelScope.launch {
            PinnApiClient.predictTask(
                conductivity = task.conductivity,
                radius = task.radius,
                depth = task.depth,
                geometryFile = geometryFile,
                surfaceTempFile = surfaceTempFile
            ) { response, error ->
                if (error != null) {
                    showSnackbarMessage(R.string.error_running_model)
                    setLoading(false)
                    _sendToPinnTriggered.value = false
                } else if (response?.status == "success" && response.output != null) {
                    _optimizedOutput.value = response.output
                    setLoading(false)
                    _sendToPinnTriggered.value = false
                } else {
                    showSnackbarMessage(R.string.error_running_model)
                    setLoading(false)
                    _sendToPinnTriggered.value = false
                }
            }
        }
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    private fun showSnackbarMessage(message: Int) {
        _userMessage.value = message
    }

    private fun handleTask(task: Task?): Async<Task?> {
        if (task == null) {
            return Async.Error(R.string.task_not_found)
        }
        return Async.Success(task)
    }
}