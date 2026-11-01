package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExportQualityControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun changingResolutionAndRateSelectsSupportedSettingsAndDisablesUnavailableChoices() {
        val hd = ExportSettings()
        val sd = ExportSettings(ExportResolution.SD, 30, 2_000_000)
        val sd24 = sd.copy(frameRate = 24)
        var chosen = hd
        compose.setContent {
            var settings by remember { mutableStateOf(hd) }
            MaterialTheme {
                ExportQualityControls(settings, ExportQualitySupport(setOf(hd, sd, sd24)), true) {
                    settings = it
                    chosen = it
                }
            }
        }

        compose.onNodeWithText("4K").assertIsNotEnabled()
        compose.onNodeWithText("60 fps").assertIsNotEnabled()
        compose.onNodeWithText("480p").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(sd, chosen) }
        compose.onNodeWithText("24 fps").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(sd24, chosen) }
        compose.onNodeWithText("2 Mbps").assertIsSelected()
    }

    @Test
    fun qualityControlsCannotChangeDuringExport() {
        compose.setContent {
            MaterialTheme {
                ExportQualityControls(ExportSettings(), ExportQualitySupport(setOf(ExportSettings())), false) {
                    error("Quality must remain fixed during export")
                }
            }
        }
        compose.onNodeWithText("720p").assertIsNotEnabled()
        compose.onNodeWithText("30 fps").assertIsNotEnabled()
    }
}
