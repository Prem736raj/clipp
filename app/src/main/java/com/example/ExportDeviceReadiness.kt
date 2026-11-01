package com.example

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecList
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import androidx.media3.common.MimeTypes

internal const val MIN_EXPORT_START_FREE_BYTES = 128L * 1024L * 1024L
internal const val EXPORT_STORAGE_WARNING_BYTES = 512L * 1024L * 1024L
internal const val EXPORT_PUBLISH_RESERVE_BYTES = 64L * 1024L * 1024L

internal data class ExportDeviceSnapshot(
    val h264EncoderAvailable: Boolean?,
    val aacEncoderAvailable: Boolean?,
    val cacheFreeBytes: Long?,
    val mediaFreeBytes: Long?,
    val isLowRamDevice: Boolean,
    val thermalStatus: Int?
)

internal data class ExportDeviceReadiness(
    val blockingReason: String? = null,
    val warnings: List<String> = emptyList()
)

internal fun EditorState.requiresAudioEncoder(): Boolean {
    return clips.any { !it.isPhoto && !it.isMuted } || audioClips.any { !it.isMuted }
}

internal fun evaluateExportDeviceReadiness(
    snapshot: ExportDeviceSnapshot,
    requiresAudioEncoder: Boolean
): ExportDeviceReadiness {
    val blockingReason = when {
        snapshot.h264EncoderAvailable == false ->
            "This device does not expose an H.264 encoder required for MP4 export."
        requiresAudioEncoder && snapshot.aacEncoderAvailable == false ->
            "This device does not expose an AAC encoder required for this project's audio."
        snapshot.thermalStatus != null &&
            snapshot.thermalStatus >= PowerManager.THERMAL_STATUS_CRITICAL ->
            "The device is too hot to start a video export. Let it cool down and try again."
        snapshot.cacheFreeBytes != null && snapshot.cacheFreeBytes < MIN_EXPORT_START_FREE_BYTES ->
            "Clipp needs at least 128 MB of free working storage before export can start."
        snapshot.mediaFreeBytes != null && snapshot.mediaFreeBytes < MIN_EXPORT_START_FREE_BYTES ->
            "Clipp needs at least 128 MB of free device storage before export can start."
        else -> null
    }

    val warnings = buildList {
        if (snapshot.isLowRamDevice) {
            add("This is a low-memory device; complex layered projects may render more slowly.")
        }
        if (snapshot.thermalStatus != null &&
            snapshot.thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE &&
            snapshot.thermalStatus < PowerManager.THERMAL_STATUS_CRITICAL
        ) {
            add("The device is running hot, so export may be throttled until it cools down.")
        }
        val knownFreeSpace = listOfNotNull(snapshot.cacheFreeBytes, snapshot.mediaFreeBytes).minOrNull()
        if (knownFreeSpace != null &&
            knownFreeSpace >= MIN_EXPORT_START_FREE_BYTES &&
            knownFreeSpace < EXPORT_STORAGE_WARNING_BYTES
        ) {
            add("Free storage is getting low; long or high-resolution exports may need more space.")
        }
    }
    return ExportDeviceReadiness(blockingReason = blockingReason, warnings = warnings)
}

internal fun inspectExportDeviceReadiness(
    context: Context,
    state: EditorState
): ExportDeviceReadiness {
    return evaluateExportDeviceReadiness(
        snapshot = captureExportDeviceSnapshot(context),
        requiresAudioEncoder = state.requiresAudioEncoder()
    )
}

internal fun captureExportDeviceSnapshot(context: Context): ExportDeviceSnapshot {
    val appContext = context.applicationContext
    val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return ExportDeviceSnapshot(
        h264EncoderAvailable = hasEncoderForMimeType(MimeTypes.VIDEO_H264),
        aacEncoderAvailable = hasEncoderForMimeType(MimeTypes.AUDIO_AAC),
        cacheFreeBytes = appContext.cacheDir.usableSpace.takeIf { it > 0L },
        mediaFreeBytes = primaryMediaFreeBytes(appContext),
        isLowRamDevice = activityManager?.isLowRamDevice == true,
        thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager?.currentThermalStatus
        } else {
            null
        }
    )
}

internal fun publicationStorageBlockingReason(
    mediaFreeBytes: Long?,
    outputBytes: Long
): String? {
    if (mediaFreeBytes == null || outputBytes <= 0L) return null
    val requiredBytes = outputBytes.coerceAtMost(Long.MAX_VALUE - EXPORT_PUBLISH_RESERVE_BYTES) +
        EXPORT_PUBLISH_RESERVE_BYTES
    return if (mediaFreeBytes < requiredBytes) {
        "There is not enough free storage to save the finished MP4. Free more space and try again."
    } else {
        null
    }
}

internal fun publicationStorageBlockingReason(context: Context, outputBytes: Long): String? {
    return publicationStorageBlockingReason(primaryMediaFreeBytes(context.applicationContext), outputBytes)
}

internal fun userFacingExportFailure(errorCodeName: String): String {
    return when {
        errorCodeName.contains("DECOD", ignoreCase = true) ->
            "Export failed because this device could not decode one of the selected media files ($errorCodeName)."
        errorCodeName.contains("ENCOD", ignoreCase = true) ->
            "Export failed because this device could not start the required media encoder ($errorCodeName)."
        errorCodeName.contains("MUX", ignoreCase = true) ->
            "Export failed while writing the MP4 container ($errorCodeName)."
        errorCodeName.contains("IO", ignoreCase = true) ->
            "Export failed while reading or writing media ($errorCodeName)."
        else -> "Export failed: $errorCodeName"
    }
}

private fun hasEncoderForMimeType(mimeType: String): Boolean? {
    return runCatching {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { codecInfo ->
            codecInfo.isEncoder && codecInfo.supportedTypes.any { supported ->
                supported.equals(mimeType, ignoreCase = true)
            }
        }
    }.getOrNull()
}

private fun primaryMediaFreeBytes(context: Context): Long? {
    return runCatching {
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?.usableSpace
            ?.takeIf { it > 0L }
    }.getOrNull()
}
