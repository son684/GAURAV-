package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.EditorViewModel
import com.example.ui.ElementState
import com.example.ui.theme.*
import com.example.data.*
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainEditorScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainEditorScreen(viewModel: EditorViewModel) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val elements by viewModel.elements.collectAsStateWithLifecycle()
    val keyframes by viewModel.keyframes.collectAsStateWithLifecycle()
    val currentTimeMs by viewModel.currentTimeMs.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val selectedElementId by viewModel.selectedElementId.collectAsStateWithLifecycle()
    
    val isGrading by viewModel.isGrading.collectAsStateWithLifecycle()
    val gradingStatus by viewModel.gradingStatus.collectAsStateWithLifecycle()
    val isUpscaling by viewModel.isUpscaling.collectAsStateWithLifecycle()
    val upscaleStatus by viewModel.upscaleStatus.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Timeline & KFs, 1: AI Grading, 2: Image Upscaler, 3: Projects list
    var showAddElementDialog by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // --- TOP HEADER ROW ---
            HeaderRow(
                currentProject = currentProject,
                syncStatus = syncStatus,
                onSyncClick = { viewModel.syncCurrentProject() },
                onNewProjectClick = { showNewProjectDialog = true },
                onManageProjectsClick = { activeTab = 3 },
                onDeleteProjectClick = { viewModel.deleteCurrentProject() }
            )

            HorizontalDivider(color = Color(0xFF1E1E28), thickness = 1.dp)

            // --- WORKSPACE VIEWPORT (Canvas) ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                // If upscaler is currently viewed, show the interactive split slider
                if (activeTab == 2) {
                    SplitUpscaleViewport(
                        currentProject = currentProject,
                        elements = elements,
                        selectedElementId = selectedElementId,
                        isPlaying = isPlaying,
                        currentTimeMs = currentTimeMs,
                        viewModel = viewModel
                    )
                } else {
                    NormalCanvasViewport(
                        currentProject = currentProject,
                        elements = elements,
                        selectedElementId = selectedElementId,
                        isPlaying = isPlaying,
                        currentTimeMs = currentTimeMs,
                        viewModel = viewModel
                    )
                }
            }

            // --- TAB CONTENT VIEWPORT Drawer --- (Styled like the timeline container: h-44 bg-[#1C1B1F] rounded-t-3xl border-t border-white/10)
            Box(
                modifier = Modifier
                    .height(295.dp)
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .border(
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(SurfaceSlate)
            ) {
                when (activeTab) {
                    0 -> TimelineTabContent(
                        viewModel = viewModel,
                        currentProject = currentProject,
                        elements = elements,
                        keyframes = keyframes,
                        currentTimeMs = currentTimeMs,
                        selectedElementId = selectedElementId,
                        isPlaying = isPlaying,
                        onAddElementClick = { showAddElementDialog = true }
                    )
                    1 -> ColorGradingTabContent(
                        viewModel = viewModel,
                        currentProject = currentProject,
                        isGrading = isGrading,
                        gradingStatus = gradingStatus
                    )
                    2 -> UpscalerTabContent(
                        viewModel = viewModel,
                        currentProject = currentProject,
                        isUpscaling = isUpscaling,
                        upscaleStatus = upscaleStatus,
                        elements = elements,
                        selectedElementId = selectedElementId
                    )
                    3 -> ProjectLibraryTabContent(
                        projects = projects,
                        currentProjectId = currentProject?.id,
                        onSelectProject = { viewModel.selectProject(it) },
                        onNewClick = { showNewProjectDialog = true }
                    )
                }
            }

            // --- WORKSPACE NAVIGATION TABS ---
            TabNavigationRow(
                activeTab = activeTab,
                onTabSelect = { activeTab = it }
            )
        }
    }

    // Dialogs
    if (showAddElementDialog) {
        val elementTypes = listOf(
            Triple("GRID", "Cyberpunk Neon Grid", "#FF00FF"),
            Triple("FLARE", "Sci-Fi Laser Flare", "#00FFFF"),
            Triple("GLITCH", "Polygonal Core Glitch", "#FFFF00"),
            Triple("PARTICLE", "Glowing Light Dust", "#00FF88"),
            Triple("TEXT", "Dynamic Floating Text", "#FFFFFF")
        )
        var nameInput by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(elementTypes.first()) }

        AlertDialog(
            onDismissRequest = { showAddElementDialog = false },
            title = { Text("Add Track Element", color = Color.White) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Select Overlay Template:", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(elementTypes) { item ->
                            val isChosen = selectedType.first == item.first
                            Card(
                                onClick = {
                                    selectedType = item
                                    nameInput = item.second
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color(0xFF1E1E26)
                                ),
                                border = if (isChosen) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .width(140.dp)
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Icon(
                                        imageVector = when (item.first) {
                                            "GRID" -> Icons.Rounded.GridOn
                                            "FLARE" -> Icons.Rounded.FlashOn
                                            "GLITCH" -> Icons.Rounded.Texture
                                            "PARTICLE" -> Icons.Rounded.BubbleChart
                                            else -> Icons.Rounded.TextFields
                                        },
                                        contentDescription = item.second,
                                        tint = Color(android.graphics.Color.parseColor(item.third))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(item.second, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Custom Identifier") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addNewElement(
                            name = nameInput.ifBlank { selectedType.second },
                            type = selectedType.first,
                            colorHex = selectedType.third
                        )
                        showAddElementDialog = false
                    },
                    modifier = Modifier.testTag("submit_add_element")
                ) {
                    Text("Add Override")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddElementDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF15151C)
        )
    }

    if (showNewProjectDialog) {
        var inputName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Create New Edit Project", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    placeholder = { Text("e.g. Cyberpunk Promo Alpha") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNewProject(inputName)
                        showNewProjectDialog = false
                    },
                    modifier = Modifier.testTag("submit_create_project")
                ) {
                    Text("Assemble Timeline")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF15151C)
        )
    }
}

// --- HEADER ROW COMPONENT ---
@Composable
fun HeaderRow(
    currentProject: Project?,
    syncStatus: SyncStatus,
    onSyncClick: () -> Unit,
    onNewProjectClick: () -> Unit,
    onManageProjectsClick: () -> Unit,
    onDeleteProjectClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(BgCarbon)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sophisticated icon container: w-10 h-10 rounded-xl bg-[#D0BCFF] flex items-center justify-center text-[#381E72]
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryLavender),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome, // Beautiful auto creative icon
                contentDescription = "Studio Icon",
                tint = OnPrimaryIndigo,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currentProject?.name ?: "No Open Timeline",
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CloudDone,
                    contentDescription = "Cloud Status",
                    tint = TextWhite.copy(alpha = 0.5f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val syncText = when (syncStatus) {
                    is SyncStatus.Idle -> if (currentProject?.isSynced == true) "Synced to Cloud" else "Draft (Local)"
                    is SyncStatus.Syncing -> "Saving Cloud Project (${(syncStatus.progress * 100).toInt()}%)"
                    is SyncStatus.Success -> "Cloud Sync Completed!"
                    is SyncStatus.Failed -> "Sync Failed"
                }
                Text(
                    text = syncText,
                    color = TextWhite.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }

        // --- ACTIONS PANEL ---
        // Drafts Button (reopens library)
        Button(
            onClick = onManageProjectsClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.05f),
                contentColor = TextWhite.copy(alpha = 0.85f)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text("Drafts", fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.width(6.dp))

        // EXPORT prominent action button (replaces sync_cloud_button, maps to sync logic)
        Button(
            onClick = onSyncClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryLavender,
                contentColor = OnPrimaryIndigo
            ),
            contentPadding = PaddingValues(horizontal = 12.dp),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .height(34.dp)
                .testTag("sync_cloud_button")
        ) {
            Text("EXPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Auxiliary actions
        IconButton(
            onClick = onNewProjectClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "New Layout Project",
                tint = TextWhite.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }

        if (currentProject != null) {
            IconButton(
                onClick = onDeleteProjectClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = "Purge Project",
                    tint = TextWhite.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// --- TAB NAVIGATION ROW --- (Styled exactly like: nav h-20 bg-[#211F26] px-4 flex items-center justify-around border-t border-white/5)
@Composable
fun TabNavigationRow(
    activeTab: Int,
    onTabSelect: (Int) -> Unit
) {
    val labels = listOf("Timeline", "AI Color Grade", "AI Upscaler", "Library")
    val icons = listOf(
        Icons.Rounded.Timeline,
        Icons.Rounded.ColorLens,
        Icons.Rounded.HighQuality,
        Icons.Rounded.FolderOpen
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f))
            .background(SurfaceNav)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        labels.forEachIndexed { idx, label ->
            val isActive = activeTab == idx
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelect(idx) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isActive) {
                        // High-tech capsule background pill: bg-[#4F378B] h-8 w-16
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 30.dp)
                                .clip(CircleShape)
                                .background(LightPurpleBrushColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icons[idx],
                                contentDescription = label,
                                tint = PrimaryLavender,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        // Inactive icon without container
                        Box(
                            modifier = Modifier.size(width = 56.dp, height = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icons[idx],
                                contentDescription = label,
                                tint = TextWhite.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = label,
                        color = if (isActive) PrimaryLavender else TextWhite.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// --- NORMAL CANVAS VIEWPORT ---
@Composable
fun NormalCanvasViewport(
    currentProject: Project?,
    elements: List<VisualElement>,
    selectedElementId: Int?,
    isPlaying: Boolean,
    currentTimeMs: Long,
    viewModel: EditorViewModel
) {
    if (currentProject == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Create or select a project on top to open workspace.", color = Color.Gray, textAlign = TextAlign.Center)
        }
        return
    }

    // Capture dynamic scale shifts
    val infiniteTransition = rememberInfiniteTransition(label = "canvasTicker")
    val gridScanOffset by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gridTicker"
    )

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
        ) {
            val viewWidth = constraints.maxWidth.toFloat()
            val viewHeight = constraints.maxHeight.toFloat()
            val centerX = viewWidth / 2f
            val centerY = viewHeight / 2f
 
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 1. Draw solid Surface Slate backdrop
                drawRect(color = SurfaceSlate)
                
                // 2. Draw atmospheric deep purple radial glow (from spec: bg-[radial-gradient(circle_at_center,_#4F378B_0%,_transparent_70%)] on 40% opacity)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(LightPurpleBrushColor.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = viewWidth * 0.72f
                    )
                )

                // 2. Draw cinematic frame guidelines
                drawRect(
                    color = Color.White.copy(alpha = 0.04f),
                    topLeft = Offset(20f, 20f),
                    size = Size(viewWidth - 40f, viewHeight - 40f),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                )

                // Draw central focus lines
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(centerX - 30f, centerY),
                    end = Offset(centerX + 30f, centerY),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(centerX, centerY - 30f),
                    end = Offset(centerX, centerY + 30f),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 3. Render animated elements with interpolated keyframes
            elements.forEach { elem ->
                val state = viewModel.getInterpolatedStateForElement(elem.id)
                if (state.isVisible) {
                    val isSelected = elem.id == selectedElementId
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (centerX + state.posX - 75.dp.toPx() / 2).roundToInt(),
                                    (centerY + state.posY - 75.dp.toPx() / 2).roundToInt()
                                )
                            }
                            .size(75.dp)
                            .graphicsLayer {
                                scaleX = state.scale
                                scaleY = state.scale
                                rotationZ = state.rotation
                                alpha = state.opacity
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) AccentCyan else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { viewModel.selectElement(elem.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                            when (elem.type) {
                                "GRID" -> {
                                    // Render Cyberpunk scanning grids
                                    val col = Color(android.graphics.Color.parseColor(elem.colorHex))
                                    for (i in 0..4) {
                                        val lineOffset = (size.width / 4) * i
                                        drawLine(
                                            color = col.copy(alpha = 0.4f),
                                            start = Offset(lineOffset, 0f),
                                            end = Offset(lineOffset, size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                        drawLine(
                                            color = col.copy(alpha = 0.4f),
                                            start = Offset(0f, lineOffset),
                                            end = Offset(size.width, lineOffset),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                    // Scan line
                                    val scanY = size.height / 2 + gridScanOffset
                                    drawLine(
                                        color = col,
                                        start = Offset(0f, scanY),
                                        end = Offset(size.width, scanY),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                                "FLARE" -> {
                                    // Glowing star lens flare
                                    val col = Color(android.graphics.Color.parseColor(elem.colorHex))
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(col, Color.Transparent),
                                            radius = size.width / 2
                                        )
                                    )
                                    // Lateral streak lines
                                    drawLine(
                                        color = col,
                                        start = Offset(0f, size.height / 2),
                                        end = Offset(size.width, size.height / 2),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                    drawLine(
                                        color = col,
                                        start = Offset(size.width / 2, 0f),
                                        end = Offset(size.width / 2, size.height),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                }
                                "GLITCH" -> {
                                    // Holographic glitch shapes
                                    val col = Color(android.graphics.Color.parseColor(elem.colorHex))
                                    drawRect(
                                        color = col.copy(alpha = 0.5f),
                                        topLeft = Offset(10f, 15f),
                                        size = Size(20f, size.height - 30f)
                                    )
                                    drawRect(
                                        color = AccentMagenta.copy(alpha = 0.6f),
                                        topLeft = Offset(size.width - 30f, 10f),
                                        size = Size(15f, size.height - 20f)
                                    )
                                    // Tiny speckles in glitch
                                    drawCircle(col, radius = 5f, center = Offset(size.width / 3, size.height / 3))
                                    drawCircle(AccentMagenta, radius = 4f, center = Offset(2 * size.width / 3, 2 * size.height / 3))
                                }
                                "PARTICLE" -> {
                                    // Concentric glowing rings
                                    val col = Color(android.graphics.Color.parseColor(elem.colorHex))
                                    for (r in 1..3) {
                                        drawCircle(
                                            color = col.copy(alpha = 0.25f * (4 - r)),
                                            radius = (size.width / 6) * r,
                                            style = Stroke(width = 1.5.dp.toPx())
                                        )
                                    }
                                }
                                "TEXT" -> {
                                    // Canvas text representation fallback
                                }
                            }
                        }

                        // Text overlay fallback
                        if (elem.type == "TEXT") {
                            Text(
                                text = elem.contentText.ifBlank { "TEXT BOX" },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(2.dp)
                            )
                        } else {
                            Text(
                                text = when (elem.type) {
                                    "GRID" -> "GRID"
                                    "FLARE" -> "FLARE"
                                    "GLITCH" -> "GLITCH"
                                    else -> "DUST"
                                },
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }

            // 4. Draw Overlay Color Grading Filters (LUTS effects simulated)
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {}) {
                // Apply Brightness/Contrast factor using full canvas alpha tints
                if (currentProject.brightness != 1.0f) {
                    val brightColor = if (currentProject.brightness > 1.0f) Color.White else Color.Black
                    val amt = Math.abs(currentProject.brightness - 1.0f).coerceIn(0f, 0.4f)
                    drawRect(color = brightColor.copy(alpha = amt))
                }

                // Apply Temperature Tint
                if (currentProject.temperature != 0.0f) {
                    val tintColor = if (currentProject.temperature > 0.0f) Color(0xFFFF9800) else Color(0xFF00E5FF)
                    val amt = Math.abs(currentProject.temperature).coerceIn(0f, 0.35f)
                    drawRect(color = tintColor.copy(alpha = amt))
                }

                // Apply Vignette dark borders
                if (currentProject.vignette > 0.0f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = currentProject.vignette)),
                            radius = viewWidth * 0.82f,
                            center = Offset(centerX, centerY)
                        )
                    )
                }
            }

            // Top Info Tags (Style matching: px-2 py-1 rounded bg-black/60 backdrop-blur-md border border-white/10)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left dynamic grading filter tag
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = "Grade Color",
                        tint = AccentCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "LUT: ${currentProject.lutFilterName.uppercase()}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Right digital time index tag
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF00FF88), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${"%.2f".format(currentTimeMs / 1000f)} / 10.00s",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// --- INTERACTIVE SPLIT UPSCALE VIEWPORT ---
@Composable
fun SplitUpscaleViewport(
    currentProject: Project?,
    elements: List<VisualElement>,
    selectedElementId: Int?,
    isPlaying: Boolean,
    currentTimeMs: Long,
    viewModel: EditorViewModel
) {
    if (currentProject == null) return

    var splitXState by remember { mutableStateOf(0.5f) } // 0.0 to 1.0 (from start of screen to end)

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = BorderStroke(1.5.dp, AccentAmber.copy(alpha = 0.5f))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = dragAmount.x / size.width
                        splitXState = (splitXState + delta).coerceIn(0.01f, 0.99f)
                    }
                }
        ) {
            val viewWidth = constraints.maxWidth.toFloat()
            val viewHeight = constraints.maxHeight.toFloat()
            val centerX = viewWidth / 2f
            val centerY = viewHeight / 2f
            val splitDividerX = viewWidth * splitXState

            // Draw full background elements
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background grid lines (low dynamic opacity)
                drawRect(Color(0xFF0A0A0E))
            }

            // LEFT SIDE: LOW-RESOLUTION CHANNEL (Simulated Pixelated/Bilinear-blur)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = (viewWidth - splitDividerX).dp / LocalDensity.current.density)
                    .clip(RoundedCornerShape(0.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw blurred/pixelated representation of elements
                    drawRect(Color(0xFF131317))
                    // Low res concentric grid circle
                    drawCircle(
                        color = Color.DarkGray,
                        radius = size.width / 3.5f,
                        style = Stroke(width = 8.dp.toPx()) // super thick
                    )
                    
                    // Draw heavily blurred low res focus cross
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(centerX - 10f, centerY),
                        end = Offset(centerX + 10f, centerY),
                        strokeWidth = 5.dp.toPx()
                    )
                }
                
                // Draw blurred text overlay indicator
                Box(modifier = Modifier.align(Alignment.Center)) {
                    Text(
                        text = "LOW-RES FEED\n(720p Interp)",
                        color = Color.LightGray.copy(alpha = 0.3f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // RIGHT SIDE: AI UPSCALE CHANNEL (Simulated crispy edges, glowing nodes, texturizers)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(splitDividerX.roundToInt(), 0) }
                    .clip(RoundedCornerShape(0.dp))
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(-splitDividerX.roundToInt(), 0) }
                        .size(viewWidth.dp / LocalDensity.current.density, viewHeight.dp / LocalDensity.current.density)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(Color(0xFF0F0F14))
                        // Crispy thin circles
                        drawCircle(
                            color = AccentCyan.copy(alpha = 0.4f),
                            radius = size.width / 3.5f,
                            style = Stroke(width = 1.dp.toPx()) // ultra sharp
                        )
                        // Tiny digital code particles
                        drawCircle(
                            color = AccentAmber,
                            radius = 3.dp.toPx(),
                            center = Offset(centerX + 40f, centerY - 60f)
                        )
                        drawCircle(
                            color = AccentCyan,
                            radius = 2.dp.toPx(),
                            center = Offset(centerX + 100f, centerY + 20f)
                        )

                        // Draw extremely thin focus lines
                        drawLine(
                            color = AccentCyan,
                            start = Offset(centerX - 40f, centerY),
                            end = Offset(centerX + 40f, centerY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        Text(
                            text = "AI CLARIFIED\n(4K Ultra HD)",
                            color = AccentCyan.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // DRAW INTERACTIVE DIVISION LINE SPLITTER
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = AccentAmber,
                    start = Offset(splitDividerX, 0f),
                    end = Offset(splitDividerX, viewHeight),
                    strokeWidth = 2.dp.toPx()
                )
                // Draw slider diamond handle
                drawCircle(
                    color = AccentAmber,
                    radius = 8.dp.toPx(),
                    center = Offset(splitDividerX, centerY)
                )
                drawCircle(
                    color = Color.Black,
                    radius = 4.dp.toPx(),
                    center = Offset(splitDividerX, centerY)
                )
            }

            // Split Viewport Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Low-Res Input", color = Color.White, fontSize = 8.sp)
                }

                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("AI Super-Resolution Pro", color = AccentAmber, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- TAB MODULE 0: TIMELINE & KEYFRAME CONTROLS ---
@Composable
fun TimelineTabContent(
    viewModel: EditorViewModel,
    currentProject: Project?,
    elements: List<VisualElement>,
    keyframes: List<KeyFrame>,
    currentTimeMs: Long,
    selectedElementId: Int?,
    isPlaying: Boolean,
    onAddElementClick: () -> Unit
) {
    if (currentProject == null) return
    val selectedElement = elements.find { it.id == selectedElementId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        // PLAYBACK AND BASIC ACTION ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.togglePlay() },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                    .size(40.dp)
                    .testTag("toggle_play_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Toggle play ticker",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onAddElementClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1B25)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("add_element_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Insert Element track", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Object", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (selectedElement != null) {
                Button(
                    onClick = { viewModel.deleteCurrentElement() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B1F24)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = "Purge Track Object", modifier = Modifier.size(16.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Track", fontSize = 11.sp, color = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // KEYFRAME DIAMOND EDIT TRIGGERS
            if (selectedElement != null) {
                val state = viewModel.getInterpolatedStateForElement(selectedElement.id)
                Button(
                    onClick = {
                        viewModel.createOrUpdateKeyframeForSelected(
                            scale = state.scale,
                            rotation = state.rotation,
                            opacity = state.opacity,
                            posX = state.posX,
                            posY = state.posY
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("add_keyframe_button")
                ) {
                    Icon(Icons.Rounded.Stars, contentDescription = "Apply Diamond Keyframe", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("◆ KF Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { viewModel.deleteKeyframeAtCurrent() },
                    modifier = Modifier
                        .background(Color(0xFF20171A), RoundedCornerShape(8.dp))
                        .size(38.dp)
                ) {
                    Icon(Icons.Rounded.StarHalf, contentDescription = "Erase Keyframe Marker", tint = AccentMagenta)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // DUAL COMPARTMENTS: (Left Track list & Right Track Property sliders)
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // LEFT COMPARTMENT: The scrolling timeline tracks with keyframe dots!
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .background(Color(0xFF07070B), RoundedCornerShape(6.dp))
                    .border(BorderStroke(1.dp, Color(0xFF1A1A22)), RoundedCornerShape(6.dp))
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        // Timeline Scrubber bar representation
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .background(Color(0xFF14141E))
                                .padding(horizontal = 4.dp)
                        ) {
                            Text("Time ruler (10s)", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    items(elements) { elem ->
                        val isChosen = elem.id == selectedElementId
                        val elemKfs = keyframes.filter { it.elementId == elem.id }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isChosen) Color(0xFF1A1A2C) else Color(0xFF111117))
                                .clickable { viewModel.selectElement(elem.id) }
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (elem.type) {
                                    "GRID" -> Icons.Rounded.GridOn
                                    "FLARE" -> Icons.Rounded.FlashOn
                                    "GLITCH" -> Icons.Rounded.Texture
                                    "PARTICLE" -> Icons.Rounded.BubbleChart
                                    else -> Icons.Rounded.TextFields
                                },
                                contentDescription = elem.name,
                                tint = Color(android.graphics.Color.parseColor(elem.colorHex)),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = elem.name,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                // Render small line representing duration, with keyframe markers
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .drawBehind {
                                            // Draw track range line
                                            val startRatio = elem.startMs.toFloat() / 10000f
                                            val durationRatio = elem.durationMs.toFloat() / 10000f
                                            drawRect(
                                                color = Color(android.graphics.Color.parseColor(elem.colorHex)).copy(alpha = 0.2f),
                                                topLeft = Offset(size.width * startRatio, 2f),
                                                size = Size(size.width * durationRatio, size.height - 4f)
                                            )
                                            // Draw little circles at keyframes offset ratios
                                            elemKfs.forEach { kf ->
                                                val kfTimeAbsolute = elem.startMs + kf.timeMs
                                                val kfRatio = kfTimeAbsolute.toFloat() / 10000f
                                                drawCircle(
                                                    color = AccentCyan,
                                                    radius = 3.dp.toPx(),
                                                    center = Offset(size.width * kfRatio, size.height / 2f)
                                                )
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // RIGHT COMPARTMENT: Keyframe property modifiers sliders!
            if (selectedElement != null) {
                val state = viewModel.getInterpolatedStateForElement(selectedElement.id)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF12121A), RoundedCornerShape(6.dp))
                        .padding(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("◆ Active KF Metrics", color = AccentMagenta, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        
                        // Property: Scale
                        Text("Scale: ${"%.2f".format(state.scale)}x", color = Color.White, fontSize = 9.sp)
                        Slider(
                            value = state.scale,
                            onValueChange = { newVal ->
                                viewModel.createOrUpdateKeyframeForSelected(
                                    scale = newVal,
                                    rotation = state.rotation,
                                    opacity = state.opacity,
                                    posX = state.posX,
                                    posY = state.posY
                                )
                            },
                            valueRange = 0.2f..2.5f,
                            modifier = Modifier.height(18.dp)
                        )

                        // Property: Rotation
                        Text("Rotation: ${state.rotation.toInt()}°", color = Color.White, fontSize = 9.sp)
                        Slider(
                            value = state.rotation,
                            onValueChange = { newVal ->
                                viewModel.createOrUpdateKeyframeForSelected(
                                    scale = state.scale,
                                    rotation = newVal,
                                    opacity = state.opacity,
                                    posX = state.posX,
                                    posY = state.posY
                                )
                            },
                            valueRange = -180f..180f,
                            modifier = Modifier.height(18.dp)
                        )

                        // Property: Opacity
                        Text("Opacity: ${(state.opacity * 100).toInt()}%", color = Color.White, fontSize = 9.sp)
                        Slider(
                            value = state.opacity,
                            onValueChange = { newVal ->
                                viewModel.createOrUpdateKeyframeForSelected(
                                    scale = state.scale,
                                    rotation = state.rotation,
                                    opacity = newVal,
                                    posX = state.posX,
                                    posY = state.posY
                                )
                            },
                            valueRange = 0f..1.0f,
                            modifier = Modifier.height(18.dp)
                        )

                        // Position X Offset
                        Text("Offset X: ${state.posX.toInt()} dp", color = Color.White, fontSize = 9.sp)
                        Slider(
                            value = state.posX,
                            onValueChange = { newVal ->
                                viewModel.createOrUpdateKeyframeForSelected(
                                    scale = state.scale,
                                    rotation = state.rotation,
                                    opacity = state.opacity,
                                    posX = newVal,
                                    posY = state.posY
                                )
                            },
                            valueRange = -150f..150f,
                            modifier = Modifier.height(18.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select a track on the left to activate parameters.", color = Color.DarkGray, fontSize = 9.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// --- TAB MODULE 1: AI COHESIVE COLOR GRADING ---
@Composable
fun ColorGradingTabContent(
    viewModel: EditorViewModel,
    currentProject: Project?,
    isGrading: Boolean,
    gradingStatus: String?
) {
    if (currentProject == null) return
    var gradingPrompt by remember { mutableStateOf("") }
    val presets = listOf("Cinema Teal", "Neon Cyber", "Retro Gold", "Moody Noir", "Classic Sepia", "Normal")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // AI PROMPT AREA
        Text("AI Automated Grading (Gemini-Powered)", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = gradingPrompt,
                onValueChange = { gradingPrompt = it },
                placeholder = { Text("e.g. vintage 80s film color, moody forest cyan shadows") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("ai_grade_field")
            )

            Spacer(modifier = Modifier.width(6.dp))

            Button(
                onClick = {
                    viewModel.applyAICohesiveColorGrading(gradingPrompt)
                },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("submit_ai_grade_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isGrading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, "Auto Grade look", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Grade Look", fontSize = 10.sp)
                    }
                }
            }
        }

        if (gradingStatus != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF20202F), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Info, "AI Response details", tint = AccentCyan, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(gradingStatus, color = Color.White, fontSize = 9.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.dismissGradingStatus() }, modifier = Modifier.size(16.dp)) {
                    Icon(Icons.Rounded.Close, "Dismiss AI Status", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // DIRECT GRADERS MANUAL OVERRIDES
        Text("Manual Grade Overrides", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preset column list
            ElevatedCard(
                modifier = Modifier
                    .weight(1.2f)
                    .height(130.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141B))
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Text("Select Film Presets", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(presets) { filter ->
                            val isCurrent = currentProject.lutFilterName == filter
                            Text(
                                text = "■ $filter",
                                color = if (isCurrent) AccentCyan else Color.White,
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.applyLUTFilter(filter) }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Sliders column lists
            ElevatedCard(
                modifier = Modifier
                    .weight(1.8f)
                    .height(130.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Brightness slider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Brightness", color = Color.White, fontSize = 9.sp)
                        Text("${"%.2f".format(currentProject.brightness)}", color = Color.Gray, fontSize = 9.sp)
                    }
                    Slider(
                        value = currentProject.brightness,
                        onValueChange = {
                            viewModel.updateManualColorOffsets(it, currentProject.contrast, currentProject.saturation, currentProject.temperature, currentProject.vignette)
                        },
                        valueRange = 0.5f..1.5f,
                        modifier = Modifier.height(14.dp)
                    )

                    // Contrast Slider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Contrast", color = Color.White, fontSize = 9.sp)
                        Text("${"%.2f".format(currentProject.contrast)}", color = Color.Gray, fontSize = 9.sp)
                    }
                    Slider(
                        value = currentProject.contrast,
                        onValueChange = {
                            viewModel.updateManualColorOffsets(currentProject.brightness, it, currentProject.saturation, currentProject.temperature, currentProject.vignette)
                        },
                        valueRange = 0.5f..1.5f,
                        modifier = Modifier.height(14.dp)
                    )

                    // Saturation Slider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Saturation", color = Color.White, fontSize = 9.sp)
                        Text("${"%.2f".format(currentProject.saturation)}", color = Color.Gray, fontSize = 9.sp)
                    }
                    Slider(
                        value = currentProject.saturation,
                        onValueChange = {
                            viewModel.updateManualColorOffsets(currentProject.brightness, currentProject.contrast, it, currentProject.temperature, currentProject.vignette)
                        },
                        valueRange = 0.0f..2.0f,
                        modifier = Modifier.height(14.dp)
                    )

                    // Temperature Slider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Warmth/Blue", color = Color.White, fontSize = 9.sp)
                        Text("${"%.2f".format(currentProject.temperature)}", color = Color.Gray, fontSize = 9.sp)
                    }
                    Slider(
                        value = currentProject.temperature,
                        onValueChange = {
                            viewModel.updateManualColorOffsets(currentProject.brightness, currentProject.contrast, currentProject.saturation, it, currentProject.vignette)
                        },
                        valueRange = -1.0f..1.0f,
                        modifier = Modifier.height(14.dp)
                    )

                    // Vignette Slider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Vignette Shadow", color = Color.White, fontSize = 9.sp)
                        Text("${"%.2f".format(currentProject.vignette)}", color = Color.Gray, fontSize = 9.sp)
                    }
                    Slider(
                        value = currentProject.vignette,
                        onValueChange = {
                            viewModel.updateManualColorOffsets(currentProject.brightness, currentProject.contrast, currentProject.saturation, currentProject.temperature, it)
                        },
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier.height(14.dp)
                    )
                }
            }
        }
    }
}

// --- TAB MODULE 2: AI IMAGE CLARIFYING UPSCALER ---
@Composable
fun UpscalerTabContent(
    viewModel: EditorViewModel,
    currentProject: Project?,
    isUpscaling: Boolean,
    upscaleStatus: String?,
    elements: List<VisualElement>,
    selectedElementId: Int?
) {
    if (currentProject == null) return
    val activeElem = elements.find { it.id == selectedElementId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("AI Super-Resolution Upscaling Dash", color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Reconstruct lossy edge frequencies of low-resolution footage into sharp detail lines.",
            color = Color.Gray,
            fontSize = 9.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Actions console
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.triggerAIResolutionUpscaling() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("upscale_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
            ) {
                if (isUpscaling) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Decompressing...", color = Color.Black, fontSize = 10.sp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.HighQuality, "AI Upscaler active", tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ENGAGE CLARITY UPSCALING ✨", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            OutlinedButton(
                onClick = { viewModel.setUpscaleMode("None") },
                modifier = Modifier.height(48.dp)
            ) {
                Text("Bypass", color = Color.LightGray, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // AI details console output
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .border(BorderStroke(1.dp, Color(0xFF282835)), RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14141B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SYSTEM UPSCALE LOGS", color = AccentAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "MODE: ${currentProject.upscaleMode}",
                        color = if (currentProject.upscaleMode == "None") Color.Gray else AccentCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                if (upscaleStatus != null) {
                    Text(
                        text = upscaleStatus,
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 12.sp
                    )
                } else {
                    Text(
                        text = "Active Layer: ${activeElem?.name ?: "Full Screen Master Video Track"}\nInput resolution: SD (720x480 pixels)\nPredicted scale: 3840x2160 (4K UHD lossless)\n\nClick the button above to execute lossy edge synthesis. Use the split slide tool above to compare edge grids in real-time.",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

// --- TAB MODULE 3: PROJECT LIBRARY & SAVE SLATE ---
@Composable
fun ProjectLibraryTabContent(
    projects: List<Project>,
    currentProjectId: Int?,
    onSelectProject: (Int) -> Unit,
    onNewClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Edit Timeline library", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = onNewClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(32.dp)
            ) {
                Text("New Project", fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (projects.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No projects saved locally yet.", color = Color.DarkGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(projects) { project ->
                    val isCurrent = project.id == currentProjectId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCurrent) Color(0xFF1B1B2C) else Color(0xFF13131B))
                            .border(
                                width = if (isCurrent) 1.5.dp else 0.dp,
                                color = if (isCurrent) AccentCyan else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelectProject(project.id) }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Movie,
                            contentDescription = "Project logo",
                            tint = if (isCurrent) AccentCyan else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(project.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("LUT: ${project.lutFilterName} | Scale: ${if (project.upscaleMode == "None") "Standard" else "AI 4K"}", color = Color.Gray, fontSize = 9.sp)
                        }
                        
                        if (project.isSynced) {
                            Icon(Icons.Rounded.CloudDone, "Cloud Ready Status", tint = Color(0xFF00FF88), modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Rounded.CloudOff, "Offline Slate Project", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
