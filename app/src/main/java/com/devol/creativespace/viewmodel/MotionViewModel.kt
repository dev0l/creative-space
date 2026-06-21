package com.devol.creativespace.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MotionViewModel : ViewModel() {
    private val _completedStrokes = MutableStateFlow<List<List<Offset>>>(emptyList())
    val completedStrokes: StateFlow<List<List<Offset>>> = _completedStrokes.asStateFlow()

    private val _currentStroke = MutableStateFlow<List<Offset>>(emptyList())
    val currentStroke: StateFlow<List<Offset>> = _currentStroke.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // Settings
    private val _sensitivity = MutableStateFlow(1.0f)
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    private val _lineThickness = MutableStateFlow(8f)
    val lineThickness: StateFlow<Float> = _lineThickness.asStateFlow()

    private val _lineColor = MutableStateFlow(Color(0xFF333333))
    val lineColor: StateFlow<Color> = _lineColor.asStateFlow()

    private val _invertDrawing = MutableStateFlow(false)
    val invertDrawing: StateFlow<Boolean> = _invertDrawing.asStateFlow()

    // Gallery (device images for canvas backgrounds)
    private val _savedImages = MutableStateFlow<List<File>>(emptyList())
    val savedImages: StateFlow<List<File>> = _savedImages.asStateFlow()

    // Space-scoped collection (what you've made/imported in a specific space)
    private val _collection = MutableStateFlow<List<File>>(emptyList())
    val collection: StateFlow<List<File>> = _collection.asStateFlow()

    // Ember bundles (cross-space curated journeys)
    private val _emberBundles = MutableStateFlow<List<EmberBundle>>(emptyList())
    val emberBundles: StateFlow<List<EmberBundle>> = _emberBundles.asStateFlow()

    // Background image — loaded from Gallery to draw on top of
    private val _backgroundImage = MutableStateFlow<File?>(null)
    val backgroundImage: StateFlow<File?> = _backgroundImage.asStateFlow()

    /**
     * Loads a gallery image as the canvas background.
     * Starts a fresh drawing session (clears existing strokes) on top of the chosen image.
     */
    fun setBackgroundImage(file: File) {
        _completedStrokes.value = emptyList()
        _currentStroke.value = emptyList()
        isInitialized = false
        _backgroundImage.value = file
    }


    fun setSensitivity(value: Float) { _sensitivity.value = value }
    fun setLineThickness(value: Float) { _lineThickness.value = value }
    fun setLineColor(color: Color) { _lineColor.value = color }
    fun setInvertDrawing(invert: Boolean) { _invertDrawing.value = invert }

    private var cursorX = 0f
    private var cursorY = 0f
    private var isInitialized = false

    var canvasWidth = 1080f
    var canvasHeight = 1920f

    fun setCanvasSize(width: Float, height: Float) {
        canvasWidth = width
        canvasHeight = height
    }

    fun toggleTracking() {
        if (_isTracking.value) {
            if (_currentStroke.value.isNotEmpty()) {
                        _completedStrokes.value += listOf(_currentStroke.value)
                _currentStroke.value = emptyList()
            }
            _isTracking.value = false
        } else {
            _isTracking.value = true
        }
    }

    fun clearCanvas() {
        _completedStrokes.value = emptyList()
        _currentStroke.value = emptyList()
        _backgroundImage.value = null
        isInitialized = false
    }

    fun addDeltas(deltaX: Float, deltaY: Float) {
        if (!_isTracking.value) return

        if (!isInitialized) {
            cursorX = canvasWidth / 2f
            cursorY = canvasHeight / 2f
            isInitialized = true
        }
        
        cursorX = (cursorX + deltaX).coerceIn(0f, canvasWidth)
        cursorY = (cursorY + deltaY).coerceIn(0f, canvasHeight)
        
        _currentStroke.value += Offset(cursorX, cursorY)
    }

    // =========================================================================
    // CANVAS → COLLECTION (Image space)
    // =========================================================================

    /**
     * Renders all strokes to a bitmap and saves as PNG to the image collection.
     * Returns true if save was successful.
     */
    fun saveCanvas(context: Context): Boolean {
        val allStrokes = _completedStrokes.value +
            (if (_currentStroke.value.isNotEmpty()) listOf(_currentStroke.value) else emptyList())

        if (allStrokes.isEmpty() && _backgroundImage.value == null) return false

        val width = canvasWidth.toInt().coerceAtLeast(1)
        val height = canvasHeight.toInt().coerceAtLeast(1)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        // Draw background — either loaded image or default canvas color
        val bgFile = _backgroundImage.value
        if (bgFile != null) {
            try {
                val bgBitmap = BitmapFactory.decodeFile(bgFile.absolutePath)
                if (bgBitmap != null) {
                    val scaled = Bitmap.createScaledBitmap(bgBitmap, width, height, true)
                    canvas.drawBitmap(scaled, 0f, 0f, null)
                    if (scaled !== bgBitmap) scaled.recycle()
                    bgBitmap.recycle()
                }
            } catch (_: Exception) {
                canvas.drawColor("#FCFBF4".toColorInt())
            }
        } else {
            canvas.drawColor("#FCFBF4".toColorInt())
        }

        val paint = Paint().apply {
            color = _lineColor.value.toArgb()
            strokeWidth = _lineThickness.value
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        for (stroke in allStrokes) {
            if (stroke.size < 2) continue
            val path = android.graphics.Path()
            path.moveTo(stroke.first().x, stroke.first().y)
            for (i in 1 until stroke.size) {
                path.lineTo(stroke[i].x, stroke[i].y)
            }
            canvas.drawPath(path, paint)
        }

        return try {
            val dir = getCollectionDir(context, "Image")
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "canvas_$timestamp.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            loadCollection(context, "Image")
            true
        } catch (_: Exception) {
            false
        }
    }

    // =========================================================================
    // SPACE-SCOPED COLLECTIONS
    // =========================================================================

    /**
     * Loads media files for a specific space's collection.
     * On first load, migrates any legacy files from embers/ to collections/image/.
     */
    fun loadCollection(context: Context, spaceType: String) {
        migrateIfNeeded(context)
        val dir = getCollectionDir(context, spaceType)
        val extensions = when (spaceType) {
            "Image" -> listOf("png", "jpg", "jpeg", "webp")
            "Audio" -> listOf("mp3", "wav", "ogg", "m4a", "aac", "flac")
            "Video" -> listOf("mp4", "mkv", "webm", "mov")
            else -> listOf("png", "jpg", "jpeg")
        }
        _collection.value = dir.listFiles()
            ?.filter { it.extension.lowercase() in extensions }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun deleteFromCollection(context: Context, file: File, spaceType: String) {
        file.delete()
        loadCollection(context, spaceType)
    }

    /**
     * Imports a file from a content URI into a space's collection.
     * Copies the stream to a timestamped file.
     */
    fun importToCollection(context: Context, uri: Uri, spaceType: String) {
        try {
            val dir = getCollectionDir(context, spaceType)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val ext = when (spaceType) {
                "Audio" -> "mp3"
                "Video" -> "mp4"
                else -> "png"
            }
            val file = File(dir, "import_${timestamp}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            loadCollection(context, spaceType)
        } catch (_: Exception) {
            // Import failed silently — no crash
        }
    }

    // =========================================================================
    // EMBER BUNDLES (cross-space curated journeys)
    // =========================================================================

    /**
     * Creates a new ember bundle with a name.
     * Returns the bundle directory.
     */
    fun createEmberBundle(context: Context, name: String): File? {
        return try {
            val dir = getEmberBundleDir(context)
            val sanitized = name.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim()
            val bundleDir = File(dir, sanitized.ifEmpty { "ember_${System.currentTimeMillis()}" })
            bundleDir.mkdirs()

            // Write manifest
            val manifest = JSONObject().apply {
                put("name", name)
                put("created", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()))
                put("items", JSONArray())
            }
            File(bundleDir, "manifest.json").writeText(manifest.toString(2))

            loadEmberBundles(context)
            bundleDir
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Adds a file from a collection into an ember bundle.
     * Copies the file (ember bundles own their files).
     */
    fun addToEmberBundle(context: Context, bundleDir: File, sourceFile: File, spaceType: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val dest = File(bundleDir, "${spaceType.lowercase()}_$timestamp.${sourceFile.extension}")
            sourceFile.copyTo(dest)

            // Update manifest
            val manifestFile = File(bundleDir, "manifest.json")
            if (manifestFile.exists()) {
                val manifest = JSONObject(manifestFile.readText())
                val items = manifest.getJSONArray("items")
                items.put(JSONObject().apply {
                    put("file", dest.name)
                    put("space", spaceType)
                    put("added", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()))
                })
                manifestFile.writeText(manifest.toString(2))
            }

            loadEmberBundles(context)
        } catch (_: Exception) {
            // Copy failed silently
        }
    }

    /**
     * Loads all ember bundles from the bundles directory.
     */
    fun loadEmberBundles(context: Context) {
        val dir = getEmberBundleDir(context)
        _emberBundles.value = dir.listFiles()
            ?.filter { it.isDirectory && File(it, "manifest.json").exists() }
            ?.map { bundleDir ->
                val manifest = try {
                    JSONObject(File(bundleDir, "manifest.json").readText())
                } catch (_: Exception) {
                    JSONObject()
                }
                val items = bundleDir.listFiles()
                    ?.filter { it.name != "manifest.json" }
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()
                EmberBundle(
                    name = manifest.optString("name", bundleDir.name),
                    directory = bundleDir,
                    items = items,
                    created = manifest.optString("created", ""),
                    thumbnail = items.firstOrNull { it.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp") }
                )
            }
            ?.sortedByDescending { it.directory.lastModified() }
            ?: emptyList()
    }

    fun deleteEmberBundle(context: Context, bundle: EmberBundle) {
        bundle.directory.deleteRecursively()
        loadEmberBundles(context)
    }

    /**
     * Removes a single item from an ember bundle.
     * Deletes the file and updates the manifest.
     */
    fun removeFromEmberBundle(context: Context, bundle: EmberBundle, file: File) {
        try {
            file.delete()
            // Update manifest — remove matching item entry
            val manifestFile = File(bundle.directory, "manifest.json")
            if (manifestFile.exists()) {
                val manifest = JSONObject(manifestFile.readText())
                val items = manifest.getJSONArray("items")
                val updatedItems = JSONArray()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    if (item.optString("file") != file.name) {
                        updatedItems.put(item)
                    }
                }
                manifest.put("items", updatedItems)
                manifestFile.writeText(manifest.toString(2))
            }
            loadEmberBundles(context)
        } catch (_: Exception) {
            // Removal failed silently
        }
    }

    /**
     * Creates a zip archive of all bundle contents.
     * Includes all media files and the manifest.
     * Returns the zip file in the cache directory, or null on failure.
     */
    fun createEmberZip(context: Context, bundle: EmberBundle): File? {
        return try {
            val sanitized = bundle.name.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim()
            val cacheSubdir = File(context.cacheDir, "creative_space_cache").also { it.mkdirs() }
            val zipFile = File(cacheSubdir, "${sanitized.ifEmpty { "ember" }}.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // Add manifest
                val manifestFile = File(bundle.directory, "manifest.json")
                if (manifestFile.exists()) {
                    zos.putNextEntry(ZipEntry("manifest.json"))
                    manifestFile.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
                // Add all items
                bundle.items.forEach { file ->
                    zos.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            zipFile
        } catch (_: Exception) {
            null
        }
    }

    // =========================================================================
    // LEGACY SUPPORT — kept for backward compatibility during migration
    // =========================================================================

    // These exist so ReEmberScreen and any old code can still call them.
    // They now redirect to the collection system.

    private val _embers = MutableStateFlow<List<File>>(emptyList())
    val embers: StateFlow<List<File>> = _embers.asStateFlow()

    fun loadEmbers(context: Context) {
        migrateIfNeeded(context)
        // Load ALL collections as a combined view for Re-Ember
        val imageDir = getCollectionDir(context, "Image")
        val audioDir = getCollectionDir(context, "Audio")
        val videoDir = getCollectionDir(context, "Video")
        val allFiles = listOf(imageDir, audioDir, videoDir).flatMap { dir ->
            dir.listFiles()?.toList() ?: emptyList()
        }.filter { it.isFile }
            .sortedByDescending { it.lastModified() }
        _embers.value = allFiles
    }

    fun deleteEmber(context: Context, file: File) {
        file.delete()
        loadEmbers(context)
    }

    fun importEmber(context: Context, uri: Uri) {
        importToCollection(context, uri, "Image")
    }

    // =========================================================================
    // GALLERY
    // =========================================================================

    fun loadSavedImages(context: Context) {
        val dir = getGalleryDir(context)
        _savedImages.value = dir.listFiles()
            ?.filter { it.extension == "png" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun deleteSavedImage(context: Context, file: File) {
        file.delete()
        loadSavedImages(context)
    }

    // =========================================================================
    // DIRECTORIES
    // =========================================================================

    private fun getGalleryDir(context: Context): File {
        val dir = File(context.filesDir, "gallery")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCollectionDir(context: Context, spaceType: String): File {
        val subdir = when (spaceType) {
            "Image" -> "image"
            "Audio" -> "audio"
            "Video" -> "video"
            else -> "image"
        }
        val dir = File(context.filesDir, "collections/$subdir")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getEmberBundleDir(context: Context): File {
        val dir = File(context.filesDir, "embers")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // =========================================================================
    // MIGRATION — moves legacy embers/ flat files to collections/image/
    // =========================================================================

    private var hasMigrated = false

    private fun migrateIfNeeded(context: Context) {
        if (hasMigrated) return
        hasMigrated = true

        val legacyDir = File(context.filesDir, "embers")
        if (!legacyDir.exists()) return

        val imageCollectionDir = getCollectionDir(context, "Image")
        legacyDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp") }
            ?.forEach { file ->
                val dest = File(imageCollectionDir, file.name)
                if (!dest.exists()) {
                    file.copyTo(dest)
                    file.delete()
                }
            }
    }
}

/**
 * Data class representing an ember bundle — a cross-space creative journey.
 */
data class EmberBundle(
    val name: String,
    val directory: File,
    val items: List<File>,
    val created: String,
    val thumbnail: File? = null
)
