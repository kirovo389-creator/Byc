package com.btcminer.android.mining

/**
 * Rate-limits GPU-retry UI notifications: first failure immediately, then at most once per
 * [MiningConstants.GPU_RETRY_NOTIFY_INTERVAL_MS] while retries continue every few seconds.
 */
internal class GpuRetryNotifyLimiter(
    private val notifyIntervalMs: Long = MiningConstants.GPU_RETRY_NOTIFY_INTERVAL_MS,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private var lastNotifyMs: Long = 0L

    fun shouldNotify(): Boolean {
        val now = nowMs()
        if (lastNotifyMs == 0L || now - lastNotifyMs >= notifyIntervalMs) {
            lastNotifyMs = now
            return true
        }
        return false
    }

    fun reset() {
        lastNotifyMs = 0L
    }
}
