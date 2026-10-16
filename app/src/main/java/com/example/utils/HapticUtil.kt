package com.example.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View

object HapticUtil {
    private fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences("clipp_settings", Context.MODE_PRIVATE).getBoolean("haptics_enabled", true)
    }

    fun playSubtleTick(view: View, context: Context) {
        if (!isEnabled(context)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            }
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun playSharpTap(view: View, context: Context) {
        if (!isEnabled(context)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            }
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
    
    fun playLightTap(view: View, context: Context) {
        if (!isEnabled(context)) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun playMediumBuzz(view: View, context: Context) {
         if (!isEnabled(context)) return
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
             val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
             if (vibrator.hasVibrator()) {
                 vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
             }
         } else {
             view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
         }
    }
    
    fun playSuccess(view: View, context: Context) {
        if (!isEnabled(context)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
             view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
             view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}
