package com.btcminer.android.mining

import org.junit.Assert.assertEquals
import org.junit.Test

class HashRateDisplayTest {

    @Test
    fun hashing_showsRateEvenAtZero() {
        assertEquals(
            HashRateDisplay.RowStatus.Hashing,
            HashRateDisplay.rowStatus(
                backendWanted = true,
                stateMining = true,
                connectingOrResuming = false,
                backendRetrying = false,
            ),
        )
    }

    @Test
    fun gpuRetryWhileMining_isInitializing() {
        assertEquals(
            HashRateDisplay.RowStatus.Initializing,
            HashRateDisplay.rowStatus(
                backendWanted = true,
                stateMining = true,
                connectingOrResuming = false,
                backendRetrying = true,
            ),
        )
    }

    @Test
    fun resumeConnect_isInitializingWhenWanted() {
        assertEquals(
            HashRateDisplay.RowStatus.Initializing,
            HashRateDisplay.rowStatus(
                backendWanted = true,
                stateMining = false,
                connectingOrResuming = true,
                backendRetrying = false,
            ),
        )
    }

    @Test
    fun gpuOff_isDashNotInitializing() {
        assertEquals(
            HashRateDisplay.RowStatus.Off,
            HashRateDisplay.rowStatus(
                backendWanted = false,
                stateMining = true,
                connectingOrResuming = true,
                backendRetrying = false,
            ),
        )
    }

    @Test
    fun idleNotRequested_isOff() {
        assertEquals(
            HashRateDisplay.RowStatus.Off,
            HashRateDisplay.rowStatus(
                backendWanted = true,
                stateMining = false,
                connectingOrResuming = false,
                backendRetrying = false,
            ),
        )
    }

    @Test
    fun connectingOrResuming_fromStates() {
        assertEquals(
            true,
            HashRateDisplay.connectingOrResuming(
                state = MiningStatus.State.Connecting,
                miningRequested = false,
                startInProgress = false,
            ),
        )
        assertEquals(
            true,
            HashRateDisplay.connectingOrResuming(
                state = MiningStatus.State.Idle,
                miningRequested = true,
                startInProgress = false,
            ),
        )
        assertEquals(
            false,
            HashRateDisplay.connectingOrResuming(
                state = MiningStatus.State.Mining,
                miningRequested = true,
                startInProgress = false,
            ),
        )
    }
}
