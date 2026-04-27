package com.example.csor.ui

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class HubState { IDLE, SPACES, DRAWING }

@Composable
fun HubPortalScreen(onNavigateToCanvas: () -> Unit) {
    var isExpanding by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var hubState by remember { mutableStateOf(HubState.IDLE) }
    var selectedOption by remember { mutableStateOf<String?>(null) }

    // Expansion animation — burst transition
    val expandScale = remember { Animatable(1f) }
    val burstFlash = remember { Animatable(0f) }
    val gridTension = remember { Animatable(0f) } // Inward pull before burst

    // Trigger expansion from current finger position
    LaunchedEffect(zoomScale) {
        if (zoomScale > 2.5f && !isExpanding) {
            if (hubState == HubState.DRAWING && selectedOption != null) {
                isExpanding = true
            }
        }
    }

    LaunchedEffect(isExpanding) {
        if (isExpanding) {
            // Phase 1: Tension pull (grid contracts inward slightly)
            gridTension.animateTo(1f, tween(150, easing = FastOutSlowInEasing))

            // Phase 2: Scale up to near screen edges (burst point)
            expandScale.snapTo(zoomScale.coerceAtLeast(1f))
            expandScale.animateTo(3.5f, tween(300, easing = FastOutLinearInEasing))

            // Phase 3: Burst flash
            gridTension.snapTo(0f)
            burstFlash.snapTo(1f)
            burstFlash.animateTo(0f, tween(150, easing = LinearOutSlowInEasing))

            // Phase 4: Navigate
            onNavigateToCanvas()
            isExpanding = false
            zoomScale = 1f
            hubState = HubState.IDLE
            selectedOption = null
            expandScale.snapTo(1f)
            burstFlash.snapTo(0f)
        }
    }

    val finalScale = if (isExpanding) expandScale.value else zoomScale

    val menuProgress by animateFloatAsState(
        targetValue = if (hubState != HubState.IDLE) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "menuProgress"
    )

    // Transition flash overlay — driven by burstFlash Animatable, not animateFloatAsState
    val flashAlpha = burstFlash.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (!isExpanding) {
                        zoomScale = (zoomScale * zoom).coerceAtLeast(1f)
                        if (zoomScale > 1.1f && hubState == HubState.SPACES) {
                            hubState = HubState.IDLE
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        ) {
            Text(
                text = "(: >x< :)",
                color = Color(0xFF00FFCC).copy(alpha = 0.8f),
                fontSize = 42.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                modifier = Modifier.alpha(
                    when {
                        isExpanding -> 0f
                        zoomScale > 1.2f -> (1f - ((zoomScale - 1.2f) / 1.3f)).coerceIn(0f, 1f)
                        else -> 1f
                    }
                )
            )
        }

        // Central Hub Bulb (rendered BEFORE menu so menu nodes are on top for touch)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(400.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            if (hubState == HubState.IDLE) {
                                hubState = HubState.SPACES
                            }
                            zoomScale = 1f
                        },
                        onTap = {
                            // Tap cycles back: DRAWING → SPACES → IDLE
                            when (hubState) {
                                HubState.DRAWING -> {
                                    hubState = HubState.SPACES
                                    selectedOption = null
                                }
                                HubState.SPACES -> hubState = HubState.IDLE
                                HubState.IDLE -> {}
                            }
                        }
                    )
                }
        ) {
            val infiniteTransition = rememberInfiniteTransition()
            
            // Subtle breathing pulse for the bulb
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )

            // Emitting ripple — spring snap with fast alpha fade
            // The impulse: sharp elastic snap at the start, then slow drift outward
            val rippleScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 2.5f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 2800
                        1f at 0 using FastOutSlowInEasing          // Start
                        1.6f at 200 using LinearOutSlowInEasing    // Elastic snap — overshoot
                        1.45f at 400 using FastOutSlowInEasing     // Settle back slightly
                        2.5f at 2800 using LinearOutSlowInEasing   // Slow drift to full radius
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleScale"
            )

            // Alpha fades out fast so only the initial snap is visible
            val rippleAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 2800
                        0.5f at 0                                  // Full intensity at impulse
                        0.35f at 200                               // Still visible during snap
                        0.15f at 600                               // Fading through settle
                        0f at 1200                                 // Gone before drift completes
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleAlpha"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = 70.dp.toPx() * finalScale * pulseScale

                // =========================================================================
                // AMBIENT STATIC GRID (Always visible — constant depth)
                // =========================================================================
                val ambientBrush = SolidColor(Color(0xFF00FFCC).copy(alpha = 0.04f))
                drawCircuitGrid(ambientBrush)

                // Draw Sonar Grid when idle
                if (hubState == HubState.IDLE && !isExpanding && zoomScale <= 1f) {
                    val rippleRadius = 70.dp.toPx() * rippleScale

                    // Sonar Reveal of the full screen circuit grid
                    val sonarBrush = Brush.radialGradient(
                        0.5f to Color.Transparent,
                        0.85f to Color(0xFF00FFCC).copy(alpha = rippleAlpha * 0.8f),
                        1.0f to Color.Transparent,
                        center = center,
                        radius = rippleRadius.coerceAtLeast(1f)
                    )
                    
                    drawCircuitGrid(sonarBrush)
                }

                // =========================================================================
                // ENERGY BUILDUP + TENSION PULL (Grid contracts inward before burst)
                // =========================================================================
                if (isExpanding) {
                    val tensionAmount = gridTension.value
                    val buildupAlpha = (expandScale.value / 3.5f).coerceIn(0f, 1f)
                    
                    // Tension: grid contracts inward a few pixels
                    if (tensionAmount > 0f) {
                        val tensionBrush = Brush.radialGradient(
                            0.0f to Color(0xFF00FFCC).copy(alpha = 0.3f * tensionAmount),
                            0.6f to Color(0xFF00FFCC).copy(alpha = 0.15f * tensionAmount),
                            1.0f to Color.Transparent,
                            center = center,
                            radius = (size.minDimension * 0.5f * (1f - tensionAmount * 0.08f)).coerceAtLeast(1f)
                        )
                        drawCircuitGrid(tensionBrush)
                    }
                    
                    // Buildup: grid brightens as energy builds
                    val buildupBrush = SolidColor(Color(0xFF00FFCC).copy(alpha = 0.25f * buildupAlpha))
                    drawCircuitGrid(buildupBrush)
                }

                // =========================================================================
                // TINY ENERGY SOURCE CONCEPT (Current Active Theme)
                // =========================================================================
                val zoomIntensity = ((zoomScale - 1f) / 1.5f).coerceIn(0f, 1f)
                val tinyRadius = 6.dp.toPx() * finalScale * pulseScale
                val coreLightPaint = Paint().apply {
                    color = Color.White
                }
                val coreGlowPaint = Paint().apply {
                    color = Color(0xFF00FFCC).copy(alpha = 0.8f + zoomIntensity * 0.2f)
                    asFrameworkPaint().apply {
                        maskFilter = BlurMaskFilter(
                            (25f + zoomIntensity * 40f) * finalScale * pulseScale,
                            BlurMaskFilter.Blur.NORMAL
                        )
                    }
                }
                
                drawIntoCanvas { canvas ->
                    canvas.drawCircle(center, tinyRadius * 2f, coreGlowPaint)
                    canvas.drawCircle(center, tinyRadius, coreLightPaint)
                }

                // Reactive outer ring during pinch gesture
                if (zoomScale > 1.05f && !isExpanding) {
                    drawCircle(
                        color = Color(0xFF00FFCC).copy(alpha = 0.12f * zoomIntensity),
                        radius = tinyRadius * 4f,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                }
            }
        }

        // Electric Circuits Layer (rendered AFTER hub box so nodes receive touch)
        if (menuProgress > 0f) {
            ElectricCircuitMenu(
                progress = menuProgress,
                hubState = hubState,
                selectedOption = selectedOption,
                onSpaceSelected = { space ->
                    if (space == "Drawing") {
                        hubState = HubState.DRAWING
                        selectedOption = null
                    }
                },
                onOptionSelected = { option ->
                    selectedOption = option
                }
            )
        }

        // =========================================================================
        // BURST FLASH OVERLAY (teal-tinted energy release)
        // =========================================================================
        if (flashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = flashAlpha * 0.9f),
                                Color(0xFF00FFCC).copy(alpha = flashAlpha * 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun ElectricCircuitMenu(
    progress: Float,
    hubState: HubState,
    selectedOption: String?,
    onSpaceSelected: (String) -> Unit,
    onOptionSelected: (String) -> Unit
) {
    val density = LocalDensity.current.density
    
    // =========================================================================
    // STAGGERED ELECTRON ANIMATIONS (per-path with ~100ms delays)
    // =========================================================================
    val staggerDelays = listOf(0L, 100L, 220L) // Slightly irregular for organic feel
    val electronProgress = List(3) { remember { Animatable(0f) } }
    
    LaunchedEffect(progress) {
        if (progress > 0f) {
            electronProgress.forEachIndexed { index, animatable ->
                kotlinx.coroutines.launch {
                    kotlinx.coroutines.delay(staggerDelays[index])
                    animatable.animateTo(
                        progress,
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            }
        } else {
            electronProgress.forEach { it.snapTo(0f) }
        }
    }

    // Track which nodes have had their electron arrive (for hologram flicker)
    val nodeArrived = electronProgress.map { it.value >= 0.95f }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val paths = getCircuitPaths(center)
            
            val faintPathPaint = Paint().apply {
                color = Color(0xFF00FFCC).copy(alpha = 0.15f)
                style = PaintingStyle.Stroke
                strokeWidth = 3f
                asFrameworkPaint().apply {
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                }
            }

            val electronGlowPaint = Paint().apply {
                color = Color(0xFF00FFCC)
                asFrameworkPaint().apply {
                    maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
                }
            }

            val electronCorePaint = Paint().apply {
                color = Color.White
            }

            val chargedPathPaint = Paint().apply {
                color = Color(0xFF00FFCC).copy(alpha = 0.5f)
                style = PaintingStyle.Stroke
                strokeWidth = 3f
                asFrameworkPaint().apply {
                    maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                }
            }

            paths.forEachIndexed { index, fullPath ->
                val pathProgress = electronProgress[index].value

                drawIntoCanvas { canvas ->
                    canvas.drawPath(fullPath, faintPathPaint)
                }

                val measure = android.graphics.PathMeasure(fullPath.asAndroidPath(), false)
                if (pathProgress > 0f) {
                    val trailPath = android.graphics.Path()
                    measure.getSegment(0f, measure.length * pathProgress, trailPath, true)
                    drawIntoCanvas { canvas ->
                        canvas.drawPath(trailPath.asComposePath(), chargedPathPaint)
                    }
                }

                val pos = FloatArray(2)
                val tan = FloatArray(2)
                measure.getPosTan(measure.length * pathProgress, pos, tan)

                if (pathProgress > 0f && pathProgress < 1f) {
                    drawIntoCanvas { canvas ->
                        canvas.drawCircle(Offset(pos[0], pos[1]), 12f, electronGlowPaint)
                        canvas.drawCircle(Offset(pos[0], pos[1]), 6f, electronCorePaint)
                    }
                }
            }
        }

        // =========================================================================
        // TIER 1: Creative Space Selection (nodes materialize on electron arrival)
        // =========================================================================
        if (hubState == HubState.SPACES) {
            val nodeLabels = listOf("Drawing", "Sound\n[Locked]", "Settings")
            val nodeOffsets = listOf(
                Pair((-300 / density).dp, (-350 / density).dp),
                Pair((320 / density).dp, (-370 / density).dp),
                Pair((100 / density).dp, (390 / density).dp)
            )
            val nodeCallbacks: List<() -> Unit> = listOf(
                { onSpaceSelected("Drawing") },
                {},
                {}
            )

            nodeLabels.forEachIndexed { index, label ->
                if (nodeArrived[index]) {
                    HologramNode(
                        text = label,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = nodeOffsets[index].first, y = nodeOffsets[index].second),
                        onClick = nodeCallbacks[index]
                    )
                }
            }
        }

        // =========================================================================
        // TIER 2: Drawing Space Options
        // =========================================================================
        if (hubState == HubState.DRAWING) {
            val unselectedAlpha by animateFloatAsState(
                targetValue = if (selectedOption != null) 0f else 1f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "unselectedFade"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                MenuNode(
                    text = "Free Canvas",
                    isCharged = true,
                    isSelected = selectedOption == "Free Canvas",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = (-300 / density).dp, y = (-350 / density).dp)
                        .alpha(if (selectedOption == "Free Canvas" || selectedOption == null) 1f else unselectedAlpha),
                    onClick = { onOptionSelected("Free Canvas") }
                )

                MenuNode(
                    text = "Import Canvas\n[Soon]",
                    isCharged = true,
                    isSelected = selectedOption == "Import Canvas",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = (320 / density).dp, y = (-370 / density).dp)
                        .alpha(if (selectedOption == "Import Canvas" || selectedOption == null) 1f else unselectedAlpha),
                    onClick = {}
                )
            }
        }
    }
}

/**
 * A menu node that materializes with a hologram flicker effect.
 * Brief alpha oscillation on entry, then settles into stable charged glow.
 */
@Composable
fun HologramNode(text: String, modifier: Modifier, onClick: () -> Unit) {
    var hasFlickered by remember { mutableStateOf(false) }
    val flickerAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Hologram flicker: rapid alpha oscillation then settle
        flickerAlpha.animateTo(0.8f, tween(60))
        flickerAlpha.animateTo(0.2f, tween(50))
        flickerAlpha.animateTo(1f, tween(80))
        flickerAlpha.animateTo(0.5f, tween(40))
        flickerAlpha.animateTo(1f, tween(100))
        hasFlickered = true
    }

    MenuNode(
        text = text,
        isCharged = hasFlickered,
        modifier = modifier.alpha(flickerAlpha.value),
        onClick = onClick
    )
}

@Composable
fun MenuNode(
    text: String,
    isCharged: Boolean = false,
    isSelected: Boolean = false,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val chargeGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chargeGlow"
    )
    
    val borderAlpha = when {
        isSelected -> chargeGlow * 1.5f  // Brighter, more prominent pulse
        isCharged -> chargeGlow
        else -> 0f
    }
    val borderColor = Color(0xFF00FFCC).copy(alpha = borderAlpha.coerceIn(0f, 1f))
    
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .background(
                if (isSelected) Color(0xFF0A2020).copy(alpha = 0.95f)
                else Color(0xFF111111).copy(alpha = 0.9f),
                RoundedCornerShape(6.dp)
            )
            .then(
                if (isCharged || isSelected) Modifier.background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00FFCC).copy(alpha = if (isSelected) 0.12f else 0.05f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(6.dp)
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Capacitor glow border
        if (isCharged || isSelected) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = if (isSelected) 2.5f else 1.5f
                    )
                )
            }
        }
        Text(
            text = text,
            color = when {
                isSelected -> Color.White // Full white when selected — "locked in"
                isCharged -> Color(0xFF00FFCC)
                else -> Color.White.copy(alpha = 0.6f)
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

fun getCircuitPaths(center: Offset): List<Path> {
    // Path 1: Top Left (Free Canvas)
    val p1 = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x - 60f, center.y - 140f)
        lineTo(center.x - 140f, center.y - 120f)
        lineTo(center.x - 220f, center.y - 320f)
        lineTo(center.x - 300f, center.y - 320f)
    }
    
    // Path 2: Top Right (Sound Canvas)
    val p2 = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x + 80f, center.y - 120f)
        lineTo(center.x + 60f, center.y - 220f)
        lineTo(center.x + 240f, center.y - 340f)
        lineTo(center.x + 320f, center.y - 340f)
    }

    // Path 3: Bottom (Settings)
    val p3 = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x + 40f, center.y + 160f)
        lineTo(center.x - 40f, center.y + 240f)
        lineTo(center.x + 40f, center.y + 360f)
        lineTo(center.x + 100f, center.y + 360f)
    }

    return listOf(p1, p2, p3)
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCircuitGrid(brush: Brush) {
    val gridSize = 120f
    val cols = (size.width / gridSize).toInt() + 1
    val rows = (size.height / gridSize).toInt() + 1
    
    val gridPath = Path()
    for (i in 0..cols) {
        for (j in 0..rows) {
            val cx = i * gridSize
            val cy = j * gridSize
            
            // Draw a '+' intersection node
            gridPath.moveTo(cx - 15f, cy)
            gridPath.lineTo(cx + 15f, cy)
            gridPath.moveTo(cx, cy - 15f)
            gridPath.lineTo(cx, cy + 15f)
            
            // Connecting line to the right (skip some for an irregular organic board look)
            if ((i + j) % 2 == 0) {
                gridPath.moveTo(cx + 15f, cy)
                gridPath.lineTo(cx + gridSize - 15f, cy)
            }
            // Connecting line downwards
            if ((i * j) % 3 != 0) {
                gridPath.moveTo(cx, cy + 15f)
                gridPath.lineTo(cx, cy + gridSize - 15f)
            }
        }
    }
    
    drawPath(gridPath, brush, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
}
