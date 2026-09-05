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
    FAQItem("How do I export a video?", "Tap the export button in the top right corner of the editor. Choose your resolution and frame rate, then tap 'Export'.", "Exporting"),
    FAQItem("How do I remove the watermark?", "Pro users can remove the watermark in the export settings before exporting their video.", "Exporting"),
    FAQItem("How do I add music?", "Tap the 'Audio' tool in the bottom menu, then select 'Sound FX' or 'AI Music' to add an audio track.", "Editing"),
    FAQItem("Can I use my own music?", "Yes! Tap 'Audio', select 'Local Files', and pick a track from your device.", "Editing"),
    FAQItem("How does Auto Caption work?", "Auto Caption uses AI to analyze your video's audio and automatically generates text captions. Tap 'Captions' -> 'Auto Caption'.", "AI Features"),
    FAQItem("Is AI Enhancer free to use?", "AI Enhancer is available to all users, but free users have a daily limit. Pro users get unlimited access.", "AI Features"),
    FAQItem("How do I split a clip?", "Move the playhead to the desired position on the timeline and tap the 'Split' tool.", "Editing"),
    FAQItem("Why did my export fail?", "Export failures can happen due to lack of storage space or memory. Try clearing cache in Settings or lowering export resolution.", "Troubleshooting"),
    FAQItem("How do I cancel my subscription?", "Go to Profile -> Subscription -> Manage Subscription. It will redirect you to Google Play.", "Account"),
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
                    Spacer(Modifier.height(16.dp))
                    Text("Tutorials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TutorialCard(modifier = Modifier.weight(1f), title = "Getting Started", icon = Icons.Filled.PlayCircle)
                        TutorialCard(modifier = Modifier.weight(1f), title = "Advanced Editing", icon = Icons.Filled.PlayCircle)
                        TutorialCard(modifier = Modifier.weight(1f), title = "AI Features", icon = Icons.Filled.AutoAwesome)
                    }
                    Spacer(Modifier.height(16.dp))
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
fun TutorialCard(modifier: Modifier = Modifier, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = modifier.height(100.dp).clickable { },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
