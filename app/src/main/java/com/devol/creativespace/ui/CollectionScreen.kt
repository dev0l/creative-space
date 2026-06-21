package com.devol.creativespace.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.devol.creativespace.viewmodel.MotionViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Collection screen — space-scoped media library.
 *
 * Interaction model:
 *   Tap         → Fullscreen preview
 *   Long-press  → Enter selection mode / toggle selection
 *   Action bar  → Appears when items are selected (Share, Ember, Canvas, Delete)
 *   Back / ✕    → Exit selection mode
 *
 * Each space (Image, Audio, Video) has its own collection directory.
 */
@Composable
fun CollectionScreen(
    viewModel: MotionViewModel,
    spaceType: String = "Image",
    onNavigateHome: () -> Unit,
    onSendToCanvas: ((File) -> Unit)? = null
) {
    val context = LocalContext.current
    val collection by viewModel.collection.collectAsState()
    val emberBundles by viewModel.emberBundles.collectAsState()

    // =========================================================================
    // SELECTION STATE
    // =========================================================================
    var selectedItems by remember { mutableStateOf(setOf<String>()) } // Track by path for stability
    val isSelectionMode = selectedItems.isNotEmpty()
    var isDeleteArmed by remember { mutableStateOf(false) }

    // Fullscreen preview target
    var previewTarget by remember { mutableStateOf<File?>(null) }

    // Export to ember dialog state
    var showExportDialog by remember { mutableStateOf(false) }
    var showCreateEmber by remember { mutableStateOf(false) }
    var newEmberName by remember { mutableStateOf("") }

    // Clear delete armed state when selection changes
    LaunchedEffect(selectedItems) {
        isDeleteArmed = false
    }

    // Load collection for this space
    LaunchedEffect(spaceType) {
        viewModel.loadCollection(context, spaceType)
        viewModel.loadEmberBundles(context)
    }

    // Import launcher — filtered by space type
    val mimeType = when (spaceType) {
        "Audio" -> "audio/*"
        "Video" -> "video/*"
        else -> "image/*"
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importToCollection(context, it, spaceType)
        }
    }

    // Resolve selected paths back to files
    val selectedFiles = remember(selectedItems, collection) {
        collection.filter { it.absolutePath in selectedItems }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // =========================================================================
            // TOP BAR — adapts to selection mode
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    // Selection mode top bar
                    IconButton(onClick = { selectedItems = emptySet() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear selection",
                            tint = Color(0xFF00FFCC).copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        "${selectedItems.size} selected",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00FFCC).copy(alpha = 0.9f),
                        fontFamily = FontFamily.SansSerif
                    )

                    // Select all
                    TextButton(onClick = {
                        selectedItems = if (selectedItems.size == collection.size) {
                            emptySet()
                        } else {
                            collection.map { it.absolutePath }.toSet()
                        }
                    }) {
                        Text(
                            if (selectedItems.size == collection.size) "None" else "All",
                            color = Color(0xFF00FFCC).copy(alpha = 0.6f),
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // Normal top bar
                    IconButton(onClick = onNavigateHome) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00FFCC).copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        "$spaceType Collection",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF00FFCC).copy(alpha = 0.8f),
                        fontFamily = FontFamily.SansSerif
                    )

                    // Import button
                    IconButton(onClick = { importLauncher.launch(mimeType) }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Import $spaceType",
                            tint = Color(0xFF00FFCC).copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // =========================================================================
            // EXPORT TO EMBER DIALOG
            // =========================================================================
            if (showExportDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showExportDialog = false
                        showCreateEmber = false
                        newEmberName = ""
                    },
                    title = {
                        Text(
                            "Export to Ember",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF8800)
                        )
                    },
                    text = {
                        Column {
                            Text(
                                "${selectedFiles.size} item${if (selectedFiles.size != 1) "s" else ""} selected",
                                fontFamily = FontFamily.SansSerif,
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (emberBundles.isEmpty() && !showCreateEmber) {
                                Text(
                                    "No ember bundles yet. Create one to start your journey.",
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                            }

                            // List existing bundles
                            emberBundles.forEach { bundle ->
                                TextButton(
                                    onClick = {
                                        selectedFiles.forEach { file ->
                                            viewModel.addToEmberBundle(context, bundle.directory, file, spaceType)
                                        }
                                        showExportDialog = false
                                        selectedItems = emptySet()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            bundle.name,
                                            color = Color(0xFFFF8800).copy(alpha = 0.8f),
                                            fontFamily = FontFamily.SansSerif
                                        )
                                        Text(
                                            "${bundle.items.size} items",
                                            color = Color.White.copy(alpha = 0.3f),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Create new bundle
                            if (showCreateEmber) {
                                OutlinedTextField(
                                    value = newEmberName,
                                    onValueChange = { newEmberName = it },
                                    label = { Text("Ember name", color = Color(0xFFFF8800).copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFF8800),
                                        unfocusedBorderColor = Color(0xFFFF8800).copy(alpha = 0.3f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White.copy(alpha = 0.7f),
                                        cursorColor = Color(0xFFFF8800)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        if (newEmberName.isNotBlank()) {
                                            val bundleDir = viewModel.createEmberBundle(context, newEmberName)
                                            if (bundleDir != null) {
                                                selectedFiles.forEach { file ->
                                                    viewModel.addToEmberBundle(context, bundleDir, file, spaceType)
                                                }
                                            }
                                            showExportDialog = false
                                            showCreateEmber = false
                                            newEmberName = ""
                                            selectedItems = emptySet()
                                        }
                                    }
                                ) {
                                    Text(
                                        "Create & Add",
                                        color = Color(0xFFFF8800),
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                TextButton(onClick = { showCreateEmber = true }) {
                                    Text(
                                        "+ New Ember",
                                        color = Color(0xFFFF8800).copy(alpha = 0.6f),
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = {
                            showExportDialog = false
                            showCreateEmber = false
                            newEmberName = ""
                        }) {
                            Text(
                                "Cancel",
                                color = Color.White.copy(alpha = 0.5f),
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    },
                    containerColor = Color(0xFF1A1008)
                )
            }

            // =========================================================================
            // CONTENT
            // =========================================================================
            if (collection.isEmpty()) {
                // Empty state — space-aware messaging
                val (title, subtitle) = when (spaceType) {
                    "Image" -> "No images yet" to "Create something in the Canvas\nand press Keep, or import with +"
                    "Audio" -> "No audio yet" to "Import audio from your device\nwith the + button above"
                    "Video" -> "No videos yet" to "Import video from your device\nwith the + button above"
                    else -> "No items yet" to "Items will appear here"
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            title,
                            fontSize = 18.sp,
                            color = Color(0xFF00FFCC).copy(alpha = 0.4f),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Light
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            subtitle,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.25f),
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Item count
                Text(
                    "${collection.size} item${if (collection.size != 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = Color(0xFF00FFCC).copy(alpha = 0.3f),
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )

                // Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp,
                        // Extra bottom padding when action bar is showing
                        bottom = if (isSelectionMode) 80.dp else 12.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    items(collection) { file ->
                        val isSelected = file.absolutePath in selectedItems
                        CollectionCard(
                            file = file,
                            spaceType = spaceType,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onTap = {
                                if (isSelectionMode) {
                                    // In selection mode, tap toggles selection
                                    selectedItems = if (isSelected) {
                                        selectedItems - file.absolutePath
                                    } else {
                                        selectedItems + file.absolutePath
                                    }
                                } else {
                                    // Normal mode, tap opens preview
                                    previewTarget = file
                                }
                            },
                            onLongPress = {
                                // Long-press always toggles selection
                                selectedItems = if (isSelected) {
                                    selectedItems - file.absolutePath
                                } else {
                                    selectedItems + file.absolutePath
                                }
                            }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // ACTION BAR — slides up when items are selected
        // =========================================================================
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val barColor by animateColorAsState(
                targetValue = if (isDeleteArmed) Color(0xFF331111) else Color(0xFF1E1E1E),
                animationSpec = tween(300),
                label = "barColor"
            )
            val barBorderColor by animateColorAsState(
                targetValue = if (isDeleteArmed) Color(0xFFCC3333).copy(alpha = 0.5f)
                             else Color(0xFF00FFCC).copy(alpha = 0.15f),
                animationSpec = tween(300),
                label = "barBorderColor"
            )

            Surface(
                color = barColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .border(1.dp, barBorderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 4.dp
            ) {
                if (isDeleteArmed) {
                    // Delete confirmation state
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Delete ${selectedFiles.size} item${if (selectedFiles.size != 1) "s" else ""}?",
                            color = Color(0xFFCC3333),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium
                        )
                        Row {
                            TextButton(onClick = { isDeleteArmed = false }) {
                                Text(
                                    "Cancel",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = {
                                selectedFiles.forEach { file ->
                                    viewModel.deleteFromCollection(context, file, spaceType)
                                }
                                selectedItems = emptySet()
                                isDeleteArmed = false
                            }) {
                                Text(
                                    "Delete",
                                    color = Color(0xFFCC3333),
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Normal action bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Share
                        IconButton(onClick = {
                            if (selectedFiles.size == 1) {
                                val file = selectedFiles.first()
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = when (spaceType) {
                                        "Audio" -> "audio/*"
                                        "Video" -> "video/*"
                                        else -> "image/png"
                                    }
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
//                                context.startActivity(Intent.createChooser(shareIntent, "Share"))
                                context.startActivity(Intent.createChooser(shareIntent, "Share"))
                            } else {
                                val uris = ArrayList(selectedFiles.map { file ->
                                    FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                })
                                val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = when (spaceType) {
                                        "Audio" -> "audio/*"
                                        "Video" -> "video/*"
                                        else -> "image/*"
                                    }
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share ${selectedFiles.size} items")
                                )
                            }
                            selectedItems = emptySet()
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White.copy(alpha = 0.45f),
                                    modifier = Modifier.size(22.dp)
                                )
//                                Text(
//                                    "Share",
//                                    fontSize = 10.sp,
//                                    color = Color.White.copy(alpha = 0.35f),
//                                    fontFamily = FontFamily.SansSerif
//                                )
                            }
                        }

                        // Export to Ember
                        IconButton(onClick = { showExportDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔥", fontSize = 20.sp)
//                                Text(
//                                    "Ember",
//                                    fontSize = 10.sp,
//                                    color = Color(0xFFFF8800).copy(alpha = 0.5f),
//                                    fontFamily = FontFamily.SansSerif
//                                )
                            }
                        }

                        // Send to Canvas — only for images, only when exactly 1 selected
                        if (spaceType == "Image" && onSendToCanvas != null && selectedFiles.size == 1) {
                            IconButton(onClick = {
                                onSendToCanvas(selectedFiles.first())
                                selectedItems = emptySet()
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Create,
                                        contentDescription = "Share",
                                        tint = Color.White.copy(alpha = 0.45f),
                                        modifier = Modifier.size(22.dp)
                                    )
//                                    Text("✏️", fontSize = 20.sp)
//                                    Text(
//                                        "Canvas",
//                                        fontSize = 10.sp,
//                                        color = Color.White.copy(alpha = 0.35f),
//                                        fontFamily = FontFamily.SansSerif
//                                    )
                                }
                            }
                        }

                        // Delete
                        IconButton(onClick = { isDeleteArmed = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White.copy(alpha = 0.35f),
                                    modifier = Modifier.size(22.dp)
                                )
//                                Text(
//                                    "Delete",
//                                    fontSize = 10.sp,
//                                    color = Color.White.copy(alpha = 0.25f),
//                                    fontFamily = FontFamily.SansSerif
//                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // FULLSCREEN PREVIEW OVERLAY
        // =========================================================================
        previewTarget?.let { file ->
            FullscreenPreview(
                file = file,
                spaceType = spaceType,
                onDismiss = { previewTarget = null }
            )
        }
    }
}

// =============================================================================
// COLLECTION CARD — clean, focused on the content
// =============================================================================

/**
 * A clean collection card.
 * Shows content preview and date — actions live in the action bar.
 * Selection is indicated by a checkmark overlay and border glow.
 */
@Composable
fun CollectionCard(
    file: File,
    spaceType: String = "Image",
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val bitmap = remember(file.absolutePath) {
        if (spaceType == "Image") {
            try {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dateText = remember(file.lastModified()) {
        dateFormat.format(Date(file.lastModified()))
    }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF00FFCC).copy(alpha = 0.6f)
                     else Color(0xFF00FFCC).copy(alpha = 0.08f),
        animationSpec = tween(250),
        label = "borderColor"
    )

    // Icon for non-image spaces
    val spaceIcon = when (spaceType) {
        "Audio" -> "🎵"
        "Video" -> "🎬"
        else -> null
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(isSelectionMode, isSelected) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        // Preview area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(Color(0xFF222222)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "$spaceType — $dateText",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (spaceIcon != null) {
                // Audio/Video show an icon + filename
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(spaceIcon, fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        file.nameWithoutExtension,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 2
                    )
                }
            }

            // Selection indicator — checkmark in top-right corner
            if (isSelected || isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color(0xFF00FFCC).copy(alpha = 0.9f)
                            else Color.Black.copy(alpha = 0.4f)
                        )
                        .border(
                            1.5.dp,
                            if (isSelected) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.4f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color(0xFF161616),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Footer: date label
        Text(
            dateText,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.4f),
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

// =============================================================================
// FULLSCREEN PREVIEW — overlay for tapped items
// =============================================================================

/**
 * Fullscreen preview overlay.
 * Image: fills screen with Fit scaling.
 * Audio/Video: centered icon with filename.
 * Tap or back dismisses.
 */
@Composable
fun FullscreenPreview(
    file: File,
    spaceType: String,
    onDismiss: () -> Unit
) {
    val bitmap = remember(file.absolutePath) {
        if (spaceType == "Image") {
            try {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val dateText = remember(file.lastModified()) {
        dateFormat.format(Date(file.lastModified()))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .pointerInput(Unit) {
                detectTapGestures { onDismiss() }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = file.nameWithoutExtension,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        } else {
            // Audio/Video preview
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    when (spaceType) {
                        "Audio" -> "🎵"
                        "Video" -> "🎬"
                        else -> "📄"
                    },
                    fontSize = 64.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    file.nameWithoutExtension,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "${file.extension.uppercase()} · $dateText",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.35f),
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // Close button top-right
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close preview",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
        }

        // File info bottom
        Text(
            dateText,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.3f),
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        )
    }
}
