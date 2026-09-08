package com.example

import android.os.Build
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class FAQItem(val question: String, val answer: String, val category: String)

val FAQs = listOf(
    FAQItem("How do I export a video?", "Open Export in the editor and tap Export MP4. The renderer supports source clips, trims, still-image durations, speed, crop, filters, basic blur, mirror, shake, comic-book/pencil-sketch/pop-art style effects, rotation/flip, clip positioning and transform keyframes, volume, volume keyframes and fade envelopes, static text, captions, stickers, drawings, frames, fade/scale/rotate/pulse/wave/swing text and sticker animation, fade/scale image-overlay animation, image overlays, photo-overlay chroma key, supported fades, and separate audio tracks. Partial-duration visual effects, advanced animated effects, video overlays/video-overlay chroma key, and advanced audio processors are still blocked.", "Exporting"),
    FAQItem("Does Clipp add a watermark?", "The current basic renderer does not add a watermark. Subscriptions and paid tiers are not active in this build.", "Exporting"),
    FAQItem("Can I add music?", "Yes. Use Music to select an audio file, or Voiceover to record from the microphone. The selected track is saved in the local project and mixed into MP4 export.", "Editing"),
    FAQItem("How do local templates work?", "Choose one of the built-in templates, select the media from your device, customize the title and subtitle, and create the project. The template state is saved locally and can be edited before export. Remote template catalogs and AI-generated templates are not active in this build.", "Editing"),
    FAQItem("How do I add captions?", "Static caption segments are rendered into export. Automatic speech transcription is not active in this build, so captions must come from an existing project state.", "Editing"),
    FAQItem("Are AI editing tools available?", "AI enhancement, object removal, background removal, smart reframe, and related processing are disabled until they can be implemented and validated locally.", "Editing"),
    FAQItem("How do I split a clip?", "Move the playhead to the desired position on the timeline and tap the 'Split' tool.", "Editing"),
    FAQItem("Why did my export fail?", "Check that each source URI is still readable, that there is enough free storage, and that the project contains no blocked animated effect, video overlay, unsupported transition, or advanced audio automation. The export screen reports the exact limitation.", "Troubleshooting"),
    FAQItem("Can I cancel a subscription?", "There is no active subscription catalog or billing flow in this build.", "Account"),
    FAQItem("Where are my projects saved?", "Projects are saved locally on your device. Ensure you have enough storage space.", "Account")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(onClose: () -> Unit, onOpenContact: () -> Unit, onOpenReportBug: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    
    val categories = listOf("All") + FAQs.map { it.category }.distinct()
    
    val filteredFAQs = FAQs.filter { 
        (selectedCategory == "All" || it.category == selectedCategory) &&
        (it.question.contains(searchQuery, ignoreCase = true) || it.answer.contains(searchQuery, ignoreCase = true))
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("Help Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
            
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        placeholder = { Text("Search help articles...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onOpenContact, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Contact Us")
                        }
                        Button(onClick = onOpenReportBug, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                            Icon(Icons.Filled.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Report Bug")
                        }
                    }
                }
                
                item {
                    ScrollableTabRow(
                        selectedTabIndex = categories.indexOf(selectedCategory),
                        edgePadding = 0.dp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        categories.forEachIndexed { index, category ->
                            Tab(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                text = { Text(category) }
                            )
                        }
                    }
                }
                
                items(filteredFAQs) { faq ->
                    FAQCard(faq)
                }
                
                if (filteredFAQs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No articles found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun FAQCard(faq: FAQItem) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(faq.question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(faq.answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
