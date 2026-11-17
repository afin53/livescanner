package com.example.scanner.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticFeedbackManager(context: Context) {

  private val vibrator: Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      manager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

  private var lastVibratedAtMs: Long = 0L

  fun pulseLockOn(cooldownMs: Long = 550L) {
    val now = System.currentTimeMillis()
    if (now - lastVibratedAtMs < cooldownMs) return
    val vib = vibrator ?: return
    if (!vib.hasVibrator()) return

    lastVibratedAtMs = now
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vib.vibrate(VibrationEffect.createOneShot(22L, 90))
      } else {
        @Suppress("DEPRECATION")
        vib.vibrate(22L)
      }
    }
  }
}
