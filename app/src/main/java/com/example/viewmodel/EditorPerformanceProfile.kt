package com.example.viewmodel

/**
 * Resource limits for the editor preview only.
 *
 * Export keeps the project's requested output settings. These limits prevent
 * preview playback and timeline thumbnails from decoding unnecessarily large
 * frames on devices that cannot sustain them interactively.
 */
data class EditorPerformanceProfile(
    val previewMaxWidth: Int,
    val previewMaxHeight: Int,
    val thumbnailMemoryCacheFraction: Double,
    val thumbnailDiskCacheFraction: Double,
    val timelineFrameWidthDp: Float,
    val thumbnailOverscanPx: Float
) {
    companion object {
        fun forMode(mode: PerformanceMode): EditorPerformanceProfile = when (mode) {
            PerformanceMode.BETTER_PERFORMANCE -> EditorPerformanceProfile(
                previewMaxWidth = 854,
                previewMaxHeight = 480,
                thumbnailMemoryCacheFraction = 0.08,
                thumbnailDiskCacheFraction = 0.01,
                timelineFrameWidthDp = 96f,
                thumbnailOverscanPx = 240f
            )
            PerformanceMode.BALANCED -> EditorPerformanceProfile(
                previewMaxWidth = 1280,
                previewMaxHeight = 720,
                thumbnailMemoryCacheFraction = 0.12,
                thumbnailDiskCacheFraction = 0.02,
                timelineFrameWidthDp = 64f,
                thumbnailOverscanPx = 400f
            )
            PerformanceMode.BEST_QUALITY -> EditorPerformanceProfile(
                previewMaxWidth = 1920,
                previewMaxHeight = 1080,
                thumbnailMemoryCacheFraction = 0.15,
                thumbnailDiskCacheFraction = 0.03,
                timelineFrameWidthDp = 48f,
                thumbnailOverscanPx = 600f
            )
        }
    }
}
