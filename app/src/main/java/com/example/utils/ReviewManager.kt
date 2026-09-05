package com.example.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object ReviewManager {
    var lastSessionDurationMs: Long = 0L

    fun recordAppStart(context: Context) {
        val prefs = context.getSharedPreferences("clipp_review", Context.MODE_PRIVATE)
        val installTime = prefs.getLong("install_time", 0L)
        if (installTime == 0L) {
            prefs.edit().putLong("install_time", System.currentTimeMillis()).apply()
        }
    }

    fun checkOtherPositiveMoments(context: Context): Boolean {
        val prefs = context.getSharedPreferences("clipp_review", Context.MODE_PRIVATE)
        val hasShown = prefs.getBoolean("has_shown_review", false)
        if (hasShown) return false
        
        // Check 7 days
        val installTime = prefs.getLong("install_time", 0L)
        if (installTime > 0L) {
            val daysDiff = (System.currentTimeMillis() - installTime) / (1000 * 60 * 60 * 24)
            if (daysDiff >= 7) return true
        }
        
        // Check long session (e.g. 30 mins)
        val sessionDurationMin = lastSessionDurationMs / (1000 * 60)
        if (sessionDurationMin >= 30) return true
        
        return false
    }

    fun incrementExportCountAndCheckReview(context: Context): Boolean {
        val prefs = context.getSharedPreferences("clipp_review", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("export_count", 0) + 1
        prefs.edit().putInt("export_count", currentCount).apply()
        
        val hasShown = prefs.getBoolean("has_shown_review", false)
        return currentCount >= 3 && !hasShown
    }
    
    fun setReviewShown(context: Context) {
        val prefs = context.getSharedPreferences("clipp_review", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("has_shown_review", true).apply()
    }
    
    fun sendFeedbackEmail(context: Context, feedback: String? = null) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@clippapp.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Feedback for Clipp App")
            feedback?.let { putExtra(Intent.EXTRA_TEXT, it) }
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Send email..."))
        } catch (e: Exception) {
            Log.e("ReviewManager", "No email app found")
        }
    }
    
    fun launchNativeReview(activity: Activity) {
        try {
            val manager = com.google.android.play.core.review.ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = manager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        setReviewShown(activity)
                    }
                } else {
                    setReviewShown(activity)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Graceful fallback for environments without Play Core
            setReviewShown(activity)
        }
    }
}
