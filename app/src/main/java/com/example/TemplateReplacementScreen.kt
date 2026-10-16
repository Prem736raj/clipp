package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.ProjectEntity
import com.example.viewmodel.ProjectViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateReplacementScreen(
    templateId: String,
    onBack: () -> Unit,
    projectViewModel: ProjectViewModel,
    onCustomize: (ProjectEntity) -> Unit
) {
    val template = remember(templateId) { TemplateRepo.find(templateId) }
    if (template == null) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Template not found", style = MaterialTheme.typography.titleLarge)
                Text("Choose one of Clipp's built-in local templates.")
            }
        }
        return
    }

    var selectedMedia by remember(templateId) { mutableStateOf<List<String>>(emptyList()) }
    var titleText by remember(templateId) {
        mutableStateOf(template.textPlaceholders.getOrNull(0) ?: "Title Here")
    }
    var subtitleText by remember(templateId) {
        mutableStateOf(template.textPlaceholders.getOrNull(1) ?: "Subtitle Here")
    }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (selectedMedia.isEmpty()) {
        MediaPickerScreen(
            onClose = onBack,
            onGoToEditor = {},
            projectViewModel = projectViewModel,
            isSelectingForExisting = true,
            onMediaSelected = { uris ->
                selectedMedia = uris.distinct().take(template.slots.coerceAtLeast(1))
                errorMessage = null
            }
        )
        return
    }

    val textValues = listOf(titleText, subtitleText)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(template.title) },
                navigationIcon = {
                    IconButton(onClick = { selectedMedia = emptyList() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Choose different media")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (isCreating) return@Button
                    isCreating = true
                    errorMessage = null
                    scope.launch {
                        val project = TemplateProjectFactory.build(
                            context = context,
                            template = template,
                            mediaUris = selectedMedia,
                            textValues = textValues
                        )
                        if (project == null) {
                            errorMessage = "The selected media could not be read. Choose supported photos or videos again."
                            isCreating = false
                        } else {
                            onCustomize(project)
                        }
                    }
                },
                enabled = !isCreating,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Text("Create local project", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(template.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Using ${selectedMedia.size} of ${template.slots} available media slot(s). All media stays on this device.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedMedia) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected template media",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(92.dp).clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }
            item {
                OutlinedButton(onClick = { selectedMedia = emptyList() }) {
                    Text("Choose different media")
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Template text", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = subtitleText,
                        onValueChange = { subtitleText = it },
                        label = { Text("Subtitle") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            if (errorMessage != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
