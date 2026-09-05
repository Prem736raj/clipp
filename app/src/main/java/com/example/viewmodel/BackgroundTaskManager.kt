package com.example.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class TaskType {
    EXPORT, AI_PROCESSING, CLOUD_SYNC
}

enum class TaskStatus {
    QUEUED, RUNNING, PAUSED, CANCELLED, COMPLETED, FAILED
}

data class BackgroundTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: TaskType,
    var progress: Float = 0f, // 0 to 1
    var status: TaskStatus = TaskStatus.QUEUED,
    var isHighPriority: Boolean = false,
    var message: String? = null
)

object BackgroundTaskManager {
    private val _tasks = MutableStateFlow<List<BackgroundTask>>(emptyList())
    val tasks: StateFlow<List<BackgroundTask>> = _tasks.asStateFlow()

    fun addTask(task: BackgroundTask) {
        _tasks.value = _tasks.value + task
        maybeStartNextTask()
    }

    fun updateProgress(id: String, progress: Float, msg: String? = null) {
        _tasks.value = _tasks.value.map {
            if (it.id == id) it.copy(progress = progress, message = msg ?: it.message) else it
        }
    }

    fun updateStatus(id: String, status: TaskStatus) {
        _tasks.value = _tasks.value.map {
            if (it.id == id) it.copy(status = status) else it
        }
        if (status == TaskStatus.CANCELLED || status == TaskStatus.COMPLETED || status == TaskStatus.FAILED || status == TaskStatus.PAUSED) {
            maybeStartNextTask()
        }
    }

    fun pauseTask(id: String) {
        updateStatus(id, TaskStatus.PAUSED)
    }

    fun resumeTask(id: String) {
        updateStatus(id, TaskStatus.QUEUED)
        maybeStartNextTask()
    }

    fun cancelTask(id: String) {
        updateStatus(id, TaskStatus.CANCELLED)
    }
    
    fun prioritizeTask(id: String) {
        val currentTasks = _tasks.value.toMutableList()
        val index = currentTasks.indexOfFirst { it.id == id }
        if (index != -1) {
            val task = currentTasks.removeAt(index)
            currentTasks.add(0, task.copy(isHighPriority = true, status = TaskStatus.QUEUED))
            // pause currently running if needed...
        }
        _tasks.value = currentTasks
        maybeStartNextTask()
    }

    private fun maybeStartNextTask() {
        val runningCount = _tasks.value.count { it.status == TaskStatus.RUNNING }
        if (runningCount < 2) { // Max 2 concurrent tasks
            val nextTask = _tasks.value.firstOrNull { it.status == TaskStatus.QUEUED }
            if (nextTask != null) {
                updateStatus(nextTask.id, TaskStatus.RUNNING)
                // In a real app we would start the WorkManager or Foreground Service here
            }
        }
    }
    
    fun clearCompleted() {
        _tasks.value = _tasks.value.filter { 
            it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED && it.status != TaskStatus.FAILED 
        }
    }
}
