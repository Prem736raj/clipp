package com.example

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IncomingLaunchParserTest {
    @Test
    fun parsesEverySupportedItemFromMultiShare() {
        val first = Uri.parse("content://media/video/1")
        val second = Uri.parse("content://media/video/2")
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "video/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(first, second))
        }

        val launch = IncomingLaunchParser.parse(intent)

        assertEquals(listOf(first.toString(), second.toString()), launch.mediaUris)
    }

    @Test
    fun rejectsNetworkUriEvenWhenShareMimeLooksLikeVideo() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, Uri.parse("https://example.com/video.mp4"))
        }

        assertTrue(IncomingLaunchParser.parse(intent).mediaUris.isEmpty())
    }

    @Test
    fun acceptsOnlyKnownShortcutActions() {
        val known = Intent(Intent.ACTION_VIEW, Uri.parse("clipp://shortcut/continue_last"))
        val unknown = Intent(Intent.ACTION_VIEW, Uri.parse("clipp://shortcut/delete_everything"))

        assertEquals("continue_last", IncomingLaunchParser.parse(known).shortcutAction)
        assertEquals(null, IncomingLaunchParser.parse(unknown).shortcutAction)
    }

    @Test
    fun projectDeepLinkRequiresUuidProjectId() {
        val id = UUID.randomUUID().toString()
        val valid = Intent(Intent.ACTION_VIEW, Uri.parse("clipp://project/$id"))
        val invalid = Intent(Intent.ACTION_VIEW, Uri.parse("clipp://project/not-a-project-id"))

        assertEquals("open_$id", IncomingLaunchParser.parse(valid).shortcutAction)
        assertEquals(null, IncomingLaunchParser.parse(invalid).shortcutAction)
    }

    @Test
    fun openWithAcceptsSupportedFileExtensionWithoutMime() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("file:///storage/emulated/0/Movies/sample.mp4"))

        assertEquals(
            listOf("file:///storage/emulated/0/Movies/sample.mp4"),
            IncomingLaunchParser.parse(intent).mediaUris
        )
    }

    @Test
    fun nonPersistableProjectMediaIsCopiedIntoOwnedStorage() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val source = File.createTempFile("clipp-shared-", ".mp4").apply {
                writeBytes(byteArrayOf(1, 2, 3, 4, 5))
            }

            val result = IncomingMediaStore.makeProjectMediaDurable(
                context = context,
                uriStrings = listOf(Uri.fromFile(source).toString()),
                incomingIntentFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            assertTrue(result.isSuccess)
            val copiedUri = Uri.parse(result.getOrThrow().single())
            val copiedFile = File(requireNotNull(copiedUri.path))
            assertTrue(copiedFile.exists())
            assertTrue(copiedFile.canonicalPath.startsWith(File(context.filesDir, "imports").canonicalPath))
            assertEquals(source.readBytes().toList(), copiedFile.readBytes().toList())

            copiedFile.delete()
            source.delete()
        }
    }
}
