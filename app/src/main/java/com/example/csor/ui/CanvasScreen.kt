package com.example.csor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csor.sensors.MotionMapper
import com.example.csor.viewmodel.MotionViewModel

@Composable
fun CanvasScreen(viewModel: MotionViewModel, onNavigateHome: () -> Unit) {
    val context = LocalContext.current
    val completedStrokes by viewModel.completedStrokes.collectAsState()
    val currentStroke by viewModel.currentStroke.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()

    DisposableEffect(Unit) {
        val mapper = MotionMapper(context, viewModel)
        mapper.start()
        
        if (!viewModel.isTracking.value) {
            viewModel.toggleTracking()
        }
        
        onDispose {
            mapper.stop()
            if (viewModel.isTracking.value) {
                viewModel.toggleTracking()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFCFBF4))) {
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            viewModel.setCanvasSize(size.width, size.height)

            val strokeParams = Stroke(
                width = 8f,
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
                drawPath(path, color = Color(0xFF333333), style = strokeParams)
            }

            if (currentStroke.isNotEmpty()) {
                val path = Path().apply {
                    moveTo(currentStroke.first().x, currentStroke.first().y)
                    for (i in 1 until currentStroke.size) {
                        lineTo(currentStroke[i].x, currentStroke[i].y)
                    }
                }
                drawPath(path, color = Color(0xFF333333), style = strokeParams)
                
                val lastPoint = currentStroke.last()
                drawCircle(color = Color(0xFF00AA00), radius = 6f, center = lastPoint)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateHome) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.DarkGray)
                }
                Text("Free Canvas", fontSize = 14.sp, color = Color.Gray)
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (completedStrokes.isEmpty() && currentStroke.size < 10) {
                Text(
                    "Move your phone to draw in space",
                    color = Color.LightGray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
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
                    onClick = { /* Dummy Save */ },
                    containerColor = Color(0xFF1A1A1A),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Save", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
