package com.example

/**
 * The single source of truth for editor capabilities.
 *
 * A control may still be present in an older saved project even when it is not
 * export-ready. New edits must use this registry so preview-only behavior is
 * never presented as a finished export feature.
 */
enum class FeatureCapability {
    PREVIEW_AND_EXPORT_SUPPORTED,
    PREVIEW_ONLY_EXPERIMENTAL,
    DISABLED,
    COMING_LATER
}

data class FeatureCapabilityStatus(
    val id: String,
    val label: String,
    val capability: FeatureCapability,
    val detail: String
) {
    val isExportReady: Boolean
        get() = capability == FeatureCapability.PREVIEW_AND_EXPORT_SUPPORTED

    val isSelectable: Boolean
        get() = isExportReady

    val shortLabel: String
        get() = when (capability) {
            FeatureCapability.PREVIEW_AND_EXPORT_SUPPORTED -> "Export ready"
            FeatureCapability.PREVIEW_ONLY_EXPERIMENTAL -> "Preview only"
            FeatureCapability.DISABLED -> "Disabled"
            FeatureCapability.COMING_LATER -> "Coming later"
        }
}

internal object FeatureCapabilityRegistry {
    internal val exportCropKeyframes = setOf("cropLeft", "cropTop", "cropRight", "cropBottom")
    internal val exportSupportedClipKeyframes = setOf("posX", "posY", "scale", "rotation") + exportCropKeyframes
    internal val exportSupportedOverlayKeyframes = setOf("posX", "posY", "scaleX", "scaleY", "rotation", "opacity")
    internal val exportSupportedAudioKeyframes = setOf("volume")

    internal val exportSupportedEffects = setOf(
        EffectType.GAUSSIAN_BLUR,
        EffectType.LETTERBOX,
        EffectType.MIRROR,
        EffectType.SHAKE,
        EffectType.COMIC_BOOK,
        EffectType.PENCIL_SKETCH,
        EffectType.POP_ART,
        EffectType.FILM_GRAIN,
        EffectType.ANAMORPHIC_FLARE,
        EffectType.SPARKLE,
        EffectType.LIGHT_LEAK,
        EffectType.LENS_FLARE,
        EffectType.BOKEH
    ) + EXPORT_SHADER_EFFECTS

    internal val exportTimedOverlayEffects = setOf(
        EffectType.FILM_GRAIN,
        EffectType.ANAMORPHIC_FLARE,
        EffectType.SPARKLE,
        EffectType.LIGHT_LEAK,
        EffectType.LENS_FLARE,
        EffectType.BOKEH
    ) + EXPORT_SHADER_EFFECTS

    internal val exportVideoTransitions = setOf(
        TransitionType.CROSSFADE,
        TransitionType.SLIDE_LEFT,
        TransitionType.SLIDE_RIGHT,
        TransitionType.SLIDE_UP,
        TransitionType.SLIDE_DOWN,
        TransitionType.ZOOM_IN,
        TransitionType.ZOOM_OUT,
        TransitionType.SPIN,
        TransitionType.FLIP
    )

    internal val exportPhotoTransitions = exportVideoTransitions + setOf(
        TransitionType.WIPE_LEFT,
        TransitionType.WIPE_RIGHT,
        TransitionType.CLOCK_WIPE
    )

    internal val exportSupportedTextAnimIn = setOf(
        TextAnimIn.NONE,
        TextAnimIn.FADE_IN,
        TextAnimIn.SCALE_IN,
        TextAnimIn.ROTATE_IN
    )
    internal val exportSupportedTextAnimLoop = setOf(
        TextAnimLoop.NONE,
        TextAnimLoop.PULSE,
        TextAnimLoop.WAVE,
        TextAnimLoop.SWING
    )
    internal val exportSupportedTextAnimOut = setOf(
        TextAnimOut.NONE,
        TextAnimOut.FADE_OUT,
        TextAnimOut.SCALE_OUT
    )

    /** The small, honest launch matrix shown on the export surface. */
    internal val launchStatuses = listOf(
        FeatureCapabilityStatus(
            id = "basic-editing",
            label = "Trim, split, reorder, crop, rotate, flip",
            capability = FeatureCapability.PREVIEW_AND_EXPORT_SUPPORTED,
            detail = "Rendered by the shared timeline and MP4 exporter."
        ),
        FeatureCapabilityStatus(
            id = "constant-speed",
            label = "Constant speed and pitch-preserving audio",
            capability = FeatureCapability.PREVIEW_AND_EXPORT_SUPPORTED,
            detail = "Speed curves are intentionally excluded from this launch set."
        ),
        FeatureCapabilityStatus(
            id = "visual-layers",
            label = "Basic text, captions, stickers, drawings, frames, and media overlays",
            capability = FeatureCapability.PREVIEW_AND_EXPORT_SUPPORTED,
            detail = "Only the animation, keyframe, mask, and blend variants listed below are export-ready."
        ),
        FeatureCapabilityStatus(
            id = "audio",
            label = "Local audio tracks and voiceover",
            capability = FeatureCapability.PREVIEW_AND_EXPORT_SUPPORTED,
            detail = "Volume, fades, supported keyframes, and implemented processors are muxed into MP4."
        ),
        FeatureCapabilityStatus(
            id = "speed-curves",
            label = "Speed curves",
            capability = FeatureCapability.COMING_LATER,
            detail = "The current preview can model them, but the exporter does not yet map curve time safely."
        ),
        FeatureCapabilityStatus(
            id = "complex-transitions",
            label = "Transitions between edited sources",
            capability = FeatureCapability.PREVIEW_AND_EXPORT_SUPPORTED,
            detail = "Crossfade, slide, zoom, spin, flip, and photo wipe transitions render export-ready edits on both adjacent sources."
        ),
        FeatureCapabilityStatus(
            id = "masks-and-blends",
            label = "Overlay masks, blend modes, and slide animations",
            capability = FeatureCapability.COMING_LATER,
            detail = "These preview paths are not reproduced by the current exporter."
        ),
        FeatureCapabilityStatus(
            id = "unsupported-audio-dsp",
            label = "Noise reduction, voice changer, ducking, and audio crossfade",
            capability = FeatureCapability.COMING_LATER,
            detail = "Controls remain unavailable until real export processors exist."
        ),
        FeatureCapabilityStatus(
            id = "online-features",
            label = "AI, cloud accounts, and billing",
            capability = FeatureCapability.DISABLED,
            detail = "No backend or verified entitlement pipeline is included in the local launch build."
        )
    )

    fun effect(type: EffectType): FeatureCapabilityStatus = if (type in exportSupportedEffects) {
        exportReady("effect:${type.name}", type.label, "The same effect family is available to preview and export.")
    } else {
        FeatureCapabilityStatus(
            id = "effect:${type.name}",
            label = type.label,
            capability = FeatureCapability.PREVIEW_ONLY_EXPERIMENTAL,
            detail = "This effect has a preview implementation but is not export-ready yet."
        )
    }

    fun textAnimation(animation: TextAnimIn): FeatureCapabilityStatus = animationStatus(
        id = "text-animation-in:${animation.name}",
        label = "Text entrance: ${animation.name}",
        supported = animation in exportSupportedTextAnimIn
    )

    fun textAnimation(animation: TextAnimLoop): FeatureCapabilityStatus = animationStatus(
        id = "text-animation-loop:${animation.name}",
        label = "Text loop: ${animation.name}",
        supported = animation in exportSupportedTextAnimLoop
    )

    fun textAnimation(animation: TextAnimOut): FeatureCapabilityStatus = animationStatus(
        id = "text-animation-out:${animation.name}",
        label = "Text exit: ${animation.name}",
        supported = animation in exportSupportedTextAnimOut
    )

    fun overlayAnimation(animation: OverlayAnim): FeatureCapabilityStatus = if (
        animation == OverlayAnim.NONE || animation == OverlayAnim.FADE || animation == OverlayAnim.SCALE
    ) {
        exportReady("overlay-animation:${animation.name}", "Overlay ${animation.name}", "The animation is rendered by preview and export.")
    } else {
        FeatureCapabilityStatus(
            id = "overlay-animation:${animation.name}",
            label = "Overlay ${animation.name}",
            capability = FeatureCapability.PREVIEW_ONLY_EXPERIMENTAL,
            detail = "Slide animation is preview-only until the exporter has matching motion semantics."
        )
    }

    fun blendMode(mode: OverlayBlendModeType): FeatureCapabilityStatus = if (mode == OverlayBlendModeType.NORMAL) {
        exportReady("overlay-blend:${mode.name}", "Overlay blend: ${mode.name}", "Normal compositing is shared by preview and export.")
    } else {
        FeatureCapabilityStatus(
            id = "overlay-blend:${mode.name}",
            label = "Overlay blend: ${mode.name}",
            capability = FeatureCapability.COMING_LATER,
            detail = "This blend mode is not reproduced by the current export compositor."
        )
    }

    fun maskShape(shape: MaskShape): FeatureCapabilityStatus = if (shape == MaskShape.NONE) {
        exportReady("overlay-mask:${shape.name}", "Overlay mask: none", "No mask is shared by preview and export.")
    } else {
        FeatureCapabilityStatus(
            id = "overlay-mask:${shape.name}",
            label = "Overlay mask: ${shape.name}",
            capability = FeatureCapability.COMING_LATER,
            detail = "This mask is preview-only until it is rendered by the export compositor."
        )
    }

    fun overlayDecoration(): FeatureCapabilityStatus = FeatureCapabilityStatus(
        id = "overlay-decoration",
        label = "Overlay border and shadow",
        capability = FeatureCapability.COMING_LATER,
        detail = "The current export compositor does not draw overlay borders or shadows."
    )

    fun textDepth(): FeatureCapabilityStatus = FeatureCapabilityStatus(
        id = "text-3d-depth",
        label = "3D text depth",
        capability = FeatureCapability.COMING_LATER,
        detail = "The current text bitmap exporter supports 2D text only."
    )

    fun transition(
        type: TransitionType,
        outgoing: MediaClip? = null,
        incoming: MediaClip? = null
    ): FeatureCapabilityStatus {
        if (type == TransitionType.NONE || type == TransitionType.FADE_TO_BLACK || type == TransitionType.FADE_TO_WHITE) {
            return exportReady("transition:${type.name}", type.label, "The transition is rendered by preview and export.")
        }
        if (type !in exportPhotoTransitions) {
            return FeatureCapabilityStatus(
                id = "transition:${type.name}",
                label = type.label,
                capability = FeatureCapability.PREVIEW_ONLY_EXPERIMENTAL,
                detail = "This transition is available in preview but not in the current exporter."
            )
        }

        val sourceCompatible = if (outgoing == null || incoming == null) {
            true
        } else {
            val outgoingCompatible = outgoing.canRenderTransitionBaseSource()
            val incomingSimple = if (incoming.isPhoto) {
                incoming.canRenderPhotoTransitionSource()
            } else {
                type in exportVideoTransitions && incoming.canRenderVideoTransitionSource()
            }
            outgoingCompatible && incomingSimple
        }

        return if (sourceCompatible) {
            exportReady(
                "transition:${type.name}",
                type.label,
                "This transition is export-ready for the selected compatible sources."
            )
        } else {
            FeatureCapabilityStatus(
                id = "transition:${type.name}",
                label = type.label,
                capability = FeatureCapability.PREVIEW_ONLY_EXPERIMENTAL,
                detail = "The selected source edits prevent the exporter from rendering both transition frames."
            )
        }
    }

    fun speedCurve(): FeatureCapabilityStatus = FeatureCapabilityStatus(
        id = "speed-curves",
        label = "Speed curves",
        capability = FeatureCapability.COMING_LATER,
        detail = "Curve time mapping is not export-ready."
    )

    private fun animationStatus(id: String, label: String, supported: Boolean): FeatureCapabilityStatus = if (supported) {
        exportReady(id, label, "The animation is rendered by preview and export.")
    } else {
        FeatureCapabilityStatus(
            id = id,
            label = label,
            capability = FeatureCapability.PREVIEW_ONLY_EXPERIMENTAL,
            detail = "This animation is preview-only until export has matching timing behavior."
        )
    }

    private fun exportReady(id: String, label: String, detail: String) = FeatureCapabilityStatus(
        id = id,
        label = label,
        capability = FeatureCapability.PREVIEW_AND_EXPORT_SUPPORTED,
        detail = detail
    )
}
