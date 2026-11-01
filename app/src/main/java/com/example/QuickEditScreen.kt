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
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import kotlin.math.max
import kotlin.math.min

internal const val QUICK_TRIM_MIN_DURATION_MS = 250L
internal val QUICK_EDIT_SPEED_PRESETS = listOf(0.5f, 1f, 1.5f, 2f)

internal data class QuickTrimSelection(
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

internal fun normalizeQuickTrimSelection(
    durationMs: Long,
    requestedStartMs: Long,
    requestedEndMs: Long,
    minimumDurationMs: Long = QUICK_TRIM_MIN_DURATION_MS
): QuickTrimSelection {
    val safeDurationMs = durationMs.coerceAtLeast(0L)
    if (safeDurationMs == 0L) return QuickTrimSelection(0L, 0L)

    val safeMinimumMs = minimumDurationMs
        .coerceAtLeast(1L)
        .coerceAtMost(safeDurationMs)
    val clampedStartMs = requestedStartMs.coerceIn(0L, safeDurationMs)
    val clampedEndMs = requestedEndMs.coerceIn(0L, safeDurationMs)
    var startMs = min(clampedStartMs, clampedEndMs)
    var endMs = max(clampedStartMs, clampedEndMs)

    if (endMs - startMs < safeMinimumMs) {
        if (startMs + safeMinimumMs <= safeDurationMs) {
            endMs = startMs + safeMinimumMs
        } else {
            startMs = (safeDurationMs - safeMinimumMs).coerceAtLeast(0L)
            endMs = safeDurationMs
        }
    }

    return QuickTrimSelection(startMs, endMs)
}

internal fun normalizeQuickRotation(rotation: Float): Float {
    val normalized = rotation % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}

internal fun quickEditOutputDurationMs(sourceDurationMs: Long, playbackSpeed: Float): Long {
    val safeDurationMs = sourceDurationMs.coerceAtLeast(0L)
    val safeSpeed = playbackSpeed.takeIf { it.isFinite() }?.coerceIn(0.1f, 10f) ?: 1f
    if (safeDurationMs == 0L) return 0L
    return (safeDurationMs / safeSpeed).toLong().coerceAtLeast(1L)
}

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
    var trimSelection by remember { mutableStateOf(QuickTrimSelection(0L, 0L)) }
    var muteOriginalAudio by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var mirrorHorizontal by remember { mutableStateOf(false) }

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
                trimSelection = normalizeQuickTrimSelection(
                    durationMs = metadata.durationMs,
                    requestedStartMs = 0L,
                    requestedEndMs = metadata.durationMs
                )
                muteOriginalAudio = false
                playbackSpeed = 1f
                rotation = 0f
                mirrorHorizontal = false
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

                sourceClip?.let { clip ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Trim range", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${formatQuickTrimTime(trimSelection.startMs)} – ${formatQuickTrimTime(trimSelection.endMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                formatQuickTrimTime(quickEditOutputDurationMs(trimSelection.durationMs, playbackSpeed)),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        RangeSlider(
                            value = trimSelection.startMs.toFloat()..trimSelection.endMs.toFloat(),
                            onValueChange = { requestedRange ->
                                trimSelection = normalizeQuickTrimSelection(
                                    durationMs = clip.originalDurationMs,
                                    requestedStartMs = requestedRange.start.toLong(),
                                    requestedEndMs = requestedRange.endInclusive.toLong()
                                )
                            },
                            valueRange = 0f..clip.originalDurationMs.toFloat().coerceAtLeast(1f),
                            enabled = !isExporting
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Mute original audio")
                                    Text(
                                        "Export only the video track",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = muteOriginalAudio,
                                onCheckedChange = { muteOriginalAudio = it },
                                enabled = !isExporting
                            )
                        }

                        Text("Speed", fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QUICK_EDIT_SPEED_PRESETS.forEach { speed ->
                                OutlinedButton(
                                    onClick = { playbackSpeed = speed },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isExporting
                                ) {
                                    Text(
                                        if (speed == 1f) "1×" else "${speed}×",
                                        color = if (playbackSpeed == speed) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }

                        Text("Transform", fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { rotation = normalizeQuickRotation(rotation - 90f) },
                                modifier = Modifier.weight(1f),
                                enabled = !isExporting
                            ) {
                                Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Left")
                            }
                            OutlinedButton(
                                onClick = { rotation = normalizeQuickRotation(rotation + 90f) },
                                modifier = Modifier.weight(1f),
                                enabled = !isExporting
                            ) {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Right")
                            }
                            OutlinedButton(
                                onClick = { mirrorHorizontal = !mirrorHorizontal },
                                modifier = Modifier.weight(1f),
                                enabled = !isExporting
                            ) {
                                Icon(Icons.Filled.Flip, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (mirrorHorizontal) "Mirrored" else "Mirror")
                            }
                        }

                        if (rotation != 0f) {
                            Text(
                                "Rotation: ${rotation.toInt()}°",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val isDefaultSelection = trimSelection.startMs == 0L &&
                            trimSelection.endMs == clip.originalDurationMs &&
                            !muteOriginalAudio &&
                            playbackSpeed == 1f &&
                            rotation == 0f &&
                            !mirrorHorizontal
                        if (!isDefaultSelection) {
                            OutlinedButton(
                                onClick = {
                                    trimSelection = normalizeQuickTrimSelection(
                                        durationMs = clip.originalDurationMs,
                                        requestedStartMs = 0L,
                                        requestedEndMs = clip.originalDurationMs
                                    )
                                    muteOriginalAudio = false
                                    playbackSpeed = 1f
                                    rotation = 0f
                                    mirrorHorizontal = false
                                },
                                enabled = !isExporting
                            ) {
                                Icon(Icons.Filled.RestartAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Reset edits")
                            }
                        }
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
                            "Trim, change speed, rotate, mirror, or mute the original audio, then export a verified MP4.",
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
                        val trimmedClip = clip.copy(
                            trimStartMs = trimSelection.startMs,
                            trimEndMs = trimSelection.endMs,
                            isMuted = muteOriginalAudio,
                            playbackSpeed = playbackSpeed,
                            rotation = rotation,
                            flipHorizontal = mirrorHorizontal
                        ).normalized()
                        if (trimmedClip.durationMs <= 0L) {
                            Toast.makeText(context, "Choose a valid trim range", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isExporting = true
                        progress = 0
                        activeExportHandle = exporter.export(
                            clips = listOf(trimmedClip),
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
                    Text(if (isExporting) "Rendering..." else "Export Video")
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

internal fun formatQuickTrimTime(durationMs: Long): String {
    val safeMs = durationMs.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val tenths = (safeMs % 1_000L) / 100L
    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}
