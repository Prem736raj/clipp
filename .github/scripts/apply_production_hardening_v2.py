from pathlib import Path
import re


def sub_once(path: str, pattern: str, replacement: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.MULTILINE | re.DOTALL)
    if count != 1:
        raise SystemExit(f"{path}: expected one regex match, found {count}: {pattern[:80]!r}")
    p.write_text(updated, encoding="utf-8")


editor = "app/src/main/java/com/example/EditorScreen.kt"
sub_once(
    editor,
    r"^    val persistHistory: \(\) -> Unit = \{\n.*?^    \}\n(?=\n    var isRecordingVoiceover)",
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

sub_once(
    editor,
    r"^        val saveDraft = \{\n.*?^        \}\n(?=\n\s*\n        val context = )",
    '''        val saveDraft = {
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

sub_once(
    editor,
    r"(^                    navigationIcon = \{\n)                        IconButton\(onClick = \{\n.*?^                        \}\) \{",
    r'''\1                        IconButton(onClick = {
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
                        }) {''',
)

Path("app/src/main/java/com/example/EditorPersistence.kt").write_text(
    '''package com.example

/** Serializes one complete live editor document for explicit save and clean-exit paths. */
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
sub_once(
    main_activity,
    r'''    \} else if \(intent\?\.action == android\.content\.Intent\.ACTION_SEND_MULTIPLE\) \{\n.*?    \} else if \(intent\?\.action == android\.content\.Intent\.ACTION_VIEW\) \{''',
    '''    } else if (intent?.action == android.content.Intent.ACTION_VIEW) {''',
)

exporter = "app/src/main/java/com/example/VideoExporter.kt"
sub_once(
    exporter,
    r"internal const val MAX_AUDIO_LOOP_EXPORT_SEGMENTS = 4_096L\n",
    '''internal const val MAX_AUDIO_LOOP_EXPORT_SEGMENTS = 4_096L

internal fun isPublishedVideoMetadataValid(durationMs: Long, width: Int, height: Int): Boolean =
    durationMs > 0L && width > 0 && height > 0
''',
)

sub_once(
    exporter,
    r'''        val durationMs = readVideoDuration\(published\.first\)\n        if \(durationMs <= 0L\) \{\n            deletePublished\(published\)\n            return null\n        \}\n        return Triple\(published\.first, durationMs, published\.second\)''',
    '''        val metadata = readPublishedVideoMetadata(published.first)
        if (!isPublishedVideoMetadataValid(metadata.durationMs, metadata.width, metadata.height)) {
            deletePublished(published)
            return null
        }
        return Triple(published.first, metadata.durationMs, published.second)''',
)

sub_once(
    exporter,
    r'''^    private fun readVideoDuration\(uri: Uri\): Long \{\n.*?^    \}\n(?=\n    private fun deletePublished)''',
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

sub_once(
    exporter,
    r"^    override fun isActive\(\): Boolean = gain != 1f$",
    "    override fun isActive(): Boolean = super.isActive() || gain != 1f",
)

sub_once(
    exporter,
    r'''^    override fun isActive\(\): Boolean = baseGain != 1f \|\|\n        fadeInDurationMs > 0L \|\|\n        fadeOutDurationMs > 0L \|\|\n        sortedVolumeKeyframes\.isNotEmpty\(\)$''',
    '''    override fun isActive(): Boolean = super.isActive() ||
        baseGain != 1f ||
        fadeInDurationMs > 0L ||
        fadeOutDurationMs > 0L ||
        sortedVolumeKeyframes.isNotEmpty()''',
)

Path("app/src/test/java/com/example/ProductionHardeningTest.kt").write_text(
    '''package com.example

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionHardeningTest {
    @Test
    fun editorHistorySnapshotSerializesTheCurrentLiveDocument() {
        val staleState = EditorState(
            canvasSettings = CanvasSettingsState(backgroundColorValue = Color.Red.value.toLong())
        )
        val liveState = EditorState(
            canvasSettings = CanvasSettingsState(backgroundColorValue = Color.Blue.value.toLong()),
            layerOrder = listOf("live-layer")
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

        assertEquals(liveState.canvasSettings.backgroundColorValue, restored.canvasSettings.backgroundColorValue)
        assertEquals(listOf("live-layer"), restored.layerOrder)
        assertEquals(staleState.canvasSettings.backgroundColorValue, restoredModel.undoStack.single().state.canvasSettings.backgroundColorValue)
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
    Path(".github/scripts/apply_production_hardening_v2.py"),
    Path(".github/workflows/one-shot-hardening.yml"),
):
    if temporary_path.exists():
        temporary_path.unlink()
