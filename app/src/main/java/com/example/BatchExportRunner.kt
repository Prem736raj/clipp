package com.example

import android.content.Context
import android.net.Uri
import com.example.data.ProjectEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

data class BatchExportResult(
    val projectId: String,
    val projectName: String,
    val outputUri: Uri? = null,
    val metadata: ExportMetadata? = null,
    val errorMessage: String? = null
) {
    val succeeded: Boolean get() = outputUri != null && metadata != null && errorMessage == null
}

/** Sequentially exports saved local projects through the same verified MP4 path as the editor. */
class BatchExportRunner(private val context: Context) {
    suspend fun export(
        projects: List<ProjectEntity>,
        onProjectStarted: (ProjectEntity) -> Unit = {},
        onProjectProgress: (ProjectEntity, Int) -> Unit = { _, _ -> },
        onProjectFinished: (ProjectEntity, BatchExportResult) -> Unit = { _, _ -> }
    ): List<BatchExportResult> {
        val exporter = VideoExporter(context)
        return try {
            buildList {
                for (project in projects) {
                    coroutineContext.ensureActive()
                    onProjectStarted(project)
                    val result = exportProject(exporter, project, onProjectProgress)
                    add(result)
                    onProjectFinished(project, result)
                }
            }
        } finally {
            exporter.close()
        }
    }

    private suspend fun exportProject(
        exporter: VideoExporter,
        project: ProjectEntity,
        onProjectProgress: (ProjectEntity, Int) -> Unit
    ): BatchExportResult {
        val state = loadEditorState(project)
            ?: return failure(project, "No readable source media or saved editor state")
        val unsupportedReasons = state.exportUnsupportedReasons()
        if (unsupportedReasons.isNotEmpty()) {
            return failure(
                project,
                "Export blocked: ${unsupportedReasons.joinToString(", ")}"
            )
        }

        return try {
            // Transformer.start and its listener are main-thread owned. Keep
            // metadata/history loading off the UI thread, then enter the main
            // dispatcher only for the actual exporter lifecycle.
            withContext(Dispatchers.Main.immediate) {
                awaitExport(exporter, project, state, onProjectProgress)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            failure(project, error.message?.takeIf(String::isNotBlank) ?: "Unexpected export error")
        }
    }

    private suspend fun loadEditorState(project: ProjectEntity): EditorState? {
        val savedState = if (project.historyState.isNotBlank()) {
            runCatching {
                editorHistoryMoshi
                    .adapter(EditorHistoryModel::class.java)
                    .fromJson(project.historyState)
                    ?.currentState
            }.getOrNull()
        } else {
            null
        }
        if (savedState?.clips?.isNotEmpty() == true) return savedState

        val clips = project.sourceMediaPaths.mapNotNull { sourceUri ->
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
        return clips.takeIf { it.isNotEmpty() }?.let { EditorState(clips = it) }
    }

    private suspend fun awaitExport(
        exporter: VideoExporter,
        project: ProjectEntity,
        state: EditorState,
        onProjectProgress: (ProjectEntity, Int) -> Unit
    ): BatchExportResult = suspendCancellableCoroutine { continuation ->
        var handle: ExportHandle? = null
        fun complete(result: BatchExportResult) {
            if (continuation.isActive) continuation.resume(result)
        }

        runCatching {
            handle = exporter.export(
                clips = state.clips,
                editorState = state,
                outputName = "Clipp_Batch_${project.name}_${project.id.take(8)}.mp4",
                onProgress = { progress -> onProjectProgress(project, progress) },
                onSuccess = { outputUri, metadata ->
                    complete(
                        BatchExportResult(
                            projectId = project.id,
                            projectName = project.name,
                            outputUri = outputUri,
                            metadata = metadata
                        )
                    )
                },
                onError = { message -> complete(failure(project, message)) }
            )
        }.onFailure { error ->
            complete(failure(project, error.message?.takeIf(String::isNotBlank) ?: "Could not start export"))
        }

        continuation.invokeOnCancellation {
            handle?.cancel()
        }
    }

    private fun failure(project: ProjectEntity, message: String): BatchExportResult =
        BatchExportResult(
            projectId = project.id,
            projectName = project.name,
            errorMessage = message
        )
}
