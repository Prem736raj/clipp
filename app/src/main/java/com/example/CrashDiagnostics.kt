package com.example

import android.content.Context
import android.os.Build

/** Local diagnostics only; sending a report remains an explicit support action. */
internal object CrashDiagnostics {
    private var installed = false

    @Synchronized
    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { record(app, "Uncaught exception on ${thread.name}", error, fatal = true) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun record(context: Context, event: String, error: Throwable, fatal: Boolean = false) {
        val prefs = context.getSharedPreferences("clipp_crash_prefs", Context.MODE_PRIVATE)
        if (!context.getSharedPreferences("clipp_privacy", Context.MODE_PRIVATE)
                .getBoolean("crash_reporting_enabled", true)) {
            prefs.edit().clear().commit()
            return
        }
        val report = "$event\nDevice: ${Build.MANUFACTURER} ${Build.MODEL}, API ${Build.VERSION.SDK_INT}\n" +
            "App: ${BuildConfig.VERSION_NAME}\n${error.stackTraceToString()}"
        prefs.edit().putString("last_crash_log", report.take(64_000)).apply {
            if (fatal) putBoolean("has_crashed", true)
        }.commit()
    }

    fun read(context: Context): String {
        val prefs = context.getSharedPreferences("clipp_crash_prefs", Context.MODE_PRIVATE)
        return prefs.getString("last_crash_log", null)
            ?: prefs.getString("crash_log", null) ?: "No recent crashes recorded."
    }
}
