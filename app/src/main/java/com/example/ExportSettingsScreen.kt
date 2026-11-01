package com.example

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ExportSettingsScreen(
    clips: List<MediaClip>,
    editorState: EditorState = EditorState(clips = clips),
    videoDurationMs: Long,
    thumbnailUri: String?,
    onClose: () -> Unit,
    onExportComplete: () -> Unit
) {
    val context = LocalContext.current
    val exporter = remember { VideoExporter(context) }
    var isExporting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var outputMetadata by remember { mutableStateOf<ExportMetadata?>(null) }
    var activeExportHandle by remember { mutableStateOf<ExportHandle?>(null) }
    var resolutionName by rememberSaveable { mutableStateOf(ExportResolution.HD.name) }
    var frameRate by rememberSaveable { mutableStateOf(30) }
    var videoBitrate by rememberSaveable { mutableStateOf(5_000_000) }
    var initializedQuality by rememberSaveable { mutableStateOf(false) }
    val settings = ExportSettings(ExportResolution.valueOf(resolutionName), frameRate, videoBitrate)
    val aspectRatio = editorState.canvasSettings.aspectOption.ratio
    val qualitySupport by produceState<ExportQualitySupport?>(null, aspectRatio) {
        value = withContext(Dispatchers.IO) { inspectExportQualitySupport(aspectRatio) }
    }
    fun selectQuality(value: ExportSettings) {
        resolutionName = value.resolution.name
        frameRate = value.frameRate
        videoBitrate = value.videoBitrate
        errorMessage = null
    }
    LaunchedEffect(qualitySupport) {
        if (!initializedQuality) {
            qualitySupport?.defaultSettings()?.let {
                selectQuality(it)
                initializedQuality = true
            }
        }
    }

    DisposableEffect(exporter) {
        onDispose { exporter.close() }
    }

    val uri = outputUri
    val metadata = outputMetadata
    val unsupportedReasons = editorState.exportUnsupportedReasons(settings)
    val deviceReadiness = remember(editorState) {
        inspectExportDeviceReadiness(context, editorState)
    }
    val qualityBlockingReason = qualitySupport?.blockingReason(settings)
    fun cancelExport() {
        activeExportHandle?.cancel()
        activeExportHandle = null
        isExporting = false
        errorMessage = "Export cancelled"
    }

    if (uri != null && metadata != null) {
        ExportSuccessScreen(
            outputUri = uri,
            thumbnailUri = thumbnailUri,
            videoDurationMs = metadata.durationMs,
            fileSizeBytes = metadata.fileSizeBytes,
            onClose = onExportComplete
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Export") },
                navigationIcon = {
                    IconButton(onClick = { if (isExporting) cancelExport() else onClose() }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = if (isExporting) "Cancel export" else "Close"
                        )
                    }
                }
            )
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("MP4 export", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Choose the quality and size of your finished video.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    ExportQualityControls(
                        settings = settings,
                        support = qualitySupport,
                        enabled = !isExporting,
                        onSettingsChanged = ::selectQuality
                    )
                }
                item {
                    val dimensions = settings.dimensions(aspectRatio)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${dimensions.width} × ${dimensions.height} · ${editorState.canvasSettings.aspectOption.label}",
                                fontWeight = FontWeight.SemiBold)
                            Text("${clips.size} clips · ${formatExportDuration(videoDurationMs)} · MP4 (H.264)")
                            Text("Estimated size: ${android.text.format.Formatter.formatShortFileSize(context,
                                settings.estimatedBytes(videoDurationMs, editorState.requiresAudioEncoder()))}")
                            Text("Actual file size varies with your video. Empty canvas space is filled with black.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (unsupportedReasons.isNotEmpty() || qualityBlockingReason != null) {
                    item {
                        Text(qualityBlockingReason ?: "Remove unsupported edits before exporting: ${unsupportedReasons.distinct().joinToString(", ")}",
                            color = MaterialTheme.colorScheme.error)
                    }
                }
                item {
                    val blocked = deviceReadiness.blockingReason
                    Surface(
                        color = if (blocked != null) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Before you export", fontWeight = FontWeight.SemiBold)
                            Text(
                                blocked ?: "Keep Clipp open until your video is saved.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (blocked != null) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            deviceReadiness.warnings.forEach { warning ->
                                Text(
                                    warning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (errorMessage != null) {
                    item {
                        Text(
                            errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (isExporting) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Rendering MP4... $progress%", fontWeight = FontWeight.SemiBold)
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Keep Clipp open until rendering finishes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(onClick = ::cancelExport) {
                                Text("Cancel export")
                            }
                        }
                    }
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        errorMessage = null
                        progress = 0
                        isExporting = true
                        activeExportHandle = exporter.export(
                            clips = clips,
                            editorState = editorState,
                            exportSettings = settings,
                            onProgress = { progress = it },
                            onSuccess = { publishedUri, metadataValue ->
                                isExporting = false
                                activeExportHandle = null
                                outputUri = publishedUri
                                outputMetadata = metadataValue
                                com.example.utils.AnalyticsManager.trackVideoExported(metadataValue.durationMs)
                            },
                            onError = { message ->
                                isExporting = false
                                activeExportHandle = null
                                errorMessage = message
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
                    enabled = !isExporting && clips.isNotEmpty() && unsupportedReasons.isEmpty() &&
                        qualitySupport != null && qualityBlockingReason == null && deviceReadiness.blockingReason == null
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isExporting) "Rendering..." else "Export MP4", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun ExportQualityControls(
    settings: ExportSettings,
    support: ExportQualitySupport?,
    enabled: Boolean,
    onSettingsChanged: (ExportSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (support == null) Text("Checking available quality options…")
        Text("Resolution", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ExportResolution.entries) { resolution ->
                val choice = support?.settingsForResolution(resolution, settings.frameRate)
                FilterChip(
                    selected = settings.resolution == resolution,
                    enabled = enabled && choice != null,
                    onClick = { choice?.let(onSettingsChanged) },
                    label = { Text(resolution.label) }
                )
            }
        }
        Text("Frame rate limit", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(EXPORT_FRAME_RATES) { rate ->
                val choice = support?.preferredSettings(settings.resolution, rate, settings.videoBitrate)
                FilterChip(
                    selected = settings.frameRate == rate,
                    enabled = enabled && choice != null,
                    onClick = { choice?.let(onSettingsChanged) },
                    label = { Text("$rate fps") }
                )
            }
        }
        Text("Lower frame rates in source videos are preserved. Photos use the selected rate.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Video bitrate", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(EXPORT_VIDEO_BITRATES) { bitrate ->
                val choice = settings.copy(videoBitrate = bitrate)
                FilterChip(
                    selected = settings.videoBitrate == bitrate,
                    enabled = enabled && support?.supportedSettings?.contains(choice) == true,
                    onClick = { onSettingsChanged(choice) },
                    label = { Text("${bitrate / 1_000_000} Mbps") }
                )
            }
        }
        Text("Higher bitrates preserve more detail and make larger files. Unavailable options are disabled.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun formatExportDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
