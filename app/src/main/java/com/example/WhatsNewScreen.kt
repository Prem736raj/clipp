package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ChangelogEntry(val version: String, val date: String, val changes: List<String>)

val changelog = listOf(
    ChangelogEntry("v1.0.0", "Today", listOf("Initial release!", "Core video editing features", "AI Auto Captions & Enhancer", "Pro subscriptions unlocked")),
    ChangelogEntry("Beta 0.9", "Last Week", listOf("Performance improvements", "Fixed export crash on budget devices", "Added new audio effects")),
    ChangelogEntry("Beta 0.8", "2 Weeks Ago", listOf("Added community transitions", "UI improvements for Editor", "Background removal beta"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("What's New", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
            
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                            Icon(Icons.Filled.NewReleases, contentDescription = null, modifier = Modifier.padding(16.dp).size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Clipp Updates", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("See what we've been building for you.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                items(changelog) { entry ->
                    ChangelogItem(entry)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ChangelogItem(entry: ChangelogEntry) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(entry.version, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(entry.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        entry.changes.forEach { change ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(change, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
