package com.example.data

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val allFolders: Flow<List<FolderEntity>> = projectDao.getAllFolders()

    suspend fun getProjectById(id: String): ProjectEntity? {
        return projectDao.getProjectById(id)
    }

    suspend fun getAllProjectsOnce(): List<ProjectEntity> {
        return projectDao.getAllProjectsOnce()
    }

    suspend fun insert(project: ProjectEntity) {
        projectDao.insertProject(project)
    }

    suspend fun update(project: ProjectEntity) {
        projectDao.updateProject(project)
    }

    suspend fun delete(project: ProjectEntity) {
        projectDao.deleteProject(project)
    }

    suspend fun deleteById(id: String) {
        projectDao.deleteProjectById(id)
    }

    suspend fun deleteAllProjects() {
        projectDao.deleteAllProjects()
    }

    suspend fun insertFolder(folder: FolderEntity) {
        projectDao.insertFolder(folder)
    }

    suspend fun deleteFolder(folder: FolderEntity) {
        projectDao.deleteFolder(folder)
    }

    suspend fun deleteAllFolders() {
        projectDao.deleteAllFolders()
    }
}
