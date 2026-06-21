package com.devol.creativespace.ui

import android.content.Intent
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.devol.creativespace.viewmodel.EmberBundle
import com.devol.creativespace.viewmodel.MotionViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ember Bundle Detail Screen — the full view of a creative journey.
 *
 * Shows all items in a bundle with the amber theme.
 * Interaction model mirrors CollectionScreen:
 *   Tap         → Fullscreen preview
 *   Long-press  → Selection mode
 *   Action bar  → Share selected, Remove from bundle
 *
 * A journey deserves a full view, not a peek.
 */
@Composable
fun EmberDetailScreen(
    viewModel: MotionViewModel,
    bundleName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val emberBundles by viewModel.emberBundles.collectAsState()

    // Find the bundle by name (re-resolves on each recomposition after changes)
    val bundle = emberBundles.find { it.name == bundleName }

    // If bundle was deleted while viewing, navigate back
    LaunchedEffect(bundle) {
        if (bundle == null && emberBundles.isNotEmpty()) {
            onNavigateBack()
        }
    }

    // Selection state
    var selectedItems by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedItems.isNotEmpty()
    var previewTarget by remember { mutableStateOf<File?>(null) }

    // Resolve selected paths to files
    val selectedFiles = remember(selectedItems, bundle?.items) {
        bundle?.items?.filter { it.absolutePath in selectedItems } ?: emptyList()
    }

    // Load bundles
    LaunchedEffect(Unit) {
        viewModel.loadEmberBundles(context)
    }

    // Date formatting
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val createdText = remember(bundle?.created) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .parse(bundle?.created ?: "")
            dateFormat.format(date ?: Date())
        } catch (_: Exception) {
            ""
        }
    }

    // Item type counts
    val imageCt = bundle?.items?.count { it.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp") } ?: 0
    val audioCt = bundle?.items?.count { it.extension.lowercase() in listOf("mp3", "wav", "ogg", "m4a") } ?: 0
    val videoCt = bundle?.items?.count { it.extension.lowercase() in listOf("mp4", "mkv", "webm", "mov") } ?: 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF161616),
                        Color(0xFF1A1008),
                        Color(0xFF161616)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // =========================================================================
            // TOP BAR
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
                    IconButton(onClick = { selectedItems = emptySet() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear selection",
                            tint = Color(0xFFFF8800).copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        "${selectedItems.size} selected",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF8800).copy(alpha = 0.9f),
                        fontFamily = FontFamily.SansSerif
                    )

                    TextButton(onClick = {
                        selectedItems = if (selectedItems.size == (bundle?.items?.size ?: 0)) {
                            emptySet()
                        } else {
                            bundle?.items?.map { it.absolutePath }?.toSet() ?: emptySet()
                        }
                    }) {
                        Text(
                            if (selectedItems.size == (bundle?.items?.size ?: 0)) "None" else "All",
                            color = Color(0xFFFF8800).copy(alpha = 0.6f),
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFFF8800).copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        bundle?.name ?: "",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFFFF8800).copy(alpha = 0.8f),
                        fontFamily = FontFamily.SansSerif
                    )

                    // Spacer to balance layout
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // =========================================================================
            // BUNDLE HEADER
            // =========================================================================
            if (!isSelectionMode && bundle != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Type breakdown
                    val parts = mutableListOf<String>()
                    if (imageCt > 0) parts.add("$imageCt 🖼")
                    if (audioCt > 0) parts.add("$audioCt 🎵")
                    if (videoCt > 0) parts.add("$videoCt 🎬")
                    val summary = if (parts.isEmpty()) "Empty ember" else parts.joinToString("  ")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            summary,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            fontFamily = FontFamily.SansSerif
                        )
                        if (createdText.isNotEmpty()) {
                            Text(
                                createdText,
                                fontSize = 12.sp,
                                color = Color(0xFFFF8800).copy(alpha = 0.3f),
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFFF8800).copy(alpha = 0.1f))
                    )
                }
            }

            // =========================================================================
            // CONTENT — Item Grid
            // =========================================================================
            if (bundle == null || bundle.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔥", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "This ember is waiting for sparks",
                            fontSize = 14.sp,
                            color = Color(0xFFFF8800).copy(alpha = 0.4f),
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Export items from any Collection\nto add them to this journey",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.2f),
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = if (isSelectionMode) 80.dp else 12.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    items(bundle.items) { file ->
                        val isSelected = file.absolutePath in selectedItems
                        EmberItemCard(
                            file = file,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onTap = {
                                if (isSelectionMode) {
                                    selectedItems = if (isSelected) {
                                        selectedItems - file.absolutePath
                                    } else {
                                        selectedItems + file.absolutePath
                                    }
                                } else {
                                    previewTarget = file
                                }
                            },
                            onLongPress = {
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
        // ACTION BAR — Share selected, Remove from bundle (two-step confirmation)
        // =========================================================================
        var isRemoveArmed by remember { mutableStateOf(false) }

        // Reset remove arm when selection changes
        LaunchedEffect(selectedItems) {
            if (selectedItems.isEmpty()) isRemoveArmed = false
        }

        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val barColor by animateColorAsState(
                targetValue = if (isRemoveArmed) Color(0xFF331111) else Color(0xFF1A1008),
                animationSpec = tween(300),
                label = "barColor"
            )
            val barBorderColor by animateColorAsState(
                targetValue = if (isRemoveArmed) Color(0xFFCC3333).copy(alpha = 0.5f)
                             else Color(0xFFFF8800).copy(alpha = 0.15f),
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
                if (isRemoveArmed) {
                    // Remove confirmation state
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Remove ${selectedFiles.size} item${if (selectedFiles.size != 1) "s" else ""}?",
                            color = Color(0xFFCC3333),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium
                        )
                        Row {
                            TextButton(onClick = { isRemoveArmed = false }) {
                                Text(
                                    "Cancel",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = {
                                if (bundle != null) {
                                    selectedFiles.forEach { file ->
                                        viewModel.removeFromEmberBundle(context, bundle, file)
                                    }
                                }
                                selectedItems = emptySet()
                                isRemoveArmed = false
                            }) {
                                Text(
                                    "Remove",
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
                        // Share selected
                        IconButton(onClick = {
                            if (selectedFiles.size == 1) {
                                val file = selectedFiles.first()
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
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
                                    type = "*/*"
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

                        // Remove from bundle (arms confirmation)
                        IconButton(onClick = { isRemoveArmed = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color.White.copy(alpha = 0.35f),
                                    modifier = Modifier.size(22.dp)
                                )
//                                Text("✕", fontSize = 20.sp, color = Color.White.copy(alpha = 0.35f))
//                                Text(
//                                    "Remove",
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
        // FULLSCREEN PREVIEW
        // =========================================================================
        previewTarget?.let { file ->
            val spaceType = when {
                file.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp") -> "Image"
                file.extension.lowercase() in listOf("mp3", "wav", "ogg", "m4a") -> "Audio"
                file.extension.lowercase() in listOf("mp4", "mkv", "webm", "mov") -> "Video"
                else -> "Image"
            }
            FullscreenPreview(
                file = file,
                spaceType = spaceType,
                onDismiss = { previewTarget = null }
            )
        }
    }
}

// =============================================================================
// EMBER ITEM CARD — amber-themed, same interaction pattern as Collection
// =============================================================================

/**
 * Card for an item within an ember bundle.
 * Amber-themed with space origin badge.
 */
@Composable
fun EmberItemCard(
    file: File,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val isImage = file.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp")
    val isAudio = file.extension.lowercase() in listOf("mp3", "wav", "ogg", "m4a")

    val bitmap = remember(file.absolutePath) {
        if (isImage) {
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
        targetValue = if (isSelected) Color(0xFFFF8800).copy(alpha = 0.6f)
                     else Color(0xFFFF8800).copy(alpha = 0.08f),
        animationSpec = tween(250),
        label = "borderColor"
    )

    // Space origin badge
    val spaceIcon = when {
        isImage -> "🖼"
        isAudio -> "🎵"
        else -> "🎬"
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1008))
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
                .background(Color(0xFF201008)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = file.nameWithoutExtension,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
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

            // Space origin badge — bottom-left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(spaceIcon, fontSize = 11.sp)
            }

            // Selection indicator
            if (isSelected || isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color(0xFFFF8800).copy(alpha = 0.9f)
                            else Color.Black.copy(alpha = 0.4f)
                        )
                        .border(
                            1.5.dp,
                            if (isSelected) Color(0xFFFF8800) else Color.White.copy(alpha = 0.4f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color(0xFF1A1008),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Footer
        Text(
            dateText,
            fontSize = 10.sp,
            color = Color(0xFFFF8800).copy(alpha = 0.35f),
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}
