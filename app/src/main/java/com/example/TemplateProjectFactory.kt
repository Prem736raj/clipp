package com.example

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.example.data.ProjectEntity

/** Builds a restorable local editor project from a built-in template definition. */
object TemplateProjectFactory {
    suspend fun build(
        context: Context,
        template: VideoTemplate,
        mediaUris: List<String>,
        textValues: List<String>
    ): ProjectEntity? {
        val clips = mediaUris.mapNotNull { sourceUri ->
            val metadata = MediaMetadataReader.read(context, Uri.parse(sourceUri)) ?: return@mapNotNull null
            val isPhoto = metadata.mimeType.startsWith("image/")
            val durationMs = if (isPhoto) PHOTO_DEFAULT_DURATION_MS else metadata.durationMs
            durationMs.takeIf { it > 0L }?.let {
                MediaClip(
                    sourceUri = sourceUri,
                    originalDurationMs = it,
                    trimEndMs = it,
                    isPhoto = isPhoto
                )
            }
        }
        if (clips.isEmpty()) return null

        val totalDurationMs = clips.sumOf { it.durationMs }
        val nonBlankText = textValues.map(String::trim)
        val state = when (template.id) {
            "local_title_intro" -> buildTitleIntroState(clips, totalDurationMs, nonBlankText)
            "local_cinematic_story" -> buildCinematicState(clips, totalDurationMs, nonBlankText)
            else -> buildCleanSlideshowState(clips, totalDurationMs, nonBlankText)
        }
        val historyState = editorHistoryMoshi
            .adapter(EditorHistoryModel::class.java)
            .toJson(EditorHistoryModel(currentState = state))

        return ProjectEntity(
            name = "${template.title} Project",
            duration = formatDuration(totalDurationMs),
            aspectRatio = "16:9",
            resolution = "1080p",
            frameRate = "30fps",
            sourceMediaPaths = clips.map { it.sourceUri },
            thumbnailUri = clips.firstOrNull()?.sourceUri,
            historyState = historyState,
            isDirty = false,
            lastEdited = System.currentTimeMillis()
        )
    }

    private fun buildCleanSlideshowState(
        clips: List<MediaClip>,
        totalDurationMs: Long,
        textValues: List<String>
    ): EditorState {
        val decoratedClips = clips.mapIndexed { index, clip ->
            clip.copy(
                transitionNext = if (index < clips.lastIndex) {
                    Transition(TransitionType.FADE_TO_BLACK, 300L)
                } else {
                    Transition()
                }
            )
        }
        return EditorState(
            clips = decoratedClips,
            texts = buildTemplateTexts(textValues, totalDurationMs, TextAnimIn.FADE_IN)
        )
    }

    private fun buildCinematicState(
        clips: List<MediaClip>,
        totalDurationMs: Long,
        textValues: List<String>
    ): EditorState {
        val decoratedClips = clips.mapIndexed { index, clip ->
            clip.copy(
                filterType = FilterType.VINTAGE_FILM,
                filterIntensity = 0.65f,
                effects = listOf(AppliedEffect(type = EffectType.LETTERBOX, intensity = 0.7f)),
                transitionNext = if (index < clips.lastIndex) {
                    Transition(TransitionType.FADE_TO_WHITE, 350L)
                } else {
                    Transition()
                }
            )
        }
        return EditorState(
            clips = decoratedClips,
            texts = buildTemplateTexts(textValues, totalDurationMs, TextAnimIn.ROTATE_IN)
        )
    }

    private fun buildTitleIntroState(
        clips: List<MediaClip>,
        totalDurationMs: Long,
        textValues: List<String>
    ): EditorState {
        val title = textValues.getOrNull(0).orEmpty().ifBlank { "Your story" }
        val subtitle = textValues.getOrNull(1).orEmpty().ifBlank { "Made with Clipp" }
        val titleDurationMs = totalDurationMs.coerceAtMost(2_500L).coerceAtLeast(1L)
        val subtitleStartMs = (titleDurationMs / 2L).coerceAtMost(totalDurationMs)
        val subtitleDurationMs = (totalDurationMs - subtitleStartMs).coerceAtLeast(1L)
        return EditorState(
            clips = clips,
            texts = listOf(
                TextOverlay(
                    text = title,
                    name = "Template title",
                    fontSize = 64f,
                    textColor = Color.White,
                    isBold = true,
                    posY = 0.28f,
                    animIn = TextAnimIn.SCALE_IN,
                    animOut = TextAnimOut.FADE_OUT,
                    durationMs = titleDurationMs
                ),
                TextOverlay(
                    text = subtitle,
                    name = "Template subtitle",
                    fontSize = 30f,
                    textColor = Color.White.copy(alpha = 0.9f),
                    posY = 0.38f,
                    startTimeOnTimelineMs = subtitleStartMs,
                    durationMs = subtitleDurationMs,
                    animIn = TextAnimIn.FADE_IN,
                    animOut = TextAnimOut.FADE_OUT
                )
            )
        )
    }

    private fun buildTemplateTexts(
        textValues: List<String>,
        totalDurationMs: Long,
        entrance: TextAnimIn
    ): List<TextOverlay> {
        val title = textValues.getOrNull(0).orEmpty().ifBlank { "A Clipp story" }
        val subtitle = textValues.getOrNull(1).orEmpty().ifBlank { "" }
        val result = mutableListOf(
            TextOverlay(
                text = title,
                name = "Template title",
                fontSize = 56f,
                isBold = true,
                posY = 0.78f,
                durationMs = totalDurationMs,
                animIn = entrance,
                animLoop = TextAnimLoop.PULSE,
                animOut = TextAnimOut.FADE_OUT
            )
        )
        if (subtitle.isNotBlank()) {
            result += TextOverlay(
                text = subtitle,
                name = "Template subtitle",
                fontSize = 28f,
                posY = 0.88f,
                durationMs = totalDurationMs,
                animIn = TextAnimIn.FADE_IN,
                animOut = TextAnimOut.FADE_OUT
            )
        }
        return result
    }
}
