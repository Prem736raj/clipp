package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportQualityTest {
    @Test
    fun outputDimensionsFollowCanvasOrientationAndStayEncoderAligned() {
        val settings = ExportSettings(resolution = ExportResolution.FULL_HD)
        assertEquals(ExportDimensions(1920, 1080), settings.dimensions(16f / 9f))
        assertEquals(ExportDimensions(1080, 1920), settings.dimensions(9f / 16f))
        assertEquals(ExportDimensions(1080, 1080), settings.dimensions(1f))
        ExportResolution.entries.forEach { resolution ->
            AspectRatioOption.entries.forEach { aspect ->
                val dimensions = settings.copy(resolution = resolution).dimensions(aspect.ratio)
                assertEquals(resolution.shortSide, minOf(dimensions.width, dimensions.height))
                assertEquals(0, dimensions.width % 2)
                assertEquals(0, dimensions.height % 2)
                assertEquals(aspect.ratio, dimensions.width.toFloat() / dimensions.height, 0.005f)
            }
        }
    }

    @Test
    fun fileSizeEstimateAccountsForDurationBitrateAndAudioWithoutOverflow() {
        val settings = ExportSettings(videoBitrate = 8_000_000)
        assertEquals(61_800_000L, settings.estimatedBytes(60_000L, false))
        assertEquals(62_788_800L, settings.estimatedBytes(60_000L, true))
        assertEquals(0L, settings.estimatedBytes(-1L, true))
        assertTrue(settings.estimatedBytes(Long.MAX_VALUE, true) > 0L)
        assertTrue(settings.copy(videoBitrate = 20_000_000).estimatedBytes(60_000L, true) >
            settings.estimatedBytes(60_000L, true))
    }

    @Test
    fun restrictedDeviceGetsSupportedDefaultsAndNearestBitrateWhenChangingQuality() {
        val sd24 = ExportSettings(ExportResolution.SD, 24, 2_000_000)
        val sd30 = sd24.copy(frameRate = 30)
        val support = ExportQualitySupport(setOf(sd24, sd30))
        assertEquals(sd30, support.defaultSettings())
        assertEquals(sd24, support.preferredSettings(ExportResolution.SD, 24, 8_000_000))
        assertNull(support.preferredSettings(ExportResolution.UHD, 60))
        assertNotNull(support.blockingReason(ExportSettings()))
        assertNull(support.blockingReason(sd24))
    }

    @Test
    fun unknownAndInvalidQualityCannotBeExported() {
        assertNotNull(ExportSettings(frameRate = 120).validationError())
        assertNotNull(ExportSettings(videoBitrate = -1).validationError())
        assertNotNull(ExportQualitySupport().blockingReason(ExportSettings()))
        assertNull(ExportQualitySupport().defaultSettings())
        val failure = ExportQualitySupport(error = "Unable to read encoder capabilities")
        assertEquals(failure.error, failure.blockingReason(ExportSettings()))
    }

    @Test
    fun resolutionChangeCanReachOptionsRequiringADifferentFrameRate() {
        val hd30 = ExportSettings()
        val uhd24 = ExportSettings(ExportResolution.UHD, 24, 40_000_000)
        val support = ExportQualitySupport(setOf(hd30, uhd24))
        assertEquals(uhd24, support.settingsForResolution(ExportResolution.UHD, 30))
        assertEquals(hd30, support.settingsForResolution(ExportResolution.HD, 24))
    }

    @Test
    fun customAspectRequiresExplicitOutputSettingsAndKeepsUnimplementedCanvasEditsBlocked() {
        val state = EditorState(canvasSettings = CanvasSettingsState(aspectOption = AspectRatioOption.R_9_16))
        assertTrue(state.exportUnsupportedReasons().contains("custom canvas output"))
        assertTrue(state.exportUnsupportedReasons(ExportSettings()).isEmpty())
        assertTrue(state.copy(canvasSettings = state.canvasSettings.copy(fitMode = FitMode.Fill))
            .exportUnsupportedReasons(ExportSettings()).contains("custom canvas output"))
        assertTrue(state.copy(canvasSettings = state.canvasSettings.copy(backgroundType = BackgroundType.Gradient))
            .exportUnsupportedReasons(ExportSettings()).contains("custom canvas output"))
    }
}
