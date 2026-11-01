package com.example

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ProjectMediaKind {
    PRIMARY_CLIP,
    OVERLAY,
    AUDIO
}

enum class ProjectMediaExpectation {
    IMAGE,
    VIDEO,
    AUDIO
}

data class ProjectMediaReference(
    val kind: ProjectMediaKind,
    val ownerId: String,
    val sourceUri: String,
    val expectation: ProjectMediaExpectation,
    val label: String
)

data class ProjectMediaIssue(
    val sourceUri: String,
    val references: List<ProjectMediaReference>
) {
    val displayLabel: String
        get() = references.firstOrNull()?.label ?: "Project media"

    fun acceptedMimeTypes(): Array<String> {
        val expectations = references.map { it.expectation }.toSet()
        return when {
            expectations == setOf(ProjectMediaExpectation.AUDIO) -> arrayOf("audio/*")
            expectations == setOf(ProjectMediaExpectation.IMAGE) -> arrayOf("image/*")
            expectations == setOf(ProjectMediaExpectation.VIDEO) -> arrayOf("video/*")
            else -> arrayOf("image/*", "video/*", "audio/*")
        }
    }
}

/**
 * Audits and repairs URI-backed project media without changing edit timing,
 * effects, transforms, clip IDs, or layer IDs.
 */
object ProjectMediaRecovery {
    fun legacyVisualIssue(sourceUri: String): ProjectMediaIssue {
        val extension = sourceUri
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('/')
            .substringAfterLast('.', "")
            .lowercase()
        val expectation = if (extension in setOf("jpg", "jpeg", "png", "webp", "heic", "gif")) {
            ProjectMediaExpectation.IMAGE
        } else {
            ProjectMediaExpectation.VIDEO
        }
        return ProjectMediaIssue(
            sourceUri = sourceUri,
            references = listOf(
                ProjectMediaReference(
                    kind = ProjectMediaKind.PRIMARY_CLIP,
                    ownerId = sourceUri,
                    sourceUri = sourceUri,
                    expectation = expectation,
                    label = "Timeline clip"
                )
            )
        )
    }

    fun references(state: EditorState): List<ProjectMediaReference> = buildList {
        state.clips.forEach { clip ->
            add(
                ProjectMediaReference(
                    kind = ProjectMediaKind.PRIMARY_CLIP,
                    ownerId = clip.id,
                    sourceUri = clip.sourceUri,
                    expectation = if (clip.isPhoto) ProjectMediaExpectation.IMAGE else ProjectMediaExpectation.VIDEO,
                    label = "Timeline clip"
                )
            )
        }
        state.overlays.forEach { overlay ->
            add(
                ProjectMediaReference(
                    kind = ProjectMediaKind.OVERLAY,
                    ownerId = overlay.id,
                    sourceUri = overlay.sourceUri,
                    expectation = if (overlay.isPhoto || overlay.isGif) {
                        ProjectMediaExpectation.IMAGE
                    } else {
                        ProjectMediaExpectation.VIDEO
                    },
                    label = "Overlay"
                )
            )
        }
        state.audioClips.forEach { audio ->
            when {
                !audio.sourceUri.isNullOrBlank() -> add(
                    ProjectMediaReference(
                        kind = ProjectMediaKind.AUDIO,
                        ownerId = audio.id,
                        sourceUri = audio.sourceUri,
                        expectation = ProjectMediaExpectation.AUDIO,
                        label = audio.displayName?.takeIf(String::isNotBlank) ?: "Audio track"
                    )
                )
                !audio.sourceClipId.isNullOrBlank() && state.clips.none { it.id == audio.sourceClipId } -> add(
                    ProjectMediaReference(
                        kind = ProjectMediaKind.AUDIO,
                        ownerId = audio.id,
                        sourceUri = "",
                        expectation = ProjectMediaExpectation.AUDIO,
                        label = audio.displayName?.takeIf(String::isNotBlank) ?: "Linked audio"
                    )
                )
            }
        }
    }

    suspend fun audit(context: Context, state: EditorState): List<ProjectMediaIssue> = withContext(Dispatchers.IO) {
        references(state)
            .groupBy { it.sourceUri }
            .mapNotNull { (sourceUri, refs) ->
                if (sourceUri.isBlank()) {
                    return@mapNotNull ProjectMediaIssue(sourceUri, refs)
                }

                val uri = runCatching { Uri.parse(sourceUri) }.getOrNull()
                    ?: return@mapNotNull ProjectMediaIssue(sourceUri, refs)
                val expectations = refs.map { it.expectation }.toSet()
                val visualReadable = if (expectations.any { it != ProjectMediaExpectation.AUDIO }) {
                    val metadata = MediaMetadataReader.read(context, uri)
                    metadata != null && expectations
                        .filter { it != ProjectMediaExpectation.AUDIO }
                        .all { expectation ->
                            when (expectation) {
                                ProjectMediaExpectation.IMAGE -> metadata.mimeType.startsWith("image/")
                                ProjectMediaExpectation.VIDEO -> metadata.mimeType.startsWith("video/")
                                ProjectMediaExpectation.AUDIO -> true
                            }
                        }
                } else {
                    true
                }
                val audioReadable = if (ProjectMediaExpectation.AUDIO in expectations) {
                    (MediaMetadataReader.readAudioDuration(context, uri) ?: 0L) > 0L
                } else {
                    true
                }

                if (visualReadable && audioReadable) null else ProjectMediaIssue(sourceUri, refs)
            }
    }

    suspend fun validateReplacement(
        context: Context,
        issue: ProjectMediaIssue,
        replacement: Uri
    ): String? = withContext(Dispatchers.IO) {
        val expectations = issue.references.map { it.expectation }.toSet()
        if (expectations.any { it != ProjectMediaExpectation.AUDIO }) {
            val metadata = MediaMetadataReader.read(context, replacement)
                ?: return@withContext "Clipp could not read that replacement file."
            val visualMatches = expectations
                .filter { it != ProjectMediaExpectation.AUDIO }
                .all { expectation ->
                    when (expectation) {
                        ProjectMediaExpectation.IMAGE -> metadata.mimeType.startsWith("image/")
                        ProjectMediaExpectation.VIDEO -> metadata.mimeType.startsWith("video/")
                        ProjectMediaExpectation.AUDIO -> true
                    }
                }
            if (!visualMatches) {
                return@withContext "Choose the same media type as the missing file."
            }
        }
        if (ProjectMediaExpectation.AUDIO in expectations &&
            (MediaMetadataReader.readAudioDuration(context, replacement) ?: 0L) <= 0L
        ) {
            return@withContext "Clipp could not read audio from that replacement file."
        }
        null
    }

    fun relink(state: EditorState, issue: ProjectMediaIssue, replacementUri: String): EditorState {
        val oldUri = issue.sourceUri
        val unresolvedAudioIds = issue.references
            .filter { it.kind == ProjectMediaKind.AUDIO && it.sourceUri.isBlank() }
            .mapTo(mutableSetOf()) { it.ownerId }
        return state.copy(
            clips = state.clips.map { clip ->
                if (oldUri.isNotBlank() && clip.sourceUri == oldUri) clip.copy(sourceUri = replacementUri) else clip
            },
            overlays = state.overlays.map { overlay ->
                if (oldUri.isNotBlank() && overlay.sourceUri == oldUri) overlay.copy(sourceUri = replacementUri) else overlay
            },
            audioClips = state.audioClips.map { audio ->
                when {
                    oldUri.isNotBlank() && audio.sourceUri == oldUri -> audio.copy(sourceUri = replacementUri)
                    audio.id in unresolvedAudioIds -> audio.copy(sourceClipId = null, sourceUri = replacementUri)
                    else -> audio
                }
            }
        )
    }

    fun relink(
        history: EditorHistoryModel,
        issue: ProjectMediaIssue,
        replacementUri: String
    ): EditorHistoryModel {
        val updatedUndo = history.undoStack.map { action ->
            action.copy(state = relink(action.state, issue, replacementUri))
        }
        val updatedRedo = history.redoStack.map { action ->
            action.copy(state = relink(action.state, issue, replacementUri))
        }
        val updatedCurrent = history.restoredEditorState()?.let { relink(it, issue, replacementUri) }
        return history.copy(
            undoStack = updatedUndo,
            redoStack = updatedRedo,
            currentState = updatedCurrent,
            timelineProject = updatedCurrent?.toTimelineProject()
        )
    }
}
