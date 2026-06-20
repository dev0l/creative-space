package com.example.csor.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.csor.viewmodel.EmberBundle
import com.example.csor.viewmodel.MotionViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Collection screen — space-scoped media library.
 *
 * Each space (Image, Audio, Video) has its own collection directory.
 * Items can be shared, exported to ember bundles, deleted, or (images) sent to canvas.
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

    // Track which file is being exported to ember
    var exportTarget by remember { mutableStateOf<File?>(null) }
    var showCreateEmber by remember { mutableStateOf(false) }
    var newEmberName by remember { mutableStateOf("") }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
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

            // =========================================================================
            // EXPORT TO EMBER DIALOG
            // =========================================================================
            exportTarget?.let { file ->
                AlertDialog(
                    onDismissRequest = { exportTarget = null },
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
                                        viewModel.addToEmberBundle(context, bundle.directory, file, spaceType)
                                        exportTarget = null
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
                                                viewModel.addToEmberBundle(context, bundleDir, file, spaceType)
                                            }
                                            exportTarget = null
                                            showCreateEmber = false
                                            newEmberName = ""
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
                            exportTarget = null
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
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    items(collection) { file ->
                        CollectionCard(
                            file = file,
                            spaceType = spaceType,
                            onShare = {
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
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share")
                                )
                            },
                            onExportToEmber = { exportTarget = file },
                            onSendToCanvas = if (spaceType == "Image" && onSendToCanvas != null) {
                                { onSendToCanvas(file) }
                            } else null,
                            onDelete = { viewModel.deleteFromCollection(context, file, spaceType) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single collection item card.
 * Shows image preview, date, and context-aware actions.
 */
@Composable
fun CollectionCard(
    file: File,
    spaceType: String = "Image",
    onShare: () -> Unit,
    onExportToEmber: () -> Unit,
    onSendToCanvas: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
        targetValue = if (showDeleteConfirm) Color(0xFFCC3333).copy(alpha = 0.6f)
                     else Color(0xFF00FFCC).copy(alpha = 0.15f),
        animationSpec = tween(300),
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
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
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
        }

        // Footer: date + actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                dateText,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f),
                fontFamily = FontFamily.SansSerif
            )

            Row {
                // Send to Canvas (image only)
                if (onSendToCanvas != null) {
                    IconButton(
                        onClick = onSendToCanvas,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(
                            "✏️",
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }

                // Export to Ember
                IconButton(
                    onClick = onExportToEmber,
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(
                        "🔥",
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Share
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF00FFCC).copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Delete — with confirmation
                if (showDeleteConfirm) {
                    TextButton(
                        onClick = {
                            onDelete()
                            showDeleteConfirm = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFCC3333)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Delete?", fontSize = 10.sp)
                    }
                } else {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
