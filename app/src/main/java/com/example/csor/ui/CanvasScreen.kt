package com.example.csor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.csor.sensors.MotionMapper
import com.example.csor.viewmodel.MotionViewModel

@Composable
fun CanvasScreen(viewModel: MotionViewModel, onNavigateHome: () -> Unit) {
    val context = LocalContext.current
    val completedStrokes by viewModel.completedStrokes.collectAsState()
    val currentStroke by viewModel.currentStroke.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val backgroundImage by viewModel.backgroundImage.collectAsState()

    // Load background bitmap once when the file reference changes
    val backgroundBitmap: ImageBitmap? = remember(backgroundImage) {
        backgroundImage?.let {
            try { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() } catch (_: Exception) { null }
        }
    }

    // Settings state
    val sensitivity by viewModel.sensitivity.collectAsState()
    val lineThickness by viewModel.lineThickness.collectAsState()
    val lineColor by viewModel.lineColor.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var saveConfirm by remember { mutableStateOf(false) }
    var showFreshCanvasDialog by remember { mutableStateOf(false) }
    val invertDrawing by viewModel.invertDrawing.collectAsState()

    // Reset "Saved!" confirmation after 2 seconds
    LaunchedEffect(saveConfirm) {
        if (saveConfirm) {
            kotlinx.coroutines.delay(2000)
            saveConfirm = false
        }
    }

    // Pause tracking when settings is open, resume only if we caused the pause
    var pausedBySettings by remember { mutableStateOf(false) }
    LaunchedEffect(showSettings) {
        if (showSettings && viewModel.isTracking.value) {
            viewModel.toggleTracking()
            pausedBySettings = true
        } else if (!showSettings && pausedBySettings) {
            viewModel.toggleTracking()
            pausedBySettings = false
        }
    }

    // Lifecycle-aware sensor management — pauses when minimized, resumes when foregrounded
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val mapper = MotionMapper(context, viewModel)
        mapper.start()

        if (!viewModel.isTracking.value) {
            viewModel.toggleTracking()
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> mapper.stop()
                Lifecycle.Event.ON_RESUME -> {
                    mapper.stop() // Ensure clean state before re-registering
                    mapper.start()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapper.stop()
            if (viewModel.isTracking.value) {
                viewModel.toggleTracking()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFCFBF4))) {

        // =========================================================================
        // FRESH CANVAS CONFIRMATION DIALOG
        // =========================================================================
        if (showFreshCanvasDialog) {
            AlertDialog(
                onDismissRequest = { showFreshCanvasDialog = false },
                title = {
                    Text(
                        "Fresh canvas?",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium
                    )
                },
                text = {
                    Text(
                        "This will clear everything and start fresh.",
                        fontFamily = FontFamily.SansSerif,
                        color = Color.Gray
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showFreshCanvasDialog = false
                            viewModel.clearCanvas()
                        }
                    ) {
                        Text(
                            "Start fresh",
                            color = Color(0xFF1A1A1A),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFreshCanvasDialog = false }) {
                        Text(
                            "Cancel",
                            color = Color.Gray,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                containerColor = Color(0xFFFCFBF4)
            )
        }
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            viewModel.setCanvasSize(size.width, size.height)

            // Background image layer (loaded from Gallery)
            backgroundBitmap?.let { bmp ->
                drawImage(
                    image = bmp,
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )
            }

            val strokeParams = Stroke(
                width = lineThickness,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )

            for (strokePoints in completedStrokes) {
                if (strokePoints.isEmpty()) continue
                val path = Path().apply {
                    moveTo(strokePoints.first().x, strokePoints.first().y)
                    for (i in 1 until strokePoints.size) {
                        lineTo(strokePoints[i].x, strokePoints[i].y)
                    }
                }
                drawPath(path, color = lineColor, style = strokeParams)
            }

            if (currentStroke.isNotEmpty()) {
                val path = Path().apply {
                    moveTo(currentStroke.first().x, currentStroke.first().y)
                    for (i in 1 until currentStroke.size) {
                        lineTo(currentStroke[i].x, currentStroke[i].y)
                    }
                }
                drawPath(path, color = lineColor, style = strokeParams)
                
                val lastPoint = currentStroke.last()
                drawCircle(color = Color(0xFF00AA00), radius = 6f, center = lastPoint)
            }
        }

        // =========================================================================
        // TOP BAR (with system bar insets)
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
                Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.DarkGray)
            }
            Text("Free Canvas", fontSize = 14.sp, color = Color.Gray)
            Row {
                IconButton(onClick = { showFreshCanvasDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Fresh canvas", tint = Color.DarkGray)
                }
                IconButton(onClick = { showSettings = !showSettings }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.DarkGray)
                }
            }
        }

        // =========================================================================
        // CENTER HINT
        // =========================================================================
        if (completedStrokes.isEmpty() && currentStroke.size < 10 && backgroundImage == null) {
            Text(
                "Move your phone to draw in space",
                color = Color.LightGray,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // =========================================================================
        // BOTTOM CONTROLS (with navigation bar insets)
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = { viewModel.toggleTracking() },
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(if (isTracking) "||" else "▶", fontSize = 18.sp)
            }

            FloatingActionButton(
                onClick = { viewModel.clearCanvas() },
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear")
            }

            FloatingActionButton(
                onClick = {
                    val saved = viewModel.saveCanvas(context)
                    if (saved) {
                        saveConfirm = true
                    }
                },
                containerColor = Color(0xFF1A1A1A),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    if (saveConfirm) "Kept!" else "Keep",
                    fontSize = 12.sp,
                    color = if (saveConfirm) Color(0xFF00FFCC) else Color.White
                )
            }
        }

        // =========================================================================
        // SETTINGS BOTTOM SHEET
        // =========================================================================
        if (showSettings) {
            SettingsSheet(
                sensitivity = sensitivity,
                lineThickness = lineThickness,
                lineColor = lineColor,
                invertDrawing = invertDrawing,
                onSensitivityChange = { viewModel.setSensitivity(it) },
                onThicknessChange = { viewModel.setLineThickness(it) },
                onColorChange = { viewModel.setLineColor(it) },
                onInvertChange = { viewModel.setInvertDrawing(it) },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
fun SettingsSheet(
    sensitivity: Float,
    lineThickness: Float,
    lineColor: Color,
    invertDrawing: Boolean,
    onSensitivityChange: (Float) -> Unit,
    onThicknessChange: (Float) -> Unit,
    onColorChange: (Color) -> Unit,
    onInvertChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colorPalette = listOf(
        Color(0xFF333333), // Charcoal
        Color(0xFF1A1A1A), // Near black
        Color(0xFF00AA00), // Green
        Color(0xFF0088FF), // Blue
        Color(0xFFFF4444), // Red
        Color(0xFFFF8800), // Orange
        Color(0xFF9933FF), // Purple
        Color(0xFF00CCCC)  // Teal
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .clickable(enabled = false, onClick = {}) // Block click-through
                .background(
                    Color(0xFFF5F3EA),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .padding(24.dp)
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.LightGray, RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Sensitivity ──
            SettingsSlider(
                label = "Sensitivity",
                value = sensitivity,
                valueRange = 0.2f..3.0f,
                displayValue = "%.1fx".format(sensitivity),
                onValueChange = onSensitivityChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Line Thickness ──
            SettingsSlider(
                label = "Line Thickness",
                value = lineThickness,
                valueRange = 1f..30f,
                displayValue = "%.0fpx".format(lineThickness),
                onValueChange = onThicknessChange
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Line Color ──
            Text(
                "Line Color",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                colorPalette.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color, CircleShape)
                            .then(
                                if (color == lineColor) Modifier.border(
                                    2.5.dp, Color(0xFF00CCCC), CircleShape
                                ) else Modifier.border(
                                    1.dp, Color.LightGray, CircleShape
                                )
                            )
                            .clickable { onColorChange(color) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Reverse Draw ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Reverse Draw",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = invertDrawing,
                    onCheckedChange = onInvertChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF1A1A1A),
                        checkedTrackColor = Color(0xFF00CCCC),
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color(0xFFE0E0E0)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SettingsSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.width(100.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF1A1A1A),
                activeTrackColor = Color(0xFF1A1A1A),
                inactiveTrackColor = Color.LightGray
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            displayValue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A1A),
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier.width(52.dp)
        )
    }
}
