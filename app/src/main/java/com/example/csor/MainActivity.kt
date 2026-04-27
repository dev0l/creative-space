package com.example.csor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.csor.ui.CanvasScreen
import com.example.csor.ui.HubPortalScreen
import com.example.csor.viewmodel.MotionViewModel
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge immersive mode
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf("PORTAL") }
                val motionViewModel: MotionViewModel = viewModel()

                when (currentScreen) {
                    "PORTAL" -> HubPortalScreen(
                        onNavigateToCanvas = { currentScreen = "CANVAS" }
                    )
                    "CANVAS" -> CanvasScreen(
                        viewModel = motionViewModel,
                        onNavigateHome = { 
                            currentScreen = "PORTAL" 
                            motionViewModel.clearCanvas()
                        }
                    )
                }
            }
        }
    }
}