package com.example.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdater {
    fun updateWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ClippSmallWidget().updateAll(context)
                ClippMediumWidget().updateAll(context)
                ClippLargeWidget().updateAll(context)
            } catch (e: Exception) {
                // Ignore update errors
            }
        }
    }
}
