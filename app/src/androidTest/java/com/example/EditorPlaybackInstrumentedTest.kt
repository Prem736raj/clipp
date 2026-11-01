package com.example

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
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
import com.example.viewmodel.ProjectViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Exercises the actual editor, decoder and native texture, rather than only exporting. */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class EditorPlaybackInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val ids = mutableListOf<String>()

    @After fun cleanup() = runBlocking {
        ids.forEach { AppDatabase.getDatabase(context).projectDao().deleteProjectById(it) }
    }

    @Test fun importedVideoShowsChangingFramesAndAdvancesClock() {
        openEditor(historyOnly = false)
        verifyMotionAndTransport()
    }

    @Test fun restoredTimelineWithoutLegacyPathsPlaysAndSurvivesFullscreen() {
        openEditor(historyOnly = true)
        verifyMotionAndTransport()
        compose.onNodeWithContentDescription("Fullscreen").performClick()
        compose.onNodeWithContentDescription("Exit Fullscreen").performClick()
        compose.waitUntil(15_000) { playerValue { it?.player?.playbackState == Player.STATE_READY } }
        togglePlayback()
        compose.waitUntil(15_000) { playerValue { it?.player?.isPlaying == true } }
        compose.waitUntil(15_000) { bitmap()?.let { !isBlack(it) } == true }
        val frame = bitmap()
        assertTrue("Returning from fullscreen retains the video surface", frame != null && !isBlack(frame))
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        assertTrue("Backgrounding pauses the decoder", playerValue { it?.player?.isPlaying == false })
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        togglePlayback()
        compose.waitUntil(10_000) { playerValue { it?.player?.isPlaying == true } }
    }

    @Test fun returningUserGetsOneRecoveryDialogAndOneStatusBarInset() {
        context.getSharedPreferences("clipp_prefs", 0).edit().putBoolean("has_completed_onboarding", true).commit()
        context.getSharedPreferences("clipp_privacy", 0).edit().putBoolean("has_seen_consent", true).commit()
        context.getSharedPreferences("clipp_crash_prefs", 0).edit().putBoolean("has_crashed", true).commit()
        val project = ProjectEntity(name = "Interrupted session", duration = "00:08", isDirty = true)
        ids += project.id
        runBlocking { AppDatabase.getDatabase(context).projectDao().insertProject(project) }
        compose.runOnUiThread { compose.activity.enableEdgeToEdge() }
        compose.setContent { ClippApp() }
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("Recover Unsaved Project").fetchSemanticsNodes().size == 1
        }
        compose.onAllNodesWithText("We recovered your last session").assertCountEquals(0)
        compose.onNodeWithText("Discard").performClick()
        compose.onNodeWithText("Choose media privately").assertDoesNotExist()
        val greeting = compose.onNode(hasText("Good", substring = true)).fetchSemanticsNode().boundsInRoot
        var statusTop = 0
        var density = 1f
        compose.runOnUiThread {
            statusTop = androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)!!
                .getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
            density = compose.activity.resources.displayMetrics.density
        }
        assertEquals("Home starts directly after one status inset and its 16dp header padding",
            statusTop + 16 * density, greeting.top, 2 * density)
    }

    @Test fun overlappingVideoBackedAudioClipsDoNotExhaustPlaybackResources() {
        openEditor(historyOnly = true, videoBackedAudioClipCount = 10)
        togglePlayback()
        compose.waitUntil(15_000) { playerValue { it?.player?.isPlaying == true } }
        compose.waitUntil(15_000) { playerValue { it?.player?.currentPosition?.let { pos -> pos > 2_000 } == true } }
        compose.onNodeWithTag("preview_error").assertDoesNotExist()

        val frame = bitmap()
        assertTrue("Main preview stays rendered while many timeline audio players are active",
            frame != null && !isBlack(frame))
    }

    private fun openEditor(historyOnly: Boolean, videoBackedAudioClipCount: Int = 0) {
        com.example.viewmodel.PerformanceModeManager.setMode(context, com.example.viewmodel.PerformanceMode.BETTER_PERFORMANCE)
        val file = File(context.cacheDir, "editor-motion.mp4")
        InstrumentationRegistry.getInstrumentation().context.assets.open("preview-motion.mp4").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val uri = Uri.fromFile(file).toString()
        val videoBackedAudio = List(videoBackedAudioClipCount) { index ->
            AudioClip(
                displayName = "Stress audio ${index + 1}",
                sourceUri = uri,
                startTimeOnTimelineMs = 0L,
                sourceDurationMs = 30_000L,
                trimEndMs = 30_000L
            )
        }
        val state = EditorState(clips = listOf(MediaClip(sourceUri = uri, originalDurationMs = 30_000, trimEndMs = 30_000)),
            audioClips = videoBackedAudio,
            canvasSettings = CanvasSettingsState(backgroundType = BackgroundType.Color))
        val project = ProjectEntity(name = "Playback regression", duration = "00:30",
            sourceMediaPaths = if (historyOnly) emptyList() else listOf(uri),
            historyState = if (historyOnly) editorHistoryMoshi.adapter(EditorHistoryModel::class.java)
                .toJson(EditorHistoryModel(currentState = state)) else "")
        ids += project.id
        runBlocking { AppDatabase.getDatabase(context).projectDao().insertProject(project) }
        compose.runOnUiThread { compose.activity.enableEdgeToEdge() }
        compose.setContent {
            MaterialTheme { EditorScreen(project.id, {}) }
        }
        compose.waitUntil(30_000) { playerValue { it?.player?.playbackState == Player.STATE_READY } }
        compose.waitUntil(15_000) { bitmap()?.let { !isBlack(it) } == true }
        compose.onNodeWithTag("editor_duration").assertTextEquals("00:30")
    }

    private fun verifyMotionAndTransport() {
        val first = bitmap() ?: error("First frame was not rendered into a TextureView")
        assertFalse("Initial preview must contain video", isBlack(first))
        togglePlayback()
        compose.waitUntil(15_000) { playerValue { it?.player?.currentPosition?.let { pos -> pos > 1_200 } == true } }
        val later = bitmap() ?: error("Playing frame missing")
        assertFalse("Frames must change during playback", first.sameAs(later))
        val beforePause = playerValue { "position=${it?.player?.currentPosition}, playing=${it?.player?.isPlaying}, state=${it?.player?.playbackState}" }
        compose.runOnUiThread { transportAction!!.invoke() }
        compose.waitUntil(5_000) { playerValue { it?.player?.isPlaying == false } }
        val pausedClock = compose.onNodeWithTag("editor_position").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text
        assertNotEquals("The editor clock advances with decoded playback ($beforePause)", "00:00", pausedClock)
        val screenshot = captureWindow()
        val previewBounds = compose.onNodeWithTag("editor_preview").fetchSemanticsNode().boundsInWindow
        val visiblePixel = screenshot.getPixel(previewBounds.center.x.toInt(), previewBounds.center.y.toInt())
        assertTrue("The composed preview is visible on screen", android.graphics.Color.red(visiblePixel) +
            android.graphics.Color.green(visiblePixel) + android.graphics.Color.blue(visiblePixel) > 24)
        File(context.getExternalFilesDir(null), "editor-playback-verified.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        // The editor header also has a Back action; use the lower transport control.
        val back = compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes()
            .filter { it.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick) }
            .maxBy { it.boundsInRoot.top }
        compose.runOnUiThread { back.config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action!!.invoke() }
        compose.waitUntil(5_000) {
            compose.onNodeWithTag("editor_position").fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text == "00:00"
        }
        assertTrue("Rewind seeks the player to the start", playerValue { it?.player?.currentPosition == 0L })
    }

    private fun captureWindow(): Bitmap {
        var screenshot: Bitmap? = null
        var result = -1
        val latch = java.util.concurrent.CountDownLatch(1)
        compose.runOnUiThread {
            val root = compose.activity.window.decorView
            screenshot = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            android.view.PixelCopy.request(compose.activity.window, screenshot!!, {
                result = it
                latch.countDown()
            }, android.os.Handler(android.os.Looper.getMainLooper()))
        }
        check(latch.await(10, java.util.concurrent.TimeUnit.SECONDS)) { "Window capture timed out" }
        assertEquals("Native window capture succeeds", android.view.PixelCopy.SUCCESS, result)
        return screenshot!!
    }

    private var transportAction: (() -> Boolean)? = null

    private fun togglePlayback() {
        // Invoke the UI's own click action without Espresso waiting for a
        // continuously recomposing video to finish before returning.
        transportAction = compose.onNodeWithContentDescription("Play").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsActions.OnClick].action
        compose.runOnUiThread { check(transportAction!!.invoke()) }
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

    private fun bitmap(): Bitmap? = playerValue { (it?.videoSurfaceView as? TextureView)?.bitmap }

    private fun isBlack(bitmap: Bitmap): Boolean = (1..4).all { x -> (1..4).all { y ->
        val color = bitmap.getPixel(bitmap.width * x / 5, bitmap.height * y / 5)
        android.graphics.Color.red(color) + android.graphics.Color.green(color) + android.graphics.Color.blue(color) < 24
    } }
}
