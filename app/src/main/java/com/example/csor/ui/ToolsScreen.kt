package com.example.csor.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tools screen — dark-themed link launcher for favourite AI generators.
 * Each creative space has its own curated set of tools.
 * Tapping a link opens it in the browser — the bridge out to other territories.
 */

data class AiTool(
    val name: String,
    val description: String,
    val url: String
)

private val imageTools = listOf(
    AiTool("ChatGPT · DALL-E", "Conversational image generation", "https://chat.openai.com"),
    AiTool("Midjourney", "Artistic image synthesis", "https://midjourney.com"),
    AiTool("Leonardo.AI", "Creative image generation & editing", "https://leonardo.ai"),
    AiTool("Ideogram", "Text-in-image generation", "https://ideogram.ai")
)

private val audioTools = listOf(
    AiTool("Suno", "AI music generation from prompts", "https://suno.com"),
    AiTool("Udio", "AI-powered music creation", "https://udio.com"),
    AiTool("ElevenLabs", "Voice synthesis & audio AI", "https://elevenlabs.io")
)

private val videoTools = listOf(
    AiTool("Runway", "AI video generation & editing", "https://runwayml.com"),
    AiTool("Pika", "Text-to-video creation", "https://pika.art"),
    AiTool("Luma", "Dream Machine — AI video", "https://lumalabs.ai")
)

@Composable
fun ToolsScreen(
    spaceType: String,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val tools = when (spaceType) {
        "Image" -> imageTools
        "Audio" -> audioTools
        "Video" -> videoTools
        else -> imageTools
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
                    "$spaceType Tools",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFF00FFCC).copy(alpha = 0.8f),
                    fontFamily = FontFamily.SansSerif
                )

                Spacer(modifier = Modifier.width(48.dp))
            }

            // =========================================================================
            // TOOLS LIST
            // =========================================================================
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                items(tools) { tool ->
                    ToolCard(tool = tool) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tool.url))
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
}

@Composable
fun ToolCard(tool: AiTool, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                tool.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                tool.description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f),
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Open ${tool.name}",
            tint = Color(0xFF00FFCC).copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
