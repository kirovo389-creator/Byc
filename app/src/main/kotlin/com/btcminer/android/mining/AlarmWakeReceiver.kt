package com.btcminer.android.mining

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

/**
 * Receives ELAPSED_REALTIME_WAKEUP alarms and starts MiningForegroundService.
 * Optionally holds a short PARTIAL_WAKE_LOCK so the process stays up until the service handles the intent.
 */
class AlarmWakeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val action = intent?.action ?: return
        if (action != MiningForegroundService.ACTION_ALARM_WAKEUP &&
            action != MiningForegroundService.ACTION_WATCHDOG
        ) {
            return
        }
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        val shortLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "btcminer:alarm").apply {
            setReferenceCounted(false)
            acquire(10_000L) // 10 s max so service can run
        }
        try {
            MiningForegroundService.startAsForeground(context, action)
        } finally {
            if (shortLock.isHeld) shortLock.release()
        }
    }
}
