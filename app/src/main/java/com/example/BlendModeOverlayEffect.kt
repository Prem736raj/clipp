package com.example

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import androidx.media3.effect.OverlaySettings
import java.io.IOException

/**
 * Composites one animated bitmap overlay with a real destination frame. Media3
 * OverlayEffect intentionally exposes normal alpha compositing only, so blend
 * modes need a small destination-aware shader. One instance per layer keeps the
 * canonical z-order intact when normal and blended layers are interleaved.
 */
internal class BlendModeOverlayEffect(
    private val overlay: BitmapOverlay,
    private val blendMode: OverlayBlendModeType,
    private val windowStartMs: Long,
    private val windowEndMs: Long
) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        BlendModeOverlayProgram(
            context = context,
            useHdr = useHdr,
            overlay = overlay,
            blendMode = blendMode,
            windowStartMs = windowStartMs,
            windowEndMs = windowEndMs
        )

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = false
}

private class BlendModeOverlayProgram(
    context: Context,
    useHdr: Boolean,
    private val overlay: BitmapOverlay,
    private val blendMode: OverlayBlendModeType,
    private val windowStartMs: Long,
    private val windowEndMs: Long
) : BaseGlShaderProgram(useHdr, 1) {
    private val glProgram: GlProgram
    private var inputWidth = 1
    private var inputHeight = 1
    private var overlayTextureId = 0
    private var overlayTextureWidth = 0
    private var overlayTextureHeight = 0
    private var lastBitmap: Bitmap? = null

    init {
        try {
            glProgram = GlProgram(
                context.resources.openRawResource(R.raw.clipp_blend_vertex_es2)
                    .bufferedReader()
                    .use { it.readText() },
                context.resources.openRawResource(R.raw.clipp_blend_fragment_es2)
                    .bufferedReader()
                    .use { it.readText() }
            )
        } catch (error: IOException) {
            throw VideoFrameProcessingException(error)
        } catch (error: GlUtil.GlException) {
            throw VideoFrameProcessingException(error)
        }

        glProgram.setBufferAttribute(
            "aFramePosition",
            GlUtil.getNormalizedCoordinateBounds(),
            GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE
        )
        glProgram.setIntUniform("uBlendMode", blendMode.shaderValue)
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        this.inputWidth = inputWidth.coerceAtLeast(1)
        this.inputHeight = inputHeight.coerceAtLeast(1)
        overlay.configure(Size(this.inputWidth, this.inputHeight))
        return Size(this.inputWidth, this.inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        val localTimeMs = presentationTimeUs / 1_000L
        val isActive = localTimeMs >= windowStartMs && localTimeMs < windowEndMs
        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uVideoTexSampler", inputTexId, 0)
            glProgram.setIntUniform("uBlendMode", blendMode.shaderValue)

            if (isActive) {
                val bitmap = overlay.getBitmap(presentationTimeUs)
                if (!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0) {
                    updateOverlayTexture(bitmap)
                    val settings = overlay.getOverlaySettings(presentationTimeUs)
                    glProgram.setSamplerTexIdUniform("uOverlayTexSampler", overlayTextureId, 1)
                    glProgram.setFloatsUniform(
                        "uOverlayTransformationMatrix",
                        createOverlaySamplingMatrix(bitmap.width, bitmap.height, settings)
                    )
                    glProgram.setFloatUniform("uOverlayAlphaScale", settings.alphaScale.coerceIn(0f, 1f))
                } else {
                    glProgram.setFloatUniform("uOverlayAlphaScale", 0f)
                    glProgram.setFloatsUniform("uOverlayTransformationMatrix", identityMatrix())
                }
            } else {
                glProgram.setFloatUniform("uOverlayAlphaScale", 0f)
                glProgram.setFloatsUniform("uOverlayTransformationMatrix", identityMatrix())
            }

            glProgram.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        } catch (error: GlUtil.GlException) {
            throw VideoFrameProcessingException(error, presentationTimeUs)
        }
    }

    override fun release() {
        try {
            if (overlayTextureId != 0) {
                GlUtil.deleteTexture(overlayTextureId)
                overlayTextureId = 0
            }
            glProgram.delete()
            overlay.release()
        } catch (error: GlUtil.GlException) {
            throw VideoFrameProcessingException(error)
        } finally {
            super.release()
        }
    }

    private fun updateOverlayTexture(bitmap: Bitmap) {
        if (overlayTextureId == 0 ||
            overlayTextureWidth != bitmap.width ||
            overlayTextureHeight != bitmap.height
        ) {
            if (overlayTextureId != 0) GlUtil.deleteTexture(overlayTextureId)
            overlayTextureId = GlUtil.createTexture(bitmap)
            overlayTextureWidth = bitmap.width
            overlayTextureHeight = bitmap.height
        } else if (lastBitmap !== bitmap) {
            GlUtil.setTexture(overlayTextureId, bitmap)
        }
        lastBitmap = bitmap
    }

    /** Duplicates Media3's overlay sampler transform for destination-aware blending. */
    private fun createOverlaySamplingMatrix(
        overlayWidth: Int,
        overlayHeight: Int,
        settings: OverlaySettings
    ): FloatArray {
        val backgroundAnchor = identityMatrix().also {
            android.opengl.Matrix.translateM(
                it,
                0,
                settings.backgroundFrameAnchor.first,
                settings.backgroundFrameAnchor.second,
                0f
            )
        }
        val aspectRatio = identityMatrix().also {
            android.opengl.Matrix.scaleM(
                it,
                0,
                overlayWidth.toFloat() / inputWidth,
                overlayHeight.toFloat() / inputHeight,
                1f
            )
        }
        val scale = identityMatrix().also {
            android.opengl.Matrix.scaleM(
                it,
                0,
                settings.scale.first,
                settings.scale.second,
                1f
            )
        }
        val scaleInverse = identityMatrix()
        android.opengl.Matrix.invertM(scaleInverse, 0, scale, 0)

        val overlayAnchor = identityMatrix().also {
            android.opengl.Matrix.translateM(
                it,
                0,
                -settings.overlayFrameAnchor.first,
                -settings.overlayFrameAnchor.second,
                0f
            )
        }
        val overlayAspectRatio = identityMatrix().also {
            android.opengl.Matrix.scaleM(
                it,
                0,
                inputHeight.toFloat() / inputWidth,
                1f,
                1f
            )
        }
        val overlayAspectRatioInverse = identityMatrix()
        android.opengl.Matrix.invertM(overlayAspectRatioInverse, 0, overlayAspectRatio, 0)
        val rotation = identityMatrix().also {
            android.opengl.Matrix.rotateM(it, 0, settings.rotationDegrees, 0f, 0f, 1f)
        }

        var transformation = identityMatrix()
        listOf(
            backgroundAnchor,
            aspectRatio,
            scale,
            overlayAnchor,
            scaleInverse,
            overlayAspectRatio,
            rotation,
            overlayAspectRatioInverse,
            scale
        ).forEach { next ->
            transformation = multiply(transformation, next)
        }

        val inverse = identityMatrix()
        if (!android.opengl.Matrix.invertM(inverse, 0, transformation, 0)) {
            return identityMatrix()
        }
        return inverse
    }

    private fun multiply(left: FloatArray, right: FloatArray): FloatArray = FloatArray(16).also {
        android.opengl.Matrix.multiplyMM(it, 0, left, 0, right, 0)
    }

    private fun identityMatrix(): FloatArray = FloatArray(16).also { matrix ->
        android.opengl.Matrix.setIdentityM(matrix, 0)
    }
}

private val OverlayBlendModeType.shaderValue: Int
    get() = when (this) {
        OverlayBlendModeType.NORMAL -> 0
        OverlayBlendModeType.MULTIPLY -> 1
        OverlayBlendModeType.SCREEN -> 2
        OverlayBlendModeType.OVERLAY -> 3
        OverlayBlendModeType.SOFT_LIGHT -> 4
        OverlayBlendModeType.HARD_LIGHT -> 5
        OverlayBlendModeType.DIFFERENCE -> 6
        OverlayBlendModeType.ADD -> 7
    }
