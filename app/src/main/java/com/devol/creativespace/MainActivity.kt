package com.devol.creativespace

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
import com.devol.creativespace.ui.CanvasScreen
import com.devol.creativespace.ui.CollectionScreen
import com.devol.creativespace.ui.EmberDetailScreen
import com.devol.creativespace.ui.HubPortalScreen
import com.devol.creativespace.ui.HubState
import com.devol.creativespace.ui.ReEmberScreen
import com.devol.creativespace.ui.ToolsScreen
import com.devol.creativespace.viewmodel.MotionViewModel
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

                // Which ember bundle to show in detail view
                var activeEmberBundle by remember { mutableStateOf("") }

                // =========================================================================
                // Back navigation — Option B: return to previous space
                //
                // When leaving an endpoint (Canvas, Collection, Tools, etc.),
                // back restores the hub to the space you came from. This preserves
                // context — you can quickly navigate to another option within
                // the same space without re-discovering from IDLE.
                //
                // IDLE is always one more tap/back away from SPACES.
                // =========================================================================
                val returnToSpace: () -> Unit = {
                    currentScreen = "PORTAL"
                    // Restore hub state to show the space menu for where we came from
                    hubState = HubState.SPACE
                }

                when (currentScreen) {
                    "PORTAL" -> {
                        HubPortalScreen(
                            hubState = hubState,
                            onHubStateChange = { hubState = it },
                            initialSpace = if (hubState == HubState.SPACE) activeSpace else null,
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
                        // Back returns to space menu — strokes are preserved until explicitly cleared
                        BackHandler { returnToSpace() }
                        CanvasScreen(
                            viewModel = motionViewModel,
                            onNavigateHome = { returnToSpace() }
                        )
                    }
                    "COLLECTION" -> {
                        BackHandler { returnToSpace() }
                        CollectionScreen(
                            viewModel = motionViewModel,
                            spaceType = activeSpace,
                            onNavigateHome = { returnToSpace() },
                            onSendToCanvas = if (activeSpace == "Image") { file ->
                                motionViewModel.setBackgroundImage(file)
                                currentScreen = "CANVAS"
                            } else null
                        )
                    }
                    "TOOLS" -> {
                        BackHandler { returnToSpace() }
                        ToolsScreen(
                            spaceType = activeSpace,
                            onNavigateHome = { returnToSpace() }
                        )
                    }
                    "RE_EMBER" -> {
                        BackHandler { returnToSpace() }
                        ReEmberScreen(
                            viewModel = motionViewModel,
                            onNavigateHome = { returnToSpace() },
                            onOpenBundle = { bundleName ->
                                activeEmberBundle = bundleName
                                currentScreen = "EMBER_DETAIL"
                            }
                        )
                    }
                    "EMBER_DETAIL" -> {
                        BackHandler { currentScreen = "RE_EMBER" }
                        EmberDetailScreen(
                            viewModel = motionViewModel,
                            bundleName = activeEmberBundle,
                            onNavigateBack = { currentScreen = "RE_EMBER" }
                        )
                    }
                }
            }
        }
    }
}
