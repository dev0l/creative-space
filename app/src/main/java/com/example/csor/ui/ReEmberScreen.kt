package com.example.csor.ui

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Re-Ember screen — the bridge for complete ember journeys.
 *
 * Shows ember bundles (cross-space creative journeys).
 * Each bundle contains items from multiple spaces.
 * Users can create new bundles, import, and export.
 *
 * Warm amber theme — visually distinct from creative spaces.
 */
@Composable
fun ReEmberScreen(
    viewModel: MotionViewModel,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val emberBundles by viewModel.emberBundles.collectAsState()
    var deleteTarget by remember { mutableStateOf<EmberBundle?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newEmberName by remember { mutableStateOf("") }

    // Load ember bundles when screen appears
    LaunchedEffect(Unit) {
        viewModel.loadEmberBundles(context)
    }

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
                IconButton(onClick = onNavigateHome) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFFF8800).copy(alpha = 0.8f)
                    )
                }

                Text(
                    "μEmbers",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFFFF8800).copy(alpha = 0.8f),
                    fontFamily = FontFamily.SansSerif
                )

                // Create new ember bundle
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Create ember",
                        tint = Color(0xFFFF8800).copy(alpha = 0.8f)
                    )
                }
            }

            // =========================================================================
            // CREATE EMBER DIALOG
            // =========================================================================
            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showCreateDialog = false
                        newEmberName = ""
                    },
                    title = {
                        Text(
                            "New Ember",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF8800)
                        )
                    },
                    text = {
                        Column {
                            Text(
                                "Name your creative journey",
                                fontFamily = FontFamily.SansSerif,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
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
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newEmberName.isNotBlank()) {
                                    viewModel.createEmberBundle(context, newEmberName)
                                    showCreateDialog = false
                                    newEmberName = ""
                                }
                            }
                        ) {
                            Text(
                                "Ignite",
                                color = Color(0xFFFF8800),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showCreateDialog = false
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
            // DELETE CONFIRMATION DIALOG
            // =========================================================================
            deleteTarget?.let { bundle ->
                AlertDialog(
                    onDismissRequest = { deleteTarget = null },
                    title = {
                        Text(
                            "Release \"${bundle.name}\"?",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF8800)
                        )
                    },
                    text = {
                        Text(
                            "This ember and all ${bundle.items.size} item${if (bundle.items.size != 1) "s" else ""} will be released. This cannot be undone.",
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteEmberBundle(context, bundle)
                                deleteTarget = null
                            }
                        ) {
                            Text(
                                "Release",
                                color = Color(0xFFFF6644),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteTarget = null }) {
                            Text(
                                "Keep",
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
            if (emberBundles.isEmpty()) {
                // Empty state with ember warmth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🔥", fontSize = 48.sp)

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            "Re-Ember",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            color = Color(0xFFFF8800).copy(alpha = 0.7f),
                            fontFamily = FontFamily.SansSerif
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "Carry the ember across territories",
                            fontSize = 14.sp,
                            color = Color(0xFFFF8800).copy(alpha = 0.4f),
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            "An ember is not a single file.\n" +
                            "It's the path a creative spark took\n" +
                            "across territories.\n\n" +
                            "Create an ember with + above,\n" +
                            "then export items from any Collection.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.2f),
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                // Bundle count
                Text(
                    "${emberBundles.size} ember${if (emberBundles.size != 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = Color(0xFFFF8800).copy(alpha = 0.35f),
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                // Ember bundle list
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    items(emberBundles) { bundle ->
                        EmberBundleCard(
                            bundle = bundle,
                            onShare = {
                                // Share the first item as a preview (or all, depending on count)
                                val shareFile = bundle.thumbnail ?: bundle.items.firstOrNull()
                                if (shareFile != null) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        shareFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "*/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Share ember: ${bundle.name}")
                                    )
                                }
                            },
                            onDelete = { deleteTarget = bundle }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card representing an ember bundle — a cross-space creative journey.
 * Shows bundle name, item count, thumbnail preview, and actions.
 */
@Composable
fun EmberBundleCard(
    bundle: EmberBundle,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val thumbnail = remember(bundle.thumbnail?.absolutePath) {
        bundle.thumbnail?.let {
            try {
                BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val dateText = remember(bundle.created) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(bundle.created)
            dateFormat.format(date ?: Date())
        } catch (_: Exception) {
            ""
        }
    }

    // Count items by space type
    val imageCt = bundle.items.count { it.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp") }
    val audioCt = bundle.items.count { it.extension.lowercase() in listOf("mp3", "wav", "ogg", "m4a") }
    val videoCt = bundle.items.count { it.extension.lowercase() in listOf("mp4", "mkv", "webm", "mov") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1008))
            .border(1.dp, Color(0xFFFF8800).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail or ember icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF201008)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = bundle.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("🔥", fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Bundle info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                bundle.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFF8800).copy(alpha = 0.9f),
                fontFamily = FontFamily.SansSerif,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Item breakdown
            val parts = mutableListOf<String>()
            if (imageCt > 0) parts.add("$imageCt 🖼")
            if (audioCt > 0) parts.add("$audioCt 🎵")
            if (videoCt > 0) parts.add("$videoCt 🎬")
            val summary = if (parts.isEmpty()) "Empty ember" else parts.joinToString("  ")

            Text(
                summary,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.35f),
                fontFamily = FontFamily.SansSerif
            )

            if (dateText.isNotEmpty()) {
                Text(
                    dateText,
                    fontSize = 10.sp,
                    color = Color(0xFFFF8800).copy(alpha = 0.25f),
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // Actions
        IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Share,
                contentDescription = "Share",
                tint = Color(0xFFFF8800).copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color(0xFFFF6644).copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
