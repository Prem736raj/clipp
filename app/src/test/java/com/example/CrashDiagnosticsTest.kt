package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CrashDiagnosticsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun reset() {
        context.getSharedPreferences("clipp_crash_prefs", 0).edit().clear().commit()
        context.getSharedPreferences("clipp_privacy", 0).edit().clear().commit()
    }

    @Test fun fatalReportIncludesStackAndCauseAndIsReadableBySupport() {
        CrashDiagnostics.record(context, "Editor", IllegalStateException("decoder failed", IllegalArgumentException("cause")), true)
        val log = CrashDiagnostics.read(context)
        assertTrue(log.contains("IllegalStateException: decoder failed"))
        assertTrue(log.contains("Caused by: java.lang.IllegalArgumentException: cause"))
        assertTrue(log.contains("CrashDiagnosticsTest"))
        assertTrue(context.getSharedPreferences("clipp_crash_prefs", 0).getBoolean("has_crashed", false))
    }

    @Test fun handledPreviewErrorDoesNotPretendAppCrashed() {
        CrashDiagnostics.record(context, "Preview", IllegalStateException("playback"))
        assertTrue(CrashDiagnostics.read(context).contains("playback"))
        assertFalse(context.getSharedPreferences("clipp_crash_prefs", 0).getBoolean("has_crashed", false))
    }

    @Test fun disabledReportingClearsLocalDiagnostics() {
        context.getSharedPreferences("clipp_privacy", 0).edit().putBoolean("crash_reporting_enabled", false).commit()
        CrashDiagnostics.record(context, "Editor", RuntimeException("private"), true)
        assertEquals("No recent crashes recorded.", CrashDiagnostics.read(context))
        assertFalse(context.getSharedPreferences("clipp_crash_prefs", 0).getBoolean("has_crashed", false))
    }
}
