package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
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

val categories = listOf("Local")

@Composable
fun TemplatesScreen(
    onUseTemplate: (VideoTemplate) -> Unit = {},
    onSlideshowCreator: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Templates", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Choose a local starting layout, then replace its media and text on this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(TemplateRepo.templates, key = { it.id }) { template ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUseTemplate(template) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(template.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${template.slots} media slot(s) · ${template.duration}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(template.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { onUseTemplate(template) }) {
                        Text("Use")
                    }
                }
            }
        }
        item {
            Button(onClick = onSlideshowCreator, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create slideshow from photos")
            }
        }
        item { Spacer(Modifier.padding(bottom = 12.dp)) }
    }
}

object TemplateRepo {
    val templates: List<VideoTemplate> = listOf(
        VideoTemplate(
            id = "local_clean_slideshow",
            title = "Clean Slideshow",
            category = "Local",
            imageUrl = "",
            duration = "Auto duration",
            uses = 0,
            slots = 3,
            description = "Simple title and subtitle overlays with clean cuts and gentle fades."
        ),
        VideoTemplate(
            id = "local_cinematic_story",
            title = "Cinematic Story",
            category = "Local",
            imageUrl = "",
            duration = "Auto duration",
            uses = 0,
            slots = 4,
            description = "Vintage color, letterbox bars, title motion, and cinematic fades."
        ),
        VideoTemplate(
            id = "local_title_intro",
            title = "Title Intro",
            category = "Local",
            imageUrl = "",
            duration = "Auto duration",
            uses = 0,
            slots = 3,
            description = "A bold title and subtitle intro over your selected media."
        )
    )

    fun find(templateId: String): VideoTemplate? = templates.firstOrNull { it.id == templateId }
}
