package com.btcminer.android.mining

/**
 * Pure decisions for process-death auto-resume. Kept free of Android types so unit tests can
 * cover watchdog / sticky-restart behavior without a Service.
 */
object MiningResumePolicy {

    fun shouldAttemptResume(
        miningRequested: Boolean,
        engineRunning: Boolean,
        startInProgress: Boolean,
    ): Boolean = miningRequested && !engineRunning && !startInProgress

    fun shouldIncrementResumeCount(freshSession: Boolean): Boolean = !freshSession

    fun shouldClearRequestedBecauseChargingConstraint(
        mineOnlyWhenCharging: Boolean,
        chargingOk: Boolean,
    ): Boolean = mineOnlyWhenCharging && !chargingOk
}
