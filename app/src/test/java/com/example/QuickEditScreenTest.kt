package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickEditScreenTest {
    @Test
    fun normalizeQuickTrimSelection_keepsValidRange() {
        val selection = normalizeQuickTrimSelection(
            durationMs = 10_000L,
            requestedStartMs = 2_000L,
            requestedEndMs = 7_500L
        )

        assertEquals(2_000L, selection.startMs)
        assertEquals(7_500L, selection.endMs)
        assertEquals(5_500L, selection.durationMs)
    }

    @Test
    fun normalizeQuickTrimSelection_clampsToSourceDuration() {
        val selection = normalizeQuickTrimSelection(
            durationMs = 8_000L,
            requestedStartMs = -1_000L,
            requestedEndMs = 12_000L
        )

        assertEquals(0L, selection.startMs)
        assertEquals(8_000L, selection.endMs)
    }

    @Test
    fun normalizeQuickTrimSelection_enforcesMinimumRangeNearEnd() {
        val selection = normalizeQuickTrimSelection(
            durationMs = 10_000L,
            requestedStartMs = 9_950L,
            requestedEndMs = 10_000L
        )

        assertEquals(9_750L, selection.startMs)
        assertEquals(10_000L, selection.endMs)
        assertEquals(QUICK_TRIM_MIN_DURATION_MS, selection.durationMs)
    }

    @Test
    fun normalizeQuickTrimSelection_usesWholeClipWhenShorterThanMinimum() {
        val selection = normalizeQuickTrimSelection(
            durationMs = 120L,
            requestedStartMs = 100L,
            requestedEndMs = 110L
        )

        assertEquals(0L, selection.startMs)
        assertEquals(120L, selection.endMs)
    }

    @Test
    fun formatQuickTrimTime_includesTenths() {
        assertEquals("01:05.4", formatQuickTrimTime(65_499L))
    }

    @Test
    fun normalizeQuickRotation_wrapsBothDirections() {
        assertEquals(270f, normalizeQuickRotation(-90f), 0f)
        assertEquals(90f, normalizeQuickRotation(450f), 0f)
    }

    @Test
    fun quickEditOutputDurationMs_accountsForSpeed() {
        assertEquals(10_000L, quickEditOutputDurationMs(5_000L, 0.5f))
        assertEquals(2_500L, quickEditOutputDurationMs(5_000L, 2f))
    }

    @Test
    fun quickEditOutputDurationMs_recoversFromInvalidSpeed() {
        assertEquals(5_000L, quickEditOutputDurationMs(5_000L, Float.NaN))
    }
}
