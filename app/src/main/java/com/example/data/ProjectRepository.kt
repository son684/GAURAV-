package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Int): Project? {
        return projectDao.getProjectById(id)
    }

    suspend fun insertProject(project: Project): Long {
        return projectDao.insertProject(project)
    }

    suspend fun updateProject(project: Project) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProjectById(id: Int) {
        projectDao.deleteProjectById(id)
    }

    fun getElementsForProject(projectId: Int): Flow<List<VisualElement>> {
        return projectDao.getElementsForProject(projectId)
    }

    suspend fun insertElement(element: VisualElement): Long {
        return projectDao.insertElement(element)
    }

    suspend fun deleteElementById(id: Int) {
        projectDao.deleteElementById(id)
        projectDao.deleteKeyframesForElement(id)
    }

    fun getKeyframesForProject(projectId: Int): Flow<List<KeyFrame>> {
        return projectDao.getKeyframesForProject(projectId)
    }

    fun getKeyframesForElement(elementId: Int): Flow<List<KeyFrame>> {
        return projectDao.getKeyframesForElement(elementId)
    }

    suspend fun insertKeyframe(keyframe: KeyFrame): Long {
        return projectDao.insertKeyframe(keyframe)
    }

    suspend fun deleteKeyframeById(id: Int) {
        projectDao.deleteKeyframeById(id)
    }

    // --- Simulated Cloud Sync with Conflict Check & States ---
    suspend fun syncProjectToCloud(projectId: Int, onStatusChange: (SyncStatus) -> Unit) {
        try {
            onStatusChange(SyncStatus.Syncing(0.1f))
            delay(500)
            onStatusChange(SyncStatus.Syncing(0.4f))
            
            val project = getProjectById(projectId) ?: throw Exception("Project not found")
            val elements = projectDao.getElementsForProjectSync(projectId)
            
            delay(600)
            onStatusChange(SyncStatus.Syncing(0.8f))
            delay(400)
            
            // Mark project as synced in database
            val updatedProject = project.copy(
                isSynced = true,
                lastUpdated = System.currentTimeMillis()
            )
            projectDao.updateProject(updatedProject)
            
            onStatusChange(SyncStatus.Success)
        } catch (e: Exception) {
            onStatusChange(SyncStatus.Failed(e.localizedMessage ?: "Network error during sync"))
        }
    }
}

sealed interface SyncStatus {
    object Idle : SyncStatus
    data class Syncing(val progress: Float) : SyncStatus
    object Success : SyncStatus
    data class Failed(val error: String) : SyncStatus
}
