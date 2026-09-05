package com.example.camera

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Helper to provide single-shot haptic feedback when a barcode is successfully decoded,
 * with cooldown protection to avoid vibrating continuously across frames.
 */
object CameraFeedbackHelper {
    private var lastVibrationTime: Long = 0L
    private const val VIBRATION_COOLDOWN_MS: Long = 1000L

    fun triggerScanHapticFeedback(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastVibrationTime < VIBRATION_COOLDOWN_MS) {
            return
        }
        lastVibrationTime = now

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(70)
                }
            }
        } catch (_: Exception) {
            // Ignore if vibration permission or hardware unavailable
        }
    }
}
