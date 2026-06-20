package com.example.csor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.csor.ui.CanvasScreen
import com.example.csor.ui.CollectionScreen
import com.example.csor.ui.HubPortalScreen
import com.example.csor.ui.HubState
import com.example.csor.ui.ReEmberScreen
import com.example.csor.ui.ToolsScreen
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

                // Hoisted so HubPortalScreen can receive and propagate state
                // across recompositions. Back navigation is owned by HubPortalScreen.
                var hubState by remember { mutableStateOf(HubState.IDLE) }

                // Which creative space the user committed from (Image, Audio, Video, Re-Ember)
                var activeSpace by remember { mutableStateOf("Image") }

                when (currentScreen) {
                    "PORTAL" -> {
                        HubPortalScreen(
                            hubState = hubState,
                            onHubStateChange = { hubState = it },
                            onSpaceCommit = { space, option ->
                                activeSpace = space
                                when (option) {
                                    "Canvas" -> currentScreen = "CANVAS"
                                    "Collection" -> currentScreen = "COLLECTION"
                                    "Tools" -> currentScreen = "TOOLS"
                                    "μEmbers" -> currentScreen = "RE_EMBER"
                                    else -> currentScreen = "CANVAS"
                                }
                            }
                        )
                    }
                    "CANVAS" -> {
                        // Back returns to Portal — strokes are preserved until explicitly cleared
                        BackHandler { currentScreen = "PORTAL" }
                        CanvasScreen(
                            viewModel = motionViewModel,
                            onNavigateHome = { currentScreen = "PORTAL" }
                        )
                    }
                    "COLLECTION" -> {
                        BackHandler { currentScreen = "PORTAL" }
                        CollectionScreen(
                            viewModel = motionViewModel,
                            spaceType = activeSpace,
                            onNavigateHome = { currentScreen = "PORTAL" },
                            onSendToCanvas = if (activeSpace == "Image") { file ->
                                motionViewModel.setBackgroundImage(file)
                                currentScreen = "CANVAS"
                            } else null
                        )
                    }
                    "TOOLS" -> {
                        BackHandler { currentScreen = "PORTAL" }
                        ToolsScreen(
                            spaceType = activeSpace,
                            onNavigateHome = { currentScreen = "PORTAL" }
                        )
                    }
                    "RE_EMBER" -> {
                        BackHandler { currentScreen = "PORTAL" }
                        ReEmberScreen(
                            viewModel = motionViewModel,
                            onNavigateHome = { currentScreen = "PORTAL" }
                        )
                    }
                }
            }
        }
    }
}
