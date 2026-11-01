package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportFrameSamplerTest {
    @Test
    fun fractionalFrameRateConversionsDoNotAccumulateTimingDrift() {
        listOf(60 to 24, 30 to 24, 60 to 30).forEach { (sourceRate, targetRate) ->
            val sampler = ExportFrameSampler(targetRate)
            val times = (0 until sourceRate * 60).map { it * 1_000_000L / sourceRate }
            val kept = times.filter(sampler::shouldKeep)
            assertEquals("$sourceRate -> $targetRate", targetRate * 60, kept.size)
            assertEquals(0L, kept.first())
            kept.forEachIndexed { index, timeUs ->
                val scheduledUs = index * 1_000_000L / targetRate
                assertTrue("Selected frame drifted from target clock", timeUs - scheduledUs in 0L..(1_000_000L / sourceRate + 1L))
            }
        }
    }

    @Test
    fun sourceAtOrBelowTheLimitKeepsEveryOriginalTimestamp() {
        listOf(24 to 24, 30 to 30, 60 to 60, 24 to 60, 30 to 60).forEach { (sourceRate, targetRate) ->
            val sampler = ExportFrameSampler(targetRate)
            val times = (0 until sourceRate * 10).map { it * 1_000_000L / sourceRate }
            assertEquals(times, times.filter(sampler::shouldKeep))
        }
    }

    @Test
    fun duplicateTimestampsAreDroppedAndNewStreamsResetTheClock() {
        val sampler = ExportFrameSampler(24)
        assertTrue(sampler.shouldKeep(10_000_000L))
        assertFalse(sampler.shouldKeep(10_000_000L))
        assertFalse(sampler.shouldKeep(9_000_000L))
        assertTrue(sampler.shouldKeep(10_041_666L))
        sampler.reset()
        assertTrue(sampler.shouldKeep(0L))
        assertTrue(sampler.shouldKeep(41_666L))
    }
}
