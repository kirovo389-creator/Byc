package com.btcminer.android.mining

/**
 * Global constants for mining (reconnect, queue, etc.).
 */
object MiningConstants {
    /** Stratum reconnect retry delay in seconds. Used for both WiFi and cell. */
    const val STRATUM_RECONNECT_RETRY_DELAY_SEC = 10//60 //300
    /** Interval (ms) between GPU init retry attempts when GPU is unavailable. */
    const val GPU_RETRY_INTERVAL_MS = 3_000L //60_000L
    /** Minimum interval between GPU-retry user notifications (Snackbar). */
    const val GPU_RETRY_NOTIFY_INTERVAL_MS = 30_000L
    /** Process-death mining resume watchdog interval. Independent of wake-lock alarm. */
    const val PROCESS_RESUME_WATCHDOG_INTERVAL_MS = 180_000L
    /** If GPU worker produces no nonces for this duration, treat as stuck and request interrupt. */
    const val WORKER_STUCK_TIMEOUT_MS = 90_000L
    /** If any worker is still alive after this round duration, interrupt all (GPU + CPU + sleeping). */
    const val ROUND_STUCK_TIMEOUT_MS = 900_000L
}
