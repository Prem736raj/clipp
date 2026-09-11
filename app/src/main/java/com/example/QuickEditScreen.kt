package com.example

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEditScreen(videoUri: String?, onClose: () -> Unit, onExportComplete: () -> Unit) {
    val context = LocalContext.current
    val exporter = remember { VideoExporter(context) }
    var sourceClip by remember { mutableStateOf<MediaClip?>(null) }
    var sourceError by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var outputMetadata by remember { mutableStateOf<ExportMetadata?>(null) }
    var activeExportHandle by remember { mutableStateOf<ExportHandle?>(null) }

    DisposableEffect(exporter) {
        onDispose { exporter.close() }
    }

    LaunchedEffect(videoUri) {
        sourceClip = null
        sourceError = null
        val parsedUri = videoUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (parsedUri == null) {
            sourceError = "No video was supplied"
        } else {
            val metadata = MediaMetadataReader.read(context, parsedUri)
            if (metadata == null || !metadata.mimeType.startsWith("video/")) {
                sourceError = "The supplied file is not a readable video"
            } else {
                sourceClip = MediaClip(
                    sourceUri = parsedUri.toString(),
                    originalDurationMs = metadata.durationMs,
                    trimEndMs = metadata.durationMs
                )
            }
        }
    }

    val publishedUri = outputUri
    val publishedMetadata = outputMetadata
    fun cancelExport() {
        activeExportHandle?.cancel()
        activeExportHandle = null
        isExporting = false
        Toast.makeText(context, "Export cancelled", Toast.LENGTH_SHORT).show()
    }

    if (publishedUri != null && publishedMetadata != null) {
        ExportSuccessScreen(
            outputUri = publishedUri,
            thumbnailUri = videoUri,
            videoDurationMs = publishedMetadata.durationMs,
            fileSizeBytes = publishedMetadata.fileSizeBytes,
            onClose = onExportComplete
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Quick Edit") },
                navigationIcon = {
                    IconButton(onClick = { if (isExporting) cancelExport() else onClose() }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = if (isExporting) "Cancel export" else "Close"
                        )
                    }
                }
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (sourceClip != null) {
                        Text(
                            "Source video loaded\n${formatExportDuration(sourceClip!!.originalDurationMs)}",
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else {
                        Text(sourceError ?: "Opening video...", color = Color.White)
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Info, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Quick Edit currently exports the original video as a verified MP4. Trimming and visual tools will be enabled when their renderer is ready.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (isExporting) {
                    Text("Rendering MP4... $progress%", fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Button(
                    onClick = {
                        val clip = sourceClip ?: return@Button
                        isExporting = true
                        progress = 0
                        activeExportHandle = exporter.export(
                            clips = listOf(clip),
                            onProgress = { progress = it },
                            onSuccess = { uri, metadata ->
                                isExporting = false
                                activeExportHandle = null
                                outputUri = uri
                                outputMetadata = metadata
                                com.example.utils.AnalyticsManager.trackVideoExported(metadata.durationMs)
                            },
                            onError = { message ->
                                isExporting = false
                                activeExportHandle = null
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = sourceClip != null && !isExporting
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isExporting) "Rendering..." else "Export MP4")
                }
                OutlinedButton(
                    onClick = { if (isExporting) cancelExport() else onClose() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (isExporting) Icons.Filled.Close else Icons.Filled.ContentCut,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isExporting) "Cancel export" else "Back")
                }
            }
        }
    }
}
