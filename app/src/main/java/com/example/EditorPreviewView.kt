package com.example

import android.content.Context
import android.view.LayoutInflater
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

/** Texture output participates in Compose transforms, clipping and color layers. */
@UnstableApi
internal fun createEditorPreviewView(context: Context): PlayerView =
    LayoutInflater.from(context).inflate(R.layout.editor_player_view, null, false) as PlayerView
