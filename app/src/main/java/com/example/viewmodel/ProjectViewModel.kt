package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.FolderEntity
import com.example.data.ProjectEntity
import com.example.data.ProjectRepository
import com.example.data.ProjectStorage
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProjectRepository
    private var autoSaveJob: Job? = null

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

        dirtyProject = repository.allProjects
            .map { list -> list.find { it.isDirty } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    suspend fun getProject(id: String): ProjectEntity? = repository.getProjectById(id)

    fun addProject(project: ProjectEntity) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            repository.insert(ProjectStorage.normalize(context, project))
            com.example.utils.AnalyticsManager.trackVideoCreated()
            WidgetUpdater.updateWidgets(context)
        }
    }

    fun updateProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.update(ProjectStorage.normalize(getApplication(), project))
            WidgetUpdater.updateWidgets(getApplication<Application>().applicationContext)
        }
    }

    fun autoSaveProject(project: ProjectEntity) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            val context = getApplication<Application>().applicationContext
            repository.update(ProjectStorage.normalize(context, project))
            WidgetUpdater.updateWidgets(context)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.delete(project)
            ProjectStorage.deleteOwnedFiles(getApplication(), project)
            WidgetUpdater.updateWidgets(getApplication<Application>().applicationContext)
        }
    }

    fun deleteAllLocalData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val projects = repository.getAllProjectsOnce()
            repository.deleteAllProjects()
            repository.deleteAllFolders()
            projects.forEach { ProjectStorage.deleteOwnedFiles(context, it) }
            context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
            WidgetUpdater.updateWidgets(context)
            onComplete()
        }
    }

    fun syncProjectToCloud(project: ProjectEntity) {
        android.widget.Toast.makeText(
            getApplication<Application>(),
            "Cloud backup is not available. This project is stored on this device.",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.insertFolder(FolderEntity(name = name))
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            // Un-folder projects when a folder is deleted.
            uiState.value
                .filter { it.folderId == folder.id }
                .forEach { repository.update(it.copy(folderId = null)) }
            repository.deleteFolder(folder)
        }
    }
}
