package com.devol.creativespace.ui

import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlin.time.Duration.Companion.milliseconds

enum class HubState { IDLE, SPACES, SPACE }

/**
 * Data-driven menu node definition.
 * Adding a new option to any tier is just adding an entry to a list.
 */
data class HubMenuNode(
    val label: String,
    val offsetX: Float,
    val offsetY: Float,
    val enabled: Boolean = true,
    val onClick: () -> Unit = {}
)

@Composable
fun HubPortalScreen(
    hubState: HubState,
    onHubStateChange: (HubState) -> Unit,
    initialSpace: String? = null,
    onSpaceCommit: (String, String) -> Unit
) {
    var isExpanding by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    // initialSpace seeds currentSpace on fresh composition (Option B return).
    // No LaunchedEffect needed — synchronous, no frame gap.
    var currentSpace by remember { mutableStateOf(initialSpace) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    val isArmed = hubState == HubState.SPACE && selectedOption != null

    // Charging invitation — tracks finger-down on center bulb in IDLE
    var isCharging by remember { mutableStateOf(false) }

    // Back unwinds one step at a time — each press peels one layer.
    BackHandler(enabled = hubState != HubState.IDLE || selectedOption != null) {
        when {
            selectedOption != null -> selectedOption = null
            hubState == HubState.SPACE -> {
                currentSpace = null
                onHubStateChange(HubState.SPACES)
            }
            hubState == HubState.SPACES -> onHubStateChange(HubState.IDLE)
        }
        zoomScale = 1f
    }

    // Expansion animation — starts from current pinch position, fast burst
    val expandScale = remember { Animatable(1f) }
    val burstFlash = remember { Animatable(0f) }

    // Trigger expansion when armed and pinch exceeds threshold
    LaunchedEffect(zoomScale) {
        if (zoomScale > 2.5f && !isExpanding && isArmed) {
            isExpanding = true
        }
    }

    LaunchedEffect(isExpanding) {
        if (isExpanding) {
            try {
                // Fast burst: scale from fingers -> flash -> navigate
                expandScale.snapTo(zoomScale.coerceAtLeast(1f))
                expandScale.animateTo(4f, tween(350, easing = FastOutSlowInEasing))
                burstFlash.snapTo(1f)
                burstFlash.animateTo(0f, tween(120, easing = LinearOutSlowInEasing))
                // Committed option navigates to its destination
                onSpaceCommit(currentSpace ?: "Image", selectedOption ?: "Canvas")
            } finally {
                // Guaranteed cleanup — runs even if this coroutine is cancelled
                // mid-animation (which happens when onNavigateToCanvas removes
                // HubPortalScreen from composition). Without finally, hubState
                // would stay SPACE on return, blocking long-press from IDLE.
                isExpanding = false
                zoomScale = 1f
                currentSpace = null
                selectedOption = null
                onHubStateChange(HubState.IDLE)
                expandScale.snapTo(1f)
                burstFlash.snapTo(0f)
            }
        }
    }

    val finalScale = if (isExpanding) expandScale.value else zoomScale

    val menuProgress by animateFloatAsState(
        targetValue = if (hubState != HubState.IDLE && selectedOption == null) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "menuProgress"
    )

    // Transition flash overlay — driven by burstFlash Animatable, not animateFloatAsState
    val flashAlpha = burstFlash.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
            // Pinch gesture — keyed on hubState + selectedOption so isArmed
            // is recaptured when state changes (Unit would freeze the initial value)
            .pointerInput(hubState, selectedOption) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (!isExpanding && isArmed) {
                        zoomScale = (zoomScale * zoom).coerceAtLeast(1f)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 60.dp)
        ) {
            Text(
                text = " ",
                color = Color(0xFF00FFCC).copy(alpha = 0.8f),
                fontSize = 28.sp,
                fontFamily = FontFamily.SansSerif,
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
                // Charging detection — tracks finger down/up for visual invitation
                .pointerInput(hubState) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        if (hubState == HubState.IDLE) isCharging = true
                        waitForUpOrCancellation()
                        isCharging = false
                    }
                }
                .pointerInput(hubState, selectedOption, currentSpace) {
                    detectTapGestures(
                        onLongPress = {
                            // Long-press is the discovery gesture — opens from idle only.
                            // Deeper navigation uses tap (one step back) or system back.
                            if (hubState == HubState.IDLE) {
                                isCharging = false // Charging complete — discovery fires
                                onHubStateChange(HubState.SPACES)
                            }
                            zoomScale = 1f
                        },
                        onTap = {
                            // Tap unwinds one step at a time
                            when {
                                selectedOption != null -> selectedOption = null
                                hubState == HubState.SPACE -> {
                                    currentSpace = null
                                    onHubStateChange(HubState.SPACES)
                                }
                                hubState == HubState.SPACES -> onHubStateChange(HubState.IDLE)
                            }
                            zoomScale = 1f
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

            // Emitting ripple to invite interaction
            val rippleScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 2.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleScale"
            )

            val rippleAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleAlpha"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // =========================================================================
                // SCALE MATRIX — coherence map
                //
                // pulseScale  : 1.0 ↔ 1.05   (always breathing, all states)
                // rippleScale : 1.0 → 2.9     (always cycling, visibility gated by showPulse)
                // zoomScale   : 1.0 → ∞       (ONLY changes when isArmed, reset on commit/tap/back)
                // finalScale  : zoomScale OR expandScale.value (during burst)
                // expandScale : zoomScale → 4.0 (only during isExpanding burst animation)
                //
                // In non-armed states, zoomScale ≡ 1.0, so:
                //   zoomIntensity ≡ 0, finalScale ≡ 1.0, outer ring hidden
                //   tinyRadius = 6dp × 1.0 × pulseScale (breathing only)
                //   coreGlow alpha = 0.8 (base), blur = 25 × 1.0 × pulseScale
                //
                // In armed state, pinch drives zoomScale:
                //   tinyRadius grows, glow intensifies, outer ring appears
                //   At 2.5x → burst animation takes over via expandScale
                // =========================================================================

                // =========================================================================
                // AMBIENT STATIC GRID (Always visible — constant depth)
                // Accent shifts to amber in Re-Ember space, matching the circuit menu
                // =========================================================================
                val bulbAccent = if (currentSpace == "Re-Ember") Color(0xFFFF8800) else Color(0xFF00FFCC)

                val ambientBrush = SolidColor(bulbAccent.copy(alpha = 0.01f))
                drawCircuitGrid(ambientBrush)

                // Sonar pulse — visible in IDLE (breathing invitation) and when armed
                // (pinch invitation). Suppressed during active pinch (zoomScale > 1)
                // because the bulb glow takes over as visual feedback.
                val showPulse = (hubState == HubState.IDLE || selectedOption != null) && !isExpanding && zoomScale <= 1f
                if (showPulse) {
                    // When charging: sonar contracts inward (energy drawn toward bulb)
                    // When idle: sonar expands outward (emitting ripple to invite)
                    val effectiveScale = if (isCharging && hubState == HubState.IDLE) {
                        3.0f - rippleScale  // Contracts: starts wide, shrinks to center
                    } else {
                        rippleScale
                    }

                    val effectiveAlpha = if (isCharging && hubState == HubState.IDLE) {
                        (1f - rippleAlpha) * 0.6f  // Brightens as it contracts
                    } else {
                        rippleAlpha
                    }
                    val rippleRadius = 70.dp.toPx() * effectiveScale

                    // Sonar Reveal of the full screen circuit grid
                    val sonarBrush = Brush.radialGradient(
                        0.5f to Color.Transparent,
                        0.85f to bulbAccent.copy(alpha = effectiveAlpha * 0.8f),
                        1.0f to Color.Transparent,
                        center = center,
                        radius = rippleRadius.coerceAtLeast(1f)
                    )
                    
                    drawCircuitGrid(sonarBrush)

                    // Inner glow intensifies when charging — the bulb drinks the energy
                    if (isCharging && hubState == HubState.IDLE) {
                        drawCircle(
                            color = bulbAccent.copy(alpha = 0.3f + rippleScale * 0.04f),
                            radius = 16.dp.toPx(),
                            center = center
                        )
                    }
                }

                // =========================================================================
                // ENERGY BUILDUP (Grid brightens during expand transition)
                // Lines appear proportionally as the burst grows — no pulse dependency
                // =========================================================================
                if (isExpanding) {
                    val buildupBrush = SolidColor(bulbAccent.copy(alpha = 0.06f * (expandScale.value / 4f).coerceIn(0f, 1f)))
                    drawCircuitGrid(buildupBrush)
                }

                // =========================================================================
                // TINY ENERGY SOURCE (Central bulb)
                //
                // Base: 6dp × pulseScale (gentle breathing)
                // Armed + pinch: 6dp × zoomScale × pulseScale (grows with pinch)
                // Expanding: 6dp × expandScale × pulseScale (burst animation)
                // =========================================================================
                val zoomIntensity = ((zoomScale - 1f) / 1.5f).coerceIn(0f, 1f)
                val tinyRadius = 6.dp.toPx() * finalScale * pulseScale
                val coreLightPaint = Paint().apply {
                    color = Color.White
                }
                val coreGlowPaint = Paint().apply {
                    color = bulbAccent.copy(alpha = 0.8f + zoomIntensity * 0.2f)
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

                // Reactive outer ring — only visible during armed pinch
                // (zoomScale > 1.05 is impossible when not armed, so this is
                // implicitly guarded by the pinch guard)
                if (zoomScale > 1.05f && !isExpanding) {
                    drawCircle(
                        color = bulbAccent.copy(alpha = 0.12f * zoomIntensity),
                        radius = tinyRadius * 4f,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                }
            }
        }

        // Electric Circuits Layer (rendered AFTER hub box so nodes receive touch)
        if (menuProgress > 0f || isArmed) {
            ElectricCircuitMenu(
                progress = menuProgress,
                hubState = hubState,
                currentSpace = currentSpace,
                selectedOption = selectedOption,
                onSpaceSelected = { space ->
                    currentSpace = space
                    onHubStateChange(HubState.SPACE)
                    selectedOption = null
                },
                onOptionSelected = { option ->
                    selectedOption = option
                }
            )
        }

        // =========================================================================
        // BURST FLASH OVERLAY (accent-tinted energy release)
        // =========================================================================
        if (flashAlpha > 0f) {
            val flashAccent = if (currentSpace == "Re-Ember") Color(0xFFFF8800) else Color(0xFF00FFCC)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = flashAlpha * 0.9f),
                                flashAccent.copy(alpha = flashAlpha * 0.6f),
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
    currentSpace: String?,
    selectedOption: String?,
    onSpaceSelected: (String) -> Unit,
    onOptionSelected: (String) -> Unit
) {
    val density = LocalDensity.current.density

    Box(modifier = Modifier.fillMaxSize()) {
        // Accent color shifts to amber in Re-Ember space
        val circuitColor = if (currentSpace == "Re-Ember") Color(0xFFFF8800) else Color(0xFF00FFCC)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val paths = getCircuitPaths(center)
            
            val faintPathPaint = Paint().apply {
                color = circuitColor.copy(alpha = 0.02f) // Whisper-dim — electrons write light
                style = PaintingStyle.Stroke
                strokeWidth = 1.5f
                asFrameworkPaint().apply {
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                }
            }

            val electronGlowPaint = Paint().apply {
                color = circuitColor
                asFrameworkPaint().apply {
                    maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
                }
            }

            val electronCorePaint = Paint().apply {
                color = Color.White
            }

            val chargedPathPaint = Paint().apply {
                color = circuitColor.copy(alpha = 0.6f)
                style = PaintingStyle.Stroke
                strokeWidth = 1.6f
                asFrameworkPaint().apply {
                    maskFilter = BlurMaskFilter(5f, BlurMaskFilter.Blur.NORMAL)
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                }
            }

            paths.forEach { fullPath ->
                // Draw faint guide path
                drawIntoCanvas { canvas ->
                    canvas.drawPath(fullPath, faintPathPaint)
                }

                // Draw charged trail behind electron
                val measure = android.graphics.PathMeasure(fullPath.asAndroidPath(), false)
                if (progress > 0f) {
                    val trailPath = android.graphics.Path()
                    measure.getSegment(0f, measure.length * progress, trailPath, true)
                    drawIntoCanvas { canvas ->
                        canvas.drawPath(trailPath.asComposePath(), chargedPathPaint)
                    }
                }

                // Draw electron at current position
                val pos = FloatArray(2)
                val tangent = FloatArray(2) // required output buffer for getPosTan; direction unused
                measure.getPosTan(measure.length * progress, pos, tangent)

                if (progress > 0f) {
                    drawIntoCanvas { canvas ->
                        canvas.drawCircle(Offset(pos[0], pos[1]), 12f, electronGlowPaint)
                        canvas.drawCircle(Offset(pos[0], pos[1]), 6f, electronCorePaint)
                    }
                }
            }
        }

        // =========================================================================
        // TIER 1: Creative Spaces
        // =========================================================================
        if (progress > 0.8f && hubState == HubState.SPACES) {
            val chargeAlpha = ((progress - 0.8f) * 5f)
            val spacesNodes = listOf(
                HubMenuNode("Image",    -300f, -350f, onClick = { onSpaceSelected("Image") }),
                HubMenuNode("Audio",     320f, -370f, onClick = { onSpaceSelected("Audio") }),
                HubMenuNode("Video",    -280f,  350f, onClick = { onSpaceSelected("Video") }),
                HubMenuNode("Re-Ember",  250f,  350f, onClick = { onSpaceSelected("Re-Ember") })
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(chargeAlpha)
            ) {
                spacesNodes.forEach { node ->
                    HologramNode(
                        text = node.label,
                        isCharged = progress >= 1f,
                        enabled = node.enabled,
                        accentColor = if (node.label == "Re-Ember") Color(0xFFFF8800) else Color(0xFF00FFCC),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(
                                x = (node.offsetX / density).dp,
                                y = (node.offsetY / density).dp
                            ),
                        onClick = node.onClick
                    )
                }
            }
        }

        // =========================================================================
        // TIER 2: Image Space Options
        // =========================================================================
        if (hubState == HubState.SPACE && currentSpace == "Image") {
            val imageNodes = listOf(
                HubMenuNode("Canvas", -300f, -350f, onClick = { onOptionSelected("Canvas") }),
                HubMenuNode("Collection",        320f, -370f, onClick = { onOptionSelected("Collection") }),
                HubMenuNode("Tools",             250f,  350f, onClick = { onOptionSelected("Tools") })
            )

            val unselectedAlpha by animateFloatAsState(
                targetValue = if (selectedOption != null) 0f else 1f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "unselectedFade"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                imageNodes.forEach { node ->
                    val isThisSelected = selectedOption == node.label.lines().first()
                    MenuNode(
                        text = node.label,
                        isCharged = true,
                        isSelected = isThisSelected,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(
                                x = (node.offsetX / density).dp,
                                y = (node.offsetY / density).dp
                            )
                            .alpha(if (isThisSelected || selectedOption == null) 1f else unselectedAlpha),
                        onClick = if (node.enabled) node.onClick else ({})
                    )
                }
            }
        }

        // =========================================================================
        // TIER 2: Audio Space Options
        // =========================================================================
        if (hubState == HubState.SPACE && currentSpace == "Audio") {
            val audioNodes = listOf(
                HubMenuNode("Collection", -300f, -350f, onClick = { onOptionSelected("Collection") }),
                HubMenuNode("Tools",       320f, -370f, onClick = { onOptionSelected("Tools") })
            )

            val unselectedAlpha by animateFloatAsState(
                targetValue = if (selectedOption != null) 0f else 1f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "unselectedFade"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                audioNodes.forEach { node ->
                    val isThisSelected = selectedOption == node.label.lines().first()
                    MenuNode(
                        text = node.label,
                        isCharged = true,
                        isSelected = isThisSelected,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(
                                x = (node.offsetX / density).dp,
                                y = (node.offsetY / density).dp
                            )
                            .alpha(if (isThisSelected || selectedOption == null) 1f else unselectedAlpha),
                        onClick = node.onClick
                    )
                }
            }
        }

        // =========================================================================
        // TIER 2: Video Space Options
        // =========================================================================
        if (hubState == HubState.SPACE && currentSpace == "Video") {
            val videoNodes = listOf(
                HubMenuNode("Collection", -300f, -350f, onClick = { onOptionSelected("Collection") }),
                HubMenuNode("Tools",       320f, -370f, onClick = { onOptionSelected("Tools") })
            )

            val unselectedAlpha by animateFloatAsState(
                targetValue = if (selectedOption != null) 0f else 1f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "unselectedFade"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                videoNodes.forEach { node ->
                    val isThisSelected = selectedOption == node.label.lines().first()
                    MenuNode(
                        text = node.label,
                        isCharged = true,
                        isSelected = isThisSelected,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(
                                x = (node.offsetX / density).dp,
                                y = (node.offsetY / density).dp
                            )
                            .alpha(if (isThisSelected || selectedOption == null) 1f else unselectedAlpha),
                        onClick = node.onClick
                    )
                }
            }
        }

        // =========================================================================
        // TIER 2: Re-Ember Options
        // =========================================================================
        if (hubState == HubState.SPACE && currentSpace == "Re-Ember") {
            val reEmberNodes = listOf(
                HubMenuNode("μEmbers", -300f, -350f, onClick = { onOptionSelected("μEmbers") })
            )

            val unselectedAlpha by animateFloatAsState(
                targetValue = if (selectedOption != null) 0f else 1f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "unselectedFade"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                reEmberNodes.forEach { node ->
                    val isThisSelected = selectedOption == node.label.replace("\n", " ").trim()
                    MenuNode(
                        text = node.label,
                        accentColor = Color(0xFFFF8800),
                        isCharged = true,
                        isSelected = isThisSelected,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(
                                x = (node.offsetX / density).dp,
                                y = (node.offsetY / density).dp
                            )
                            .alpha(if (isThisSelected || selectedOption == null) 1f else unselectedAlpha),
                        onClick = node.onClick
                    )
                }
            }
        }
    }
}

/**
 * A menu node that materializes with a hologram flicker effect.
 * Brief alpha oscillation on entry, then settles into stable charged glow.
 * Offline (disabled) nodes flicker in, then gently fade to dim.
 */
@Composable
fun HologramNode(
    text: String,
    isCharged: Boolean = false,
    enabled: Boolean = true,
    accentColor: Color = Color(0xFF00FFCC),
    modifier: Modifier,
    onClick: () -> Unit
) {
    val isEmber = accentColor != Color(0xFF00FFCC)
    var hasFlickered by remember { mutableStateOf(false) }
    val flickerAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (isEmber) {
            // Re-Ember: slower ignition — the ember takes time to glow
            flickerAlpha.animateTo(0.3f, tween(120))
            flickerAlpha.animateTo(0.1f, tween(80))
            flickerAlpha.animateTo(0.5f, tween(150))
            flickerAlpha.animateTo(0.2f, tween(100))
            flickerAlpha.animateTo(0.7f, tween(200))
            flickerAlpha.animateTo(0.4f, tween(120))
            flickerAlpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        } else {
            // Standard hologram flicker: rapid alpha oscillation then settle
            flickerAlpha.animateTo(0.8f, tween(60))
            flickerAlpha.animateTo(0.2f, tween(50))
            flickerAlpha.animateTo(1f, tween(80))
            flickerAlpha.animateTo(0.5f, tween(40))
            flickerAlpha.animateTo(1f, tween(100))
        }
        hasFlickered = true

        // Offline nodes fade to dim after the flicker settles
        if (!enabled) {
            delay(200.milliseconds) // Brief hold at full — so you see it before it dims
            flickerAlpha.animateTo(0.25f, tween(400, easing = FastOutSlowInEasing))
        }
    }

    MenuNode(
        text = text,
        isCharged = if (hasFlickered && enabled) isCharged else false,
        accentColor = accentColor,
        modifier = modifier.alpha(flickerAlpha.value),
        onClick = if (enabled) onClick else ({})
    )
}

@Composable
fun MenuNode(
    text: String,
    isCharged: Boolean = false,
    isSelected: Boolean = false,
    accentColor: Color = Color(0xFF00FFCC),
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
    val borderColor = accentColor.copy(alpha = borderAlpha.coerceIn(0f, 1f))

    // Selected background tint derives from accent color
    val selectedBgColor = if (accentColor == Color(0xFF00FFCC))
        Color(0xFF0A2020) else Color(0xFF201008)
    
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .background(
                if (isSelected) selectedBgColor.copy(alpha = 0.95f)
                else Color(0xFF111111).copy(alpha = 0.9f),
                RoundedCornerShape(6.dp)
            )
            .then(
                if (isCharged || isSelected) Modifier.background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isSelected) 0.12f else 0.05f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(6.dp)
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Capacitor / Hologram glow border
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
                isCharged -> accentColor
                else -> Color.White.copy(alpha = 0.6f)
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

fun getCircuitPaths(center: Offset): List<Path> {
    // Path 1: Top Left — Image
    val p1 = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x - 60f, center.y - 140f)
        lineTo(center.x - 140f, center.y - 120f)
        lineTo(center.x - 220f, center.y - 320f)
        lineTo(center.x - 250f, center.y - 320f)
    }

    // Path 2: Top Right — Audio
    val p2 = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x + 80f, center.y - 120f)
        lineTo(center.x + 60f, center.y - 220f)
        lineTo(center.x + 240f, center.y - 340f)
        lineTo(center.x + 320f, center.y - 340f)
    }

    // Path 3: Bottom Right — Re-ember
    val p3 = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x + 60f, center.y + 140f)
        lineTo(center.x + 120f, center.y + 180f)
        lineTo(center.x + 220f, center.y + 320f)
        lineTo(center.x + 250f, center.y + 320f)
    }

    // Path 4: Bottom Left — Video
    val p4 = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x - 40f, center.y + 140f)
        lineTo(center.x - 120f, center.y + 220f)
        lineTo(center.x - 240f, center.y + 320f)
        lineTo(center.x - 280f, center.y + 320f)
    }

    return listOf(p1, p2, p3, p4)
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
