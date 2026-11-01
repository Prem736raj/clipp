package com.example

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExportSuccessScreenTest {
    @Test
    fun shareIntentGrantsReadAccessThroughExtraAndClipData() {
        val uri = Uri.parse("content://com.example.fileprovider/exports/video.mp4")

        val intent = buildExportShareIntent(uri)

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("video/mp4", intent.type)
        assertEquals(uri, intent.getParcelableExtra(Intent.EXTRA_STREAM))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertNotNull(intent.clipData)
        assertEquals(uri, intent.clipData!!.getItemAt(0).uri)
    }
}
