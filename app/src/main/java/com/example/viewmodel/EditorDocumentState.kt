package com.example.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.example.AudioClip
import com.example.AutoCaptionSegment
import com.example.CaptionSettings
import com.example.CanvasSettingsState
import com.example.DrawOverlay
import com.example.FrameOverlay
import com.example.HistoryAction
import com.example.MediaClip
import com.example.OverlayClip
import com.example.StickerOverlay
import com.example.TextOverlay

/**
 * Observable state for the editor's persisted document model.
 *
 * Transient controls such as selection, playback, zoom, and open panels stay
 * in the composable. Keeping the document model together gives future save,
 * undo/redo, and ViewModel work one explicit state boundary without changing
 * the existing editing behavior.
 */
class EditorDocumentState {
    val clips: MutableState<List<MediaClip>> = mutableStateOf(emptyList())
    val overlays: MutableState<List<OverlayClip>> = mutableStateOf(emptyList())
    val texts: MutableState<List<TextOverlay>> = mutableStateOf(emptyList())
    val captions: MutableState<List<AutoCaptionSegment>> = mutableStateOf(emptyList())
    val captionSettings: MutableState<CaptionSettings> = mutableStateOf(CaptionSettings())
    val stickers: MutableState<List<StickerOverlay>> = mutableStateOf(emptyList())
    val drawings: MutableState<List<DrawOverlay>> = mutableStateOf(emptyList())
    val frames: MutableState<List<FrameOverlay>> = mutableStateOf(emptyList())
    val audioClips: MutableState<List<AudioClip>> = mutableStateOf(emptyList())
    val layerOrder: MutableState<List<String>> = mutableStateOf(emptyList())
    val canvasSettings: MutableState<CanvasSettingsState> = mutableStateOf(CanvasSettingsState())
    val undoStack: MutableState<List<HistoryAction>> = mutableStateOf(emptyList())
    val redoStack: MutableState<List<HistoryAction>> = mutableStateOf(emptyList())
}
