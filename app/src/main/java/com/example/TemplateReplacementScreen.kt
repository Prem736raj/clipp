package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.viewmodel.ProjectViewModel

/**
 * Template replacement is kept as a safe route target while the template
 * catalog and renderer are unavailable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateReplacementScreen(
    templateId: String,
    onBack: () -> Unit,
    projectViewModel: ProjectViewModel,
    onCustomize: (VideoTemplate, List<String>, List<String>) -> Unit,
    onQuickExport: (VideoTemplate, List<String>, List<String>) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Template") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Text("Templates are not available yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Clipp will only enable templates after their media replacement, preview, and export state can be restored reliably.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
