package com.example

import android.content.Context
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import java.io.IOException

/**
 * Effects that are rendered in the export frame shader rather than as a bitmap
 * decoration. Keeping these in one list makes the capability gate match the
 * actual renderer and prevents the editor from advertising a no-op export.
 */
internal val EXPORT_SHADER_EFFECTS = setOf(
    EffectType.DIGITAL_GLITCH,
    EffectType.RGB_SPLIT,
    EffectType.SIGNAL_ERROR,
    EffectType.DATAMOSH,
    EffectType.MOTION_BLUR,
    EffectType.RADIAL_BLUR,
    EffectType.TILT_SHIFT,
    EffectType.KALEIDOSCOPE,
    EffectType.FISHEYE,
    EffectType.WAVE,
    EffectType.PIXELATE,
    EffectType.PRISM,
    EffectType.NEON_GLOW
)

private fun EffectType.shaderMode(): Int = when (this) {
    EffectType.RGB_SPLIT -> 1
    EffectType.DIGITAL_GLITCH -> 2
    EffectType.SIGNAL_ERROR -> 3
    EffectType.DATAMOSH -> 4
    EffectType.FISHEYE -> 5
    EffectType.WAVE -> 6
    EffectType.PIXELATE -> 7
    EffectType.KALEIDOSCOPE -> 8
    EffectType.MOTION_BLUR -> 9
    EffectType.RADIAL_BLUR -> 10
    EffectType.TILT_SHIFT -> 11
    EffectType.PRISM -> 12
    EffectType.NEON_GLOW -> 13
    else -> error("No shader mode for $this")
}

/** A timestamp-aware GLES effect used for source-dependent distortions. */
internal class ShaderDistortionEffect(
    type: EffectType,
    intensity: Float,
    startTimeMs: Long,
    endTimeMs: Long
) : GlEffect {
    private val mode = type.shaderMode()
    private val intensity = intensity.coerceIn(0f, 1f)
    private val startTimeMs = startTimeMs.coerceAtLeast(0L)
    private val endTimeMs = endTimeMs.coerceAtLeast(this.startTimeMs + 1L)

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return ShaderDistortionProgram(
            context = context,
            useHdr = useHdr,
            mode = mode,
            intensity = intensity,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs
        )
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = intensity <= 0f
}

private class ShaderDistortionProgram(
    context: Context,
    useHdr: Boolean,
    mode: Int,
    intensity: Float,
    startTimeMs: Long,
    endTimeMs: Long
) : BaseGlShaderProgram(useHdr, 1) {
    private val glProgram: GlProgram

    init {
        try {
            glProgram = GlProgram(
                context.resources.openRawResource(R.raw.clipp_distortion_vertex_es2)
                    .bufferedReader()
                    .use { it.readText() },
                context.resources.openRawResource(R.raw.clipp_distortion_fragment_es2)
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
        val identity = GlUtil.create4x4IdentityMatrix()
        glProgram.setFloatsUniform("uTransformationMatrix", identity)
        glProgram.setFloatsUniform("uTexTransformationMatrix", identity)
        glProgram.setIntUniform("uMode", mode)
        glProgram.setFloatUniform("uIntensity", intensity)
        glProgram.setFloatUniform("uStartMs", startTimeMs.toFloat())
        glProgram.setFloatUniform("uEndMs", endTimeMs.toFloat())
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size = Size(inputWidth, inputHeight)

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            glProgram.setFloatUniform("uTime", presentationTimeUs / 1_000_000f)
            glProgram.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        } catch (error: GlUtil.GlException) {
            throw VideoFrameProcessingException(error, presentationTimeUs)
        }
    }

    override fun release() {
        try {
            glProgram.delete()
        } catch (error: GlUtil.GlException) {
            throw VideoFrameProcessingException(error)
        } finally {
            super.release()
        }
    }
}
