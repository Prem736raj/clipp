from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


editor = "app/src/main/java/com/example/EditorScreen.kt"
replace_once(
    editor,
    '''    val persistHistory: () -> Unit = {
        project?.let { currentProject ->
            val historyModel = EditorHistoryModel(undoStack, redoStack, EditorState(clips, canvasSettings, overlays, texts, captions, captionSettings, stickers, drawings, frames, audioClips, layerOrder))
            val historyJson = try { editorHistoryMoshi.adapter(EditorHistoryModel::class.java).toJson(historyModel) } catch(e: Exception) { "" }
            val savedProject = currentProject.copy(
                isDirty = true,
                historyState = historyJson,
                lastEdited = System.currentTimeMillis()
            )
            projectViewModel.updateProject(savedProject)
            project = savedProject
        }
    }
''',
    '''    fun currentEditorState(): EditorState = EditorState(
        clips = clips,
        canvasSettings = canvasSettings,
        overlays = overlays,
        texts = texts,
        captions = captions,
        captionSettings = captionSettings,
        stickers = stickers,
        drawings = drawings,
        frames = frames,
        audioClips = audioClips,
        layerOrder = layerOrder
    )

    fun persistCurrentDocument(
        isDirty: Boolean,
        projectStateOverride: String? = null
    ): Boolean {
        val currentProject = project ?: return false
        val historyJson = serializeEditorHistorySnapshot(
            undoStack = undoStack,
            redoStack = redoStack,
            currentState = currentEditorState()
        ) ?: return false
        val savedProject = currentProject.copy(
            isDirty = isDirty,
            historyState = historyJson,
            lastEdited = System.currentTimeMillis(),
            projectState = projectStateOverride ?: currentProject.projectState
        )
        projectViewModel.updateProject(savedProject)
        project = savedProject
        return true
    }

    val persistHistory: () -> Unit = {
        persistCurrentDocument(isDirty = true)
    }
''',
)

replace_once(
    editor,
    '''    val saveDraft = {
        project?.let { p ->
            val json = org.json.JSONObject()
            try {
                if (p.historyState.isNotEmpty()) {
                    val oldJson = org.json.JSONObject(p.historyState)
                    oldJson.keys().forEach { json.put(it, oldJson.get(it)) }
                }
            } catch (e: Exception) {}
            json.put("zoom", zoom.toDouble())
            json.put("pos", currentPositionMs)
            json.put("tool", activeTool ?: "")
            
            val finalProject = p.copy(
                isDirty = false, 
                lastEdited = System.currentTimeMillis(), 
                projectState = "Draft",
                historyState = json.toString()
            )
            projectViewModel.updateProject(finalProject)
            android.widget.Toast.makeText(context, "Saved Draft", android.widget.Toast.LENGTH_SHORT).show()
        }
        onBack()
    }
''',
    '''    val saveDraft = {
        if (persistCurrentDocument(isDirty = false, projectStateOverride = "Draft")) {
            android.widget.Toast.makeText(context, "Saved Draft", android.widget.Toast.LENGTH_SHORT).show()
            onBack()
        } else {
            android.widget.Toast.makeText(
                context,
                "Could not save the draft. Your edits are still open.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
''',
)

replace_once(
    editor,
    '''                    IconButton(onClick = {
                        project?.let {
                            val finalProject = it.copy(isDirty = false, lastEdited = System.currentTimeMillis())
                            projectViewModel.updateProject(finalProject)
                        }
                        onBack()
                    }) {
''',
    '''                    IconButton(onClick = {
                        if (project == null) {
                            onBack()
                        } else if (persistCurrentDocument(isDirty = false)) {
                            onBack()
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "Could not save the latest edits. Please try again.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }) {
''',
)

Path("app/src/main/java/com/example/EditorPersistence.kt").write_text(
    '''package com.example

/** Serializes one complete live editor document for explicit save/clean-exit paths. */
internal fun serializeEditorHistorySnapshot(
    undoStack: List<HistoryAction>,
    redoStack: List<HistoryAction>,
    currentState: EditorState
): String? = runCatching {
    editorHistoryMoshi.adapter(EditorHistoryModel::class.java).toJson(
        EditorHistoryModel(
            undoStack = undoStack,
            redoStack = redoStack,
            currentState = currentState
        )
    )
}.getOrNull()
''',
    encoding="utf-8",
)

main_activity = "app/src/main/java/com/example/MainActivity.kt"
replace_once(
    main_activity,
    '''    } else if (intent?.action == android.content.Intent.ACTION_SEND_MULTIPLE) {
        if (intent.type?.startsWith("video/") == true || intent.type?.startsWith("image/") == true) {
            val uris = intent.getParcelableArrayListExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)
            if (!uris.isNullOrEmpty()) {
                sharedVideoUri = uris[0].toString()
            }
        }
    } else if (intent?.action == android.content.Intent.ACTION_VIEW) {
''',
    '''    } else if (intent?.action == android.content.Intent.ACTION_VIEW) {
''',
)

exporter = "app/src/main/java/com/example/VideoExporter.kt"
replace_once(
    exporter,
    '''internal const val MAX_AUDIO_LOOP_EXPORT_SEGMENTS = 4_096L

internal data class AudioExportSegment(
''',
    '''internal const val MAX_AUDIO_LOOP_EXPORT_SEGMENTS = 4_096L

internal fun isPublishedVideoMetadataValid(durationMs: Long, width: Int, height: Int): Boolean =
    durationMs > 0L && width > 0 && height > 0

internal data class AudioExportSegment(
''',
)

replace_once(
    exporter,
    '''        val durationMs = readVideoDuration(published.first)
        if (durationMs <= 0L) {
            deletePublished(published)
            return null
        }
        return Triple(published.first, durationMs, published.second)
''',
    '''        val metadata = readPublishedVideoMetadata(published.first)
        if (!isPublishedVideoMetadataValid(metadata.durationMs, metadata.width, metadata.height)) {
            deletePublished(published)
            return null
        }
        return Triple(published.first, metadata.durationMs, published.second)
''',
)

replace_once(
    exporter,
    '''    private fun readVideoDuration(uri: Uri): Long {
        val retriever = android.media.MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(appContext, uri)
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        }.getOrDefault(0L).also { runCatching { retriever.release() } }
    }
''',
    '''    private data class PublishedVideoMetadata(
        val durationMs: Long,
        val width: Int,
        val height: Int
    )

    private fun readPublishedVideoMetadata(uri: Uri): PublishedVideoMetadata {
        val retriever = android.media.MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(appContext, uri)
            PublishedVideoMetadata(
                durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L,
                width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0,
                height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0
            )
        }.getOrDefault(PublishedVideoMetadata(0L, 0, 0)).also {
            runCatching { retriever.release() }
        }
    }
''',
)

replace_once(
    exporter,
    "    override fun isActive(): Boolean = gain != 1f\n",
    "    override fun isActive(): Boolean = super.isActive() || gain != 1f\n",
)

replace_once(
    exporter,
    '''    override fun isActive(): Boolean = baseGain != 1f ||
        fadeInDurationMs > 0L ||
        fadeOutDurationMs > 0L ||
        sortedVolumeKeyframes.isNotEmpty()
''',
    '''    override fun isActive(): Boolean = super.isActive() ||
        baseGain != 1f ||
        fadeInDurationMs > 0L ||
        fadeOutDurationMs > 0L ||
        sortedVolumeKeyframes.isNotEmpty()
''',
)

Path("app/src/test/java/com/example/ProductionHardeningTest.kt").write_text(
    '''package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionHardeningTest {
    @Test
    fun editorHistorySnapshotSerializesTheCurrentLiveDocument() {
        val staleState = EditorState(
            clips = listOf(
                MediaClip(
                    id = "stale-video",
                    sourceUri = "content://stale",
                    originalDurationMs = 1_000L,
                    trimEndMs = 1_000L
                )
            )
        )
        val liveState = EditorState(
            clips = listOf(
                MediaClip(
                    id = "live-video",
                    sourceUri = "content://live",
                    originalDurationMs = 5_000L,
                    trimEndMs = 5_000L,
                    playbackSpeed = 1.5f
                )
            ),
            texts = listOf(TextOverlay(id = "live-text", text = "Current draft")),
            audioClips = listOf(
                AudioClip(
                    id = "live-audio",
                    sourceUri = "content://audio",
                    startTimeOnTimelineMs = 250L,
                    sourceDurationMs = 3_000L,
                    trimEndMs = 3_000L
                )
            ),
            layerOrder = listOf("live-text")
        )

        val json = serializeEditorHistorySnapshot(
            undoStack = listOf(HistoryAction(staleState, "Before live edit")),
            redoStack = emptyList(),
            currentState = liveState
        )
        assertNotNull(json)

        val restoredModel = editorHistoryMoshi
            .adapter(EditorHistoryModel::class.java)
            .fromJson(requireNotNull(json))
        val restored = requireNotNull(restoredModel?.restoredEditorState())

        assertEquals("live-video", restored.clips.single().id)
        assertEquals(1.5f, restored.clips.single().playbackSpeed, 0.0001f)
        assertEquals("Current draft", restored.texts.single().text)
        assertEquals("live-audio", restored.audioClips.single().id)
        assertEquals(listOf("live-text"), restored.layerOrder)
        assertEquals("stale-video", restoredModel.undoStack.single().state.clips.single().id)
    }

    @Test
    fun publishedVideoMetadataRequiresDurationAndDimensions() {
        assertTrue(isPublishedVideoMetadataValid(durationMs = 1L, width = 1, height = 1))
        assertFalse(isPublishedVideoMetadataValid(durationMs = 0L, width = 1920, height = 1080))
        assertFalse(isPublishedVideoMetadataValid(durationMs = 1_000L, width = 0, height = 1080))
        assertFalse(isPublishedVideoMetadataValid(durationMs = 1_000L, width = 1920, height = 0))
    }
}
''',
    encoding="utf-8",
)

for temporary_path in (
    Path(".github/scripts/apply_production_hardening.py"),
    Path(".github/workflows/one-shot-hardening.yml"),
):
    if temporary_path.exists():
        temporary_path.unlink()
