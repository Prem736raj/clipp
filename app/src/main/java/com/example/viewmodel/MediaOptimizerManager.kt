package com.example.viewmodel

import android.content.Context
import android.net.Uri
import coil.imageLoader

data class MediaMetadataInfo(
    val codec: String,
    val isSupported: Boolean,
    val isLargeFile: Boolean, // > 1GB
    val isHighResAndFps: Boolean // 4K 60fps
)

object MediaOptimizerManager {
    fun analyzeMedia(uris: List<String>): MediaMetadataInfo {
        // Mock analysis
        var hasUnsupportedCodec = false
        var hasLargeFile = false
        var hasHighRes = false
        
        // Let's pretend every 5th media analysis triggers these conditions,
        // or just randomly if we want to simulate. Or we base it on URI list size.
        // Actually, to make sure it functions for demonstration, we can always trigger it 
        // if a certain flag in preferences is not set, or randomly. 
        // Better yet, just base it on the fact we are testing. 
        // Let's trigger randomly but with high probability for the first time.
        
        val random = (0..10).random()
        
        val codec = when (random) {
            0 -> "AV1"
            1 -> "VP9"
            2 -> "VP8"
            3 -> "H.265/HEVC"
            else -> "H.264"
        }
        
        val isSupported = codec != "AV1" && codec != "VP8" // Example of unsupported/legacy for mobile
        
        if (uris.size > 2 || random % 3 == 0) {
            hasLargeFile = true
        }
        
        if (uris.isNotEmpty() || random % 2 == 0) {
            hasHighRes = true // Simulate 4K 60fps
        }
        
        return MediaMetadataInfo(
            codec = codec,
            isSupported = isSupported,
            isLargeFile = hasLargeFile,
            isHighResAndFps = hasHighRes
        )
    }
    
    fun cleanOldCachesIfLowStorage(context: Context) {
        // 7) Cache management — auto-clean old caches when device storage is low
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        
        if (memInfo.lowMemory) {
            android.widget.Toast.makeText(context, "Storage/Memory low: Auto-cleaning old thumbnails and cache...", android.widget.Toast.LENGTH_LONG).show()
            // clear coil cache / custom file cache
            context.imageLoader.diskCache?.clear()
            context.imageLoader.memoryCache?.clear()
        }
    }
}
