package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class VideoTemplate(
    val id: String,
    val title: String,
    val category: String,
    val imageUrl: String,
    val duration: String,
    val uses: Int,
    val slots: Int = 3,
    val textPlaceholders: List<String> = listOf("Title Here", "Subtitle Here"),
    val description: String = "",
    val isCustom: Boolean = false,
    val version: Int = 1,
    val originalId: String? = null
)

// Kept for the editor's compatibility UI; only a future persisted template
// implementation should populate this list.
val categories = listOf("My Templates")

@Composable
fun TemplatesScreen(
    onUseTemplate: (VideoTemplate) -> Unit = {},
    onSlideshowCreator: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Templates", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Text("Template library is not available yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Clipp does not display fabricated templates, usage counts, or remote preview media. Choose your own media to create a local project.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(onClick = onSlideshowCreator, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text("Create slideshow from photos", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

object TemplateRepo {
    val templates: List<VideoTemplate> = emptyList()

    fun addTemplate(template: VideoTemplate) {
        // Template serialization and persistence are intentionally not exposed
        // until the format can be imported and rendered without data loss.
    }
}
