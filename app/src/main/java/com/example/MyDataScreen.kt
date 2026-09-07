package com.example

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ProjectEntity
import com.example.data.ProjectStorage
import com.example.viewmodel.ProjectViewModel
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDataScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val projectViewModel: ProjectViewModel = viewModel()
    val projects by projectViewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val totalBytes = projects.sumOf { ProjectStorage.calculateOwnedSizeBytes(context, it) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { destination ->
        if (destination == null) return@rememberLauncherForActivityResult

        val result = runCatching {
            val payload = JSONObject().apply {
                put("app", "Clipp")
                put("exportedAt", System.currentTimeMillis())
                put("storage", "local_only")
                put("projects", JSONArray().apply {
                    projects.forEach { project -> put(project.toExportJson(context)) }
                })
            }.toString(2)

            val output = context.contentResolver.openOutputStream(destination)
                ?: error("Could not open the selected destination")
            output.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        }

        Toast.makeText(
            context,
            if (result.isSuccess) "Data exported successfully" else "Could not export data",
            Toast.LENGTH_LONG
        ).show()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("My Data", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Projects are kept on this device until you delete them. Clipp does not upload them in this build. Original gallery media is never deleted by Clipp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Local Storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                SettingsItem(
                    icon = Icons.Filled.SdStorage,
                    title = "Local project data",
                    subtitle = "${projects.size} project(s) · ${formatDataSize(totalBytes)} of app-owned state",
                    onClick = { }
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    "Manage Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = { exportLauncher.launch("clipp-data.json") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Export My Data")
                }
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Delete All Local Data")
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { if (!isDeleting) showDeleteConfirm = false },
                title = { Text("Delete all local data?") },
                text = {
                    Text(
                        "This deletes Clipp projects, app-owned thumbnails, caches, and settings from this device. Source media in your gallery is not deleted."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isDeleting = true
                            projectViewModel.deleteAllLocalData {
                                clearLegacyPreferences(context)
                                isDeleting = false
                                showDeleteConfirm = false
                                Toast.makeText(
                                    context,
                                    "Local Clipp data deleted",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        enabled = !isDeleting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (isDeleting) "Deleting..." else "Delete everything")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirm = false },
                        enabled = !isDeleting
                    ) { Text("Cancel") }
                }
            )
        }
    }
}

private fun ProjectEntity.toExportJson(context: Context): JSONObject {
    val actualSize = ProjectStorage.calculateOwnedSizeBytes(context, this)
    return JSONObject().apply {
        put("id", id)
        put("name", name)
        put("duration", duration)
        put("createdAt", creationDate)
        put("lastEdited", lastEdited)
        put("aspectRatio", aspectRatio)
        put("resolution", resolution)
        put("frameRate", frameRate)
        put("projectState", projectState)
        put("historyState", historyState)
        put("folderId", folderId)
        put("tags", tags)
        put("isArchived", isArchived)
        put("sizeBytes", actualSize)
        put("sourceMediaPaths", JSONArray().apply {
            sourceMediaPaths.forEach { path -> put(path) }
        })
    }
}

private fun clearLegacyPreferences(context: Context) {
    listOf(
        "clipp_prefs",
        "clipp_settings",
        "clipp_billing",
        "clipp_auth_prefs",
        "clipp_privacy",
        "clipp_stats",
        "clipp_thumb_cache",
        "clipp_crash_prefs"
    )
        .forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
        }
}

private fun formatDataSize(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    bytes < 1024L * 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB"
    else -> "${bytes / (1024L * 1024L * 1024L)} GB"
}
