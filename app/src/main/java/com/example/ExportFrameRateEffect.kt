package com.example

import android.content.Context
import androidx.media3.common.GlObjectsProvider
import androidx.media3.common.GlTextureInfo
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import androidx.media3.effect.PassthroughShaderProgram

/**
 * Selects at most one source frame in each target interval, anchored to a fixed
 * clock. Keeping the clock independent of the last selected frame prevents
 * fractional conversions such as 60 -> 24 from drifting to 30 fps.
 * Original timestamps are retained; lower-rate footage is never duplicated.
 */
internal class ExportFrameSampler(private val frameRate: Int) {
    private var originUs: Long? = null
    private var lastSlot = -1L

    init {
        require(frameRate in EXPORT_FRAME_RATES)
    }

    fun shouldKeep(presentationTimeUs: Long): Boolean {
        val origin = originUs ?: presentationTimeUs.also { originUs = it }
        val elapsedUs = presentationTimeUs - origin
        if (elapsedUs < 0L) return false
        // One microsecond of tolerance accommodates truncated source timestamps
        // (for example, a 24 fps source represents 1/24s as 41,666 microseconds).
        val slot = (elapsedUs.toDouble() * frameRate / 1_000_000.0 + frameRate / 1_000_000.0).toLong()
        if (slot <= lastSlot) return false
        lastSlot = slot
        return true
    }

    fun reset() {
        originUs = null
        lastSlot = -1L
    }
}

internal class ExportFrameRateEffect(private val frameRate: Int) : GlEffect {
    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = false

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        object : PassthroughShaderProgram() {
            private val sampler = ExportFrameSampler(frameRate)

            override fun queueInputFrame(
                glObjectsProvider: GlObjectsProvider,
                inputTexture: GlTextureInfo,
                presentationTimeUs: Long
            ) {
                if (sampler.shouldKeep(presentationTimeUs)) {
                    super.queueInputFrame(glObjectsProvider, inputTexture, presentationTimeUs)
                } else {
                    getInputListener().onInputFrameProcessed(inputTexture)
                    getInputListener().onReadyToAcceptInputFrame()
                }
            }

            override fun signalEndOfCurrentInputStream() {
                sampler.reset()
                super.signalEndOfCurrentInputStream()
            }

            override fun flush() {
                sampler.reset()
                super.flush()
            }

            override fun release() {
                sampler.reset()
                super.release()
            }
        }
}
