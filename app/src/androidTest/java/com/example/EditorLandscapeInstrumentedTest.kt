package com.example

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.AppDatabase
import com.example.data.ProjectEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@UnstableApi
@RunWith(AndroidJUnit4::class)
class EditorLandscapeInstrumentedTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var projectId: String? = null

    @After
    fun cleanup() {
        runBlocking {
            compose.runOnUiThread {
                compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            projectId?.let { AppDatabase.getDatabase(context).projectDao().deleteProjectById(it) }
        }
    }

    @Test
    fun orientationButtonSwitchesWorkspaceWithoutLosingPlaybackState() {
        val project = createPlaybackProject()
        projectId = project.id

        compose.runOnUiThread {
            compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            compose.activity.setContent {
                MaterialTheme { EditorScreen(project.id, {}) }
            }
        }

        compose.waitUntil(30_000) { playerValue { it?.player?.playbackState == Player.STATE_READY } }
        compose.onNodeWithContentDescription("Switch to landscape editor").assertExists()
        compose.onNodeWithTag("preview_error").assertDoesNotExist()

        invokeTransport("Play")
        compose.waitUntil(15_000) {
            playerValue { view ->
                view?.player?.let { player -> player.isPlaying && player.currentPosition > 1_000L } == true
            }
        }
        val positionBeforeRotation = playerValue { it?.player?.currentPosition ?: 0L }

        compose.onNodeWithContentDescription("Switch to landscape editor").performClick()
        compose.waitUntil(10_000) {
            compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
        compose.onNodeWithContentDescription("Switch to portrait editor").assertExists()
        compose.onNodeWithTag("editor_preview").assertExists()
        compose.onNodeWithTag("preview_error").assertDoesNotExist()
        compose.onNodeWithTag("editor_timeline").assertExists()
        compose.onNodeWithTag("editor_timeline_tracks").assert(hasScrollAction())
        compose.onNodeWithTag("timeline_tracks_down").assertIsEnabled()
        compose.onNodeWithTag("timeline_zoom_level").assertTextEquals("100%")
        compose.onNodeWithTag("timeline_zoom_in").performClick()
        compose.onNodeWithTag("timeline_zoom_level").assertTextEquals("125%")
        compose.onNodeWithTag("timeline_zoom_out").performClick()
        compose.onNodeWithTag("timeline_zoom_level").assertTextEquals("100%")
        compose.onNodeWithTag("timeline_tracks_down").performClick()
        compose.onNodeWithTag("timeline_tracks_up").assertIsEnabled()
        compose.onNodeWithTag("timeline_tracks_up").performClick()

        compose.waitUntil(15_000) {
            playerValue { view ->
                view?.player?.let { player ->
                    player.playbackState == Player.STATE_READY &&
                        player.currentPosition >= positionBeforeRotation
                } == true
            }
        }
        var landscapeWidth = 0
        var landscapeHeight = 0
        compose.runOnUiThread {
            landscapeWidth = compose.activity.window.decorView.width
            landscapeHeight = compose.activity.window.decorView.height
        }
        assertTrue("Editor window is actually landscape", landscapeWidth > landscapeHeight)

        val screenshot = captureWindow()
        File(context.getExternalFilesDir(null), "editor-landscape-toggle.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        compose.onNodeWithContentDescription("Switch to portrait editor").performClick()
        compose.waitUntil(10_000) {
            compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        }
        compose.onNodeWithContentDescription("Switch to landscape editor").assertExists()
        compose.onNodeWithTag("editor_preview").assertExists()
        compose.onNodeWithTag("preview_error").assertDoesNotExist()
        compose.onNodeWithTag("timeline_zoom_in").assertDoesNotExist()
        compose.onNodeWithTag("timeline_tracks_down").assertDoesNotExist()
        assertEquals(Player.STATE_READY, playerValue { it?.player?.playbackState })
    }

    private fun createPlaybackProject(): ProjectEntity {
        com.example.viewmodel.PerformanceModeManager.setMode(
            context,
            com.example.viewmodel.PerformanceMode.BETTER_PERFORMANCE
        )
        val file = File(context.cacheDir, "editor-landscape-motion.mp4")
        InstrumentationRegistry.getInstrumentation().context.assets.open("preview-motion.mp4").use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        val uri = Uri.fromFile(file).toString()
        val state = EditorState(
            clips = listOf(
                MediaClip(
                    sourceUri = uri,
                    originalDurationMs = 30_000L,
                    trimEndMs = 30_000L
                )
            ),
            canvasSettings = CanvasSettingsState(backgroundType = BackgroundType.Color)
        )
        val project = ProjectEntity(
            name = "Landscape regression",
            duration = "00:30",
            sourceMediaPaths = listOf(uri),
            historyState = editorHistoryMoshi.adapter(EditorHistoryModel::class.java)
                .toJson(EditorHistoryModel(currentState = state))
        )
        runBlocking { AppDatabase.getDatabase(context).projectDao().insertProject(project) }
        return project
    }

    private fun invokeTransport(contentDescription: String) {
        val action = compose.onNodeWithContentDescription(contentDescription).fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action
        compose.runOnUiThread { check(action?.invoke() == true) }
    }

    private fun <T> playerValue(block: (PlayerView?) -> T): T {
        var value: Any? = null
        compose.runOnUiThread {
            var result: PlayerView? = null
            fun visit(view: View) {
                if (view is PlayerView && view.player?.volume != 0f) result = view
                if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
            }
            visit(compose.activity.window.decorView)
            value = block(result)
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    private fun captureWindow(): Bitmap {
        var screenshot: Bitmap? = null
        var result = -1
        val latch = java.util.concurrent.CountDownLatch(1)
        compose.runOnUiThread {
            val root = compose.activity.window.decorView
            screenshot = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            android.view.PixelCopy.request(
                compose.activity.window,
                screenshot!!,
                {
                    result = it
                    latch.countDown()
                },
                android.os.Handler(android.os.Looper.getMainLooper())
            )
        }
        check(latch.await(10, java.util.concurrent.TimeUnit.SECONDS)) { "Window capture timed out" }
        assertEquals(android.view.PixelCopy.SUCCESS, result)
        return screenshot!!
    }
}
