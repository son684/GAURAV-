package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// --- entities ---

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val thumbnailSeed: Int = (1..1000).random(),
    val durationMs: Long = 10000L,
    
    // Color Grading settings
    val brightness: Float = 1.0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val temperature: Float = 0.0f, // -1.0 to 1.0 (cooling to warming)
    val vignette: Float = 0.0f,     // 0.0 to 1.0
    val lutFilterName: String = "Normal", // Preset or Gemini custom name
    
    // Upscaling settings
    val upscaleMode: String = "None" // "None", "Bicubic Detailer", "AI Hyper-Clarity"
)

@Entity(tableName = "visual_elements")
data class VisualElement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val name: String,
    val type: String, // "GRID", "FLARE", "GLITCH", "TEXT", "PARTICLE"
    val contentText: String = "", // custom label or text if type == "TEXT"
    val colorHex: String = "#00FFFF", // default cyan or neon color
    val startMs: Long = 1000L,
    val durationMs: Long = 4000L
)

@Entity(tableName = "keyframes")
data class KeyFrame(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val elementId: Int,
    val timeMs: Long, // relative playhead offset
    val scale: Float = 1.0f,
    val rotation: Float = 0.0f, // degrees
    val opacity: Float = 1.0f,
    val posX: Float = 0.0f, // relative X offset in pixels/dp
    val posY: Float = 0.0f  // relative Y offset in pixels/dp
)

// --- dao ---

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastUpdated DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Int): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)

    // Visual Elements queries
    @Query("SELECT * FROM visual_elements WHERE projectId = :projectId ORDER BY startMs ASC")
    fun getElementsForProject(projectId: Int): Flow<List<VisualElement>>

    @Query("SELECT * FROM visual_elements WHERE projectId = :projectId ORDER BY startMs ASC")
    suspend fun getElementsForProjectSync(projectId: Int): List<VisualElement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElement(element: VisualElement): Long

    @Query("DELETE FROM visual_elements WHERE id = :id")
    suspend fun deleteElementById(id: Int)

    // Keyframes queries
    @Query("SELECT * FROM keyframes WHERE elementId IN (SELECT id FROM visual_elements WHERE projectId = :projectId) ORDER BY timeMs ASC")
    fun getKeyframesForProject(projectId: Int): Flow<List<KeyFrame>>

    @Query("SELECT * FROM keyframes WHERE elementId = :elementId ORDER BY timeMs ASC")
    fun getKeyframesForElement(elementId: Int): Flow<List<KeyFrame>>

    @Query("SELECT * FROM keyframes WHERE elementId = :elementId ORDER BY timeMs ASC")
    suspend fun getKeyframesForElementSync(elementId: Int): List<KeyFrame>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyframe(keyframe: KeyFrame): Long

    @Query("DELETE FROM keyframes WHERE id = :id")
    suspend fun deleteKeyframeById(id: Int)

    @Query("DELETE FROM keyframes WHERE elementId = :elementId")
    suspend fun deleteKeyframesForElement(elementId: Int)
}

// --- database ---

@Database(entities = [Project::class, VisualElement::class, KeyFrame::class], version = 1, exportSchema = false)
abstract class ProjectDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: ProjectDatabase? = null

        fun getDatabase(context: Context): ProjectDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ProjectDatabase::class.java,
                    "project_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
