package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.ColorGradeResult
import com.example.api.GeminiClient
import com.example.api.UpscaleResult
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ProjectDatabase.getDatabase(application)
    private val repository = ProjectRepository(database.projectDao())

    // UI state streams
    val projects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentProjectId = MutableStateFlow<Int?>(null)
    val currentProjectId: StateFlow<Int?> = _currentProjectId.asStateFlow()

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _elements = MutableStateFlow<List<VisualElement>>(emptyList())
    val elements: StateFlow<List<VisualElement>> = _elements.asStateFlow()

    private val _keyframes = MutableStateFlow<List<KeyFrame>>(emptyList())
    val keyframes: StateFlow<List<KeyFrame>> = _keyframes.asStateFlow()

    // Timeline and Playback States
    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs: StateFlow<Long> = _currentTimeMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _selectedElementId = MutableStateFlow<Int?>(null)
    val selectedElementId: StateFlow<Int?> = _selectedElementId.asStateFlow()

    // AI & Sync states
    private val _gradingStatus = MutableStateFlow<String?>(null)
    val gradingStatus: StateFlow<String?> = _gradingStatus.asStateFlow()

    private val _isGrading = MutableStateFlow(false)
    val isGrading: StateFlow<Boolean> = _isGrading.asStateFlow()

    private val _upscaleStatus = MutableStateFlow<String?>(null)
    val upscaleStatus: StateFlow<String?> = _upscaleStatus.asStateFlow()

    private val _isUpscaling = MutableStateFlow(false)
    val isUpscaling: StateFlow<Boolean> = _isUpscaling.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var playbackJob: Job? = null
    private var observeJob: Job? = null

    init {
        // Create initial starter project if list is empty
        viewModelScope.launch {
            projects.collectLatest { list ->
                if (list.isEmpty() && _currentProjectId.value == null) {
                    createStarterProject()
                } else if (list.isNotEmpty() && _currentProjectId.value == null) {
                    selectProject(list.first().id)
                }
            }
        }
    }

    private suspend fun createStarterProject() {
        val projId = repository.insertProject(
            Project(
                name = "Cinematic Intro",
                brightness = 1.0f,
                contrast = 1.1f,
                saturation = 1.2f,
                temperature = 0.1f,
                vignette = 0.2f,
                lutFilterName = "Retro Gold"
            )
        ).toInt()

        // Starter visual elements
        val neonGridId = repository.insertElement(
            VisualElement(
                projectId = projId,
                name = "Cyberpunk Grid Overlay",
                type = "GRID",
                colorHex = "#FF00FF",
                startMs = 0L,
                durationMs = 6000L
            )
        ).toInt()

        val lensFlareId = repository.insertElement(
            VisualElement(
                projectId = projId,
                name = "Laser Flare Sparkle",
                type = "FLARE",
                colorHex = "#00FFFF",
                startMs = 2000L,
                durationMs = 5000L
            )
        ).toInt()

        val glitchId = repository.insertElement(
            VisualElement(
                projectId = projId,
                name = "Core Glitch Overlay",
                type = "GLITCH",
                colorHex = "#FFFF00",
                startMs = 4000L,
                durationMs = 4000L
            )
        ).toInt()

        // Add interesting keyframes
        // Neon Grid: fade in & scale up
        repository.insertKeyframe(KeyFrame(elementId = neonGridId, timeMs = 0L, scale = 0.5f, rotation = 0f, opacity = 0.0f))
        repository.insertKeyframe(KeyFrame(elementId = neonGridId, timeMs = 3000L, scale = 1.2f, rotation = 15f, opacity = 1.0f))
        repository.insertKeyframe(KeyFrame(elementId = neonGridId, timeMs = 6000L, scale = 1.8f, rotation = 45f, opacity = 0.0f))

        // Laser Flare: float left to right
        repository.insertKeyframe(KeyFrame(elementId = lensFlareId, timeMs = 0L, scale = 0.8f, rotation = 0f, opacity = 0.8f, posX = -120f, posY = -40f))
        repository.insertKeyframe(KeyFrame(elementId = lensFlareId, timeMs = 2500L, scale = 2.0f, rotation = 180f, opacity = 1.0f, posX = 0f, posY = 20f))
        repository.insertKeyframe(KeyFrame(elementId = lensFlareId, timeMs = 5000L, scale = 0.8f, rotation = 360f, opacity = 0.2f, posX = 120f, posY = -40f))

        // Core Glitch: violent scale & rapid rotate
        repository.insertKeyframe(KeyFrame(elementId = glitchId, timeMs = 0L, scale = 1.0f, rotation = 0f, opacity = 0.1f))
        repository.insertKeyframe(KeyFrame(elementId = glitchId, timeMs = 2000L, scale = 2.5f, rotation = -90f, opacity = 0.9f))
        repository.insertKeyframe(KeyFrame(elementId = glitchId, timeMs = 4000L, scale = 1.0f, rotation = -180f, opacity = 0.0f))

        selectProject(projId)
    }

    fun selectProject(projectId: Int) {
        _currentProjectId.value = projectId
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            // Observe current project metadata
            launch {
                flow {
                    while (true) {
                        val proj = repository.getProjectById(projectId)
                        emit(proj)
                        delay(1000)
                    }
                }.collectLatest {
                    _currentProject.value = it
                }
            }

            // Observe core elements
            launch {
                repository.getElementsForProject(projectId).collectLatest {
                    _elements.value = it
                    if (_selectedElementId.value == null && it.isNotEmpty()) {
                        _selectedElementId.value = it.first().id
                    }
                }
            }

            // Observe all keyframes associated with this project
            launch {
                repository.getKeyframesForProject(projectId).collectLatest {
                    _keyframes.value = it
                }
            }
        }
    }

    fun createNewProject(name: String) {
        viewModelScope.launch {
            val projId = repository.insertProject(
                Project(
                    name = name.ifBlank { "Unsaved Project" },
                    lutFilterName = "Normal"
                )
            ).toInt()
            
            // Starter default element
            val starterElemId = repository.insertElement(
                VisualElement(
                    projectId = projId,
                    name = "AI Glow Dust Particle",
                    type = "PARTICLE",
                    colorHex = "#00FFFF",
                    startMs = 0L,
                    durationMs = 10000L
                )
            ).toInt()
            
            repository.insertKeyframe(KeyFrame(elementId = starterElemId, timeMs = 0L, scale = 1.0f, rotation = 0f, opacity = 0.2f, posX = 0f, posY = 0f))
            repository.insertKeyframe(KeyFrame(elementId = starterElemId, timeMs = 5000L, scale = 1.6f, rotation = 180f, opacity = 0.8f, posX = -50f, posY = 40f))
            repository.insertKeyframe(KeyFrame(elementId = starterElemId, timeMs = 10000L, scale = 1.0f, rotation = 360f, opacity = 0.2f, posX = 50f, posY = -40f))

            selectProject(projId)
        }
    }

    fun deleteCurrentProject() {
        val projId = currentProjectId.value ?: return
        viewModelScope.launch {
            repository.deleteProjectById(projId)
            _currentProjectId.value = null
            _currentProject.value = null
            _selectedElementId.value = null
            // Reset states
            _currentTimeMs.value = 0L
            _isPlaying.value = false
        }
    }

    // --- Playback mechanics ---
    fun togglePlay() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    private fun play() {
        playbackJob?.cancel()
        _isPlaying.value = true
        playbackJob = viewModelScope.launch {
            val projectDuration = _currentProject.value?.durationMs ?: 10000L
            val stepMs = 50L
            while (_isPlaying.value) {
                delay(stepMs)
                var nextTime = _currentTimeMs.value + stepMs
                if (nextTime >= projectDuration) {
                    nextTime = 0L // loop playhead
                }
                _currentTimeMs.value = nextTime
            }
        }
    }

    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun seekTo(timeMs: Long) {
        val duration = _currentProject.value?.durationMs ?: 10000L
        _currentTimeMs.value = timeMs.coerceIn(0L, duration)
    }

    // --- Track Element Actions ---
    fun selectElement(elementId: Int) {
        _selectedElementId.value = elementId
    }

    fun addNewElement(name: String, type: String, colorHex: String) {
        val projId = currentProjectId.value ?: return
        viewModelScope.launch {
            val elemId = repository.insertElement(
                VisualElement(
                    projectId = projId,
                    name = name,
                    type = type,
                    colorHex = colorHex,
                    startMs = _currentTimeMs.value,
                    durationMs = 4000L
                )
            ).toInt()
            
            // Add default initial keyframe at placement
            repository.insertKeyframe(KeyFrame(elementId = elemId, timeMs = 0L, scale = 1.0f, rotation = 0f, opacity = 1.0f))
            _selectedElementId.value = elemId
        }
    }

    fun deleteCurrentElement() {
        val elemId = selectedElementId.value ?: return
        viewModelScope.launch {
            repository.deleteElementById(elemId)
            _selectedElementId.value = null
        }
    }

    // --- Keyframe Manipulation ---
    fun createOrUpdateKeyframeForSelected(scale: Float, rotation: Float, opacity: Float, posX: Float, posY: Float) {
        val elemId = selectedElementId.value ?: return
        val currentElem = elements.value.find { it.id == elemId } ?: return
        val offsetTime = (_currentTimeMs.value - currentElem.startMs).coerceIn(0L, currentElem.durationMs)
        
        viewModelScope.launch {
            val existingKfs = keyframes.value.filter { it.elementId == elemId }
            // If there's an existing keyframe within 250ms, update it, otherwise insert new
            val snapThreshold = 250L
            val nearbyKf = existingKfs.find { Math.abs(it.timeMs - offsetTime) <= snapThreshold }
            
            if (nearbyKf != null) {
                val updated = nearbyKf.copy(
                    scale = scale,
                    rotation = rotation,
                    opacity = opacity,
                    posX = posX,
                    posY = posY
                )
                repository.insertKeyframe(updated) // REPLACE conflict inserts
            } else {
                repository.insertKeyframe(
                    KeyFrame(
                        elementId = elemId,
                        timeMs = offsetTime,
                        scale = scale,
                        rotation = rotation,
                        opacity = opacity,
                        posX = posX,
                        posY = posY
                    )
                )
            }
        }
    }

    fun deleteKeyframeAtCurrent() {
        val elemId = selectedElementId.value ?: return
        val currentElem = elements.value.find { it.id == elemId } ?: return
        val offsetTime = (_currentTimeMs.value - currentElem.startMs).coerceIn(0L, currentElem.durationMs)
        
        viewModelScope.launch {
            val existingKfs = keyframes.value.filter { it.elementId == elemId }
            val snapThreshold = 250L
            val nearbyKf = existingKfs.find { Math.abs(it.timeMs - offsetTime) <= snapThreshold }
            if (nearbyKf != null) {
                repository.deleteKeyframeById(nearbyKf.id)
            }
        }
    }

    // Interpolation utility for the visualization panel
    fun getInterpolatedStateForElement(elementId: Int): ElementState {
        val elem = elements.value.find { it.id == elementId } ?: return ElementState()
        val playhead = currentTimeMs.value
        
        // Element only exists during its lifetime
        if (playhead < elem.startMs || playhead > (elem.startMs + elem.durationMs)) {
            return ElementState(isVisible = false)
        }
        
        val offsetTime = playhead - elem.startMs
        val kfs = keyframes.value.filter { it.elementId == elementId }.sortedBy { it.timeMs }
        
        if (kfs.isEmpty()) {
            return ElementState(isVisible = true)
        }
        if (kfs.size == 1) {
            val only = kfs.first()
            return ElementState(
                isVisible = true,
                scale = only.scale,
                rotation = only.rotation,
                opacity = only.opacity,
                posX = only.posX,
                posY = only.posY
            )
        }
        
        // Find interval
        val firstKf = kfs.first()
        val lastKf = kfs.last()
        
        if (offsetTime <= firstKf.timeMs) {
            return ElementState(
                isVisible = true,
                scale = firstKf.scale,
                rotation = firstKf.rotation,
                opacity = firstKf.opacity,
                posX = firstKf.posX,
                posY = firstKf.posY
            )
        }
        if (offsetTime >= lastKf.timeMs) {
            return ElementState(
                isVisible = true,
                scale = lastKf.scale,
                rotation = lastKf.rotation,
                opacity = lastKf.opacity,
                posX = lastKf.posX,
                posY = lastKf.posY
            )
        }
        
        // Interpolate between surrounding keyframes
        var nextIndex = kfs.indexOfFirst { it.timeMs > offsetTime }
        if (nextIndex == -1) nextIndex = kfs.size - 1
        
        val kfMin = kfs[nextIndex - 1]
        val kfMax = kfs[nextIndex]
        
        val duration = (kfMax.timeMs - kfMin.timeMs).toFloat()
        val fraction = if (duration == 0f) 0f else (offsetTime - kfMin.timeMs) / duration
        
        return ElementState(
            isVisible = true,
            scale = kfMin.scale + fraction * (kfMax.scale - kfMin.scale),
            rotation = kfMin.rotation + fraction * (kfMax.rotation - kfMin.rotation),
            opacity = kfMin.opacity + fraction * (kfMax.opacity - kfMin.opacity),
            posX = kfMin.posX + fraction * (kfMax.posX - kfMin.posX),
            posY = kfMin.posY + fraction * (kfMax.posY - kfMin.posY)
        )
    }

    // --- Color Grading Adjustments ---
    fun updateManualColorOffsets(brightness: Float, contrast: Float, saturation: Float, temperature: Float, vignette: Float) {
        val proj = currentProject.value ?: return
        viewModelScope.launch {
            val updated = proj.copy(
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                temperature = temperature,
                vignette = vignette,
                lutFilterName = "Custom Slider"
            )
            repository.updateProject(updated)
            _currentProject.value = updated
        }
    }

    fun applyLUTFilter(filterName: String) {
        val proj = currentProject.value ?: return
        viewModelScope.launch {
            val updated = when (filterName) {
                "Cinema Teal" -> proj.copy(lutFilterName = "Cinema Teal", brightness = 0.95f, contrast = 1.25f, saturation = 0.85f, temperature = -0.3f, vignette = 0.4f)
                "Neon Cyber" -> proj.copy(lutFilterName = "Neon Cyber", brightness = 1.05f, contrast = 1.3f, saturation = 1.45f, temperature = -0.5f, vignette = 0.1f)
                "Retro Gold" -> proj.copy(lutFilterName = "Retro Gold", brightness = 1.0f, contrast = 1.0f, saturation = 1.15f, temperature = 0.4f, vignette = 0.2f)
                "Moody Noir" -> proj.copy(lutFilterName = "Moody Noir", brightness = 0.85f, contrast = 1.4f, saturation = 0.0f, temperature = 0.0f, vignette = 0.6f)
                "Classic Sepia" -> proj.copy(lutFilterName = "Classic Sepia", brightness = 0.95f, contrast = 0.95f, saturation = 0.5f, temperature = 0.6f, vignette = 0.3f)
                else -> proj.copy(lutFilterName = "Normal", brightness = 1.0f, contrast = 1.0f, saturation = 1.0f, temperature = 0.0f, vignette = 0.0f)
            }
            repository.updateProject(updated)
            _currentProject.value = updated
        }
    }

    // --- AI Automated Color Grading ---
    fun applyAICohesiveColorGrading(userPrompt: String) {
        val proj = currentProject.value ?: return
        if (userPrompt.isBlank()) return

        _isGrading.value = true
        _gradingStatus.value = "Consulting Gemini AI Colorist..."

        viewModelScope.launch {
            val result = GeminiClient.getColorGradeFromPrompt(userPrompt)
            when (result) {
                is ColorGradeResult.Success -> {
                    val updated = proj.copy(
                        brightness = result.brightness,
                        contrast = result.contrast,
                        saturation = result.saturation,
                        temperature = result.temperature,
                        vignette = result.vignette,
                        lutFilterName = "AI: ${result.filterName}"
                    )
                    repository.updateProject(updated)
                    _currentProject.value = updated
                    _gradingStatus.value = "Applied AI Color Preset: '${result.filterName}' successfully."
                }
                is ColorGradeResult.Error -> {
                    _gradingStatus.value = "AI Grading failed: ${result.message}. Applying offline cinematic blend."
                    // Fail-safe offline placeholder matching
                    delay(1200)
                    applyLUTFilter("Cinema Teal")
                }
            }
            _isGrading.value = false
        }
    }

    fun dismissGradingStatus() {
        _gradingStatus.value = null
    }

    // --- AI Image Upscale Resolution Clarifier ---
    fun triggerAIResolutionUpscaling() {
        val proj = currentProject.value ?: return
        val activeElem = elements.value.find { it.id == selectedElementId.value }
        val targetName = activeElem?.name ?: "Primary Video Frame Buffer"

        _isUpscaling.value = true
        _upscaleStatus.value = "Upscaling raw footage..."

        viewModelScope.launch {
            // Apply visual upscaling config changes
            val updated = proj.copy(upscaleMode = "AI Hyper-Clarity")
            repository.updateProject(updated)
            _currentProject.value = updated

            val result = GeminiClient.getUpscaleDetailsFromGemini(targetName)
            when (result) {
                is UpscaleResult.Success -> {
                    _upscaleStatus.value = "AI Super-Scale Complete! Details reconstructed:\n" +
                            "${result.reconstructedDetails}\n" +
                            "PSNR Metric Improvement: ${result.psnrIncrease}"
                }
                is UpscaleResult.Error -> {
                    _upscaleStatus.value = "Upscale Error: ${result.message}. Local super-sampling interpolation engaged."
                }
            }
            _isUpscaling.value = false
        }
    }

    fun setUpscaleMode(mode: String) {
        val proj = currentProject.value ?: return
        viewModelScope.launch {
            val updated = proj.copy(upscaleMode = mode)
            repository.updateProject(updated)
            _currentProject.value = updated
        }
    }

    fun dismissUpscaleStatus() {
        _upscaleStatus.value = null
    }

    // --- Project Cloud Sync Button Action ---
    fun syncCurrentProject() {
        val projId = currentProjectId.value ?: return
        viewModelScope.launch {
            repository.syncProjectToCloud(projId) { status ->
                _syncStatus.value = status
            }
        }
    }

    fun resetSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }
}

data class ElementState(
    val isVisible: Boolean = false,
    val scale: Float = 1.0f,
    val rotation: Float = 0.0f,
    val opacity: Float = 1.0f,
    val posX: Float = 0.0f,
    val posY: Float = 0.0f
)
