package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ProjectEntity
import com.example.data.ProjectRepository
import com.example.data.FolderEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.example.widget.WidgetUpdater
import java.util.UUID
import com.example.viewmodel.BackgroundTaskManager
import com.example.viewmodel.BackgroundTask
import com.example.viewmodel.TaskStatus
import com.example.viewmodel.TaskType

class ProjectViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProjectRepository

    val uiState: StateFlow<List<ProjectEntity>>
    val foldersState: StateFlow<List<FolderEntity>>
    val dirtyProject: StateFlow<ProjectEntity?>

    init {
        val projectDao = AppDatabase.getDatabase(application).projectDao()
        repository = ProjectRepository(projectDao)
        uiState = repository.allProjects
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
            
        foldersState = repository.allFolders
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
            
        dirtyProject = repository.allProjects.map { list -> list.find { it.isDirty } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    suspend fun getProject(id: String): ProjectEntity? {
        return repository.getProjectById(id)
    }

    fun addProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.insert(project)
            WidgetUpdater.updateWidgets(getApplication<Application>().applicationContext)
        }
    }

    fun updateProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.update(project)
            WidgetUpdater.updateWidgets(getApplication<Application>().applicationContext)
        }
    }

    fun autoSaveProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.update(project)
            val context = getApplication<Application>().applicationContext
            com.example.NotificationHelper.showAutoSaveNotification(context, project.name)
            WidgetUpdater.updateWidgets(context)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.delete(project)
            WidgetUpdater.updateWidgets(getApplication<Application>().applicationContext)
        }
    }
    
    fun syncProjectToCloud(project: ProjectEntity) {
        val taskId = UUID.randomUUID().toString()
        BackgroundTaskManager.addTask(
            BackgroundTask(
                id = taskId,
                title = "Syncing ${project.name}",
                type = TaskType.CLOUD_SYNC,
                status = TaskStatus.RUNNING
            )
        )
        
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            com.example.NotificationHelper.showCloudSyncNotification(context, project.name, false)
            repository.update(project.copy(syncStatus = "syncing", progress = 0.1f))
            BackgroundTaskManager.updateProgress(taskId, 0.1f, "Connecting to cloud...")
            kotlinx.coroutines.delay(1000)
            
            repository.update(project.copy(syncStatus = "syncing", progress = 0.5f))
            BackgroundTaskManager.updateProgress(taskId, 0.5f, "Uploading media assets...")
            kotlinx.coroutines.delay(1500)
            
            repository.update(project.copy(syncStatus = "syncing", progress = 0.9f))
            BackgroundTaskManager.updateProgress(taskId, 0.9f, "Finalizing project state...")
            kotlinx.coroutines.delay(1000)
            
            val size = (50000..500000).random().toLong()
            repository.update(project.copy(syncStatus = "synced", lastBackupTime = System.currentTimeMillis(), backupSize = size, progress = null))
            com.example.NotificationHelper.showCloudSyncNotification(context, project.name, true)
            BackgroundTaskManager.updateStatus(taskId, TaskStatus.COMPLETED)
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.insertFolder(FolderEntity(name = name))
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            // Un-folder projects when a folder is deleted
            uiState.value.filter { it.folderId == folder.id }.forEach {
                repository.update(it.copy(folderId = null))
            }
            repository.deleteFolder(folder)
        }
    }
}
