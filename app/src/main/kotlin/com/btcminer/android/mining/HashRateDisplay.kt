package com.btcminer.android.mining

/**
 * Page-1 CPU/GPU hash-rate row: numeric rate while hashing, Initializing during connect/retry/resume,
 * em dash when that backend is off or mining is idle.
 */
object HashRateDisplay {

    enum class RowStatus { Hashing, Initializing, Off }

    fun rowStatus(
        backendWanted: Boolean,
        stateMining: Boolean,
        connectingOrResuming: Boolean,
        backendRetrying: Boolean,
    ): RowStatus {
        if (!backendWanted) return RowStatus.Off
        if (backendRetrying) return RowStatus.Initializing
        if (stateMining) return RowStatus.Hashing
        if (connectingOrResuming) return RowStatus.Initializing
        return RowStatus.Off
    }

    fun connectingOrResuming(
        state: MiningStatus.State,
        miningRequested: Boolean,
        startInProgress: Boolean,
    ): Boolean {
        if (startInProgress) return true
        if (state == MiningStatus.State.Connecting) return true
        return miningRequested && state != MiningStatus.State.Mining
    }
}
