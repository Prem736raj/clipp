package com.example.viewmodel

import android.app.ActivityManager
import android.content.Context

enum class PerformanceMode(val displayName: String) {
    BETTER_PERFORMANCE("Better Performance"),
    BALANCED("Balanced"),
    BEST_QUALITY("Best Quality")
}

object PerformanceModeManager {
    var currentMode: PerformanceMode? = null
    var hasShownBudgetTip = false
    
    fun getMode(context: Context): PerformanceMode {
        if (currentMode != null) return currentMode!!
        
        val pref = context.getSharedPreferences("clipp_prefs", Context.MODE_PRIVATE)
        val savedMode = pref.getString("performance_mode", null)
        if (savedMode != null) {
            currentMode = PerformanceMode.valueOf(savedMode)
            return currentMode!!
        }
        
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        
        val totalRamGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        
        currentMode = when {
            totalRamGb <= 4.5 -> PerformanceMode.BETTER_PERFORMANCE
            totalRamGb <= 6.5 -> PerformanceMode.BALANCED
            else -> PerformanceMode.BEST_QUALITY
        }
        
        pref.edit().putString("performance_mode", currentMode!!.name).apply()
        return currentMode!!
    }
    
    fun setMode(context: Context, mode: PerformanceMode) {
        currentMode = mode
        val pref = context.getSharedPreferences("clipp_prefs", Context.MODE_PRIVATE)
        pref.edit().putString("performance_mode", mode.name).apply()
    }
}
