package com.btcminer.android.mining

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiningResumePolicyTest {

    @Test
    fun wakeup_withMiningRequested_startsResume() {
        assertTrue(
            MiningResumePolicy.shouldAttemptResume(
                miningRequested = true,
                engineRunning = false,
                startInProgress = false,
            ),
        )
        assertTrue(MiningResumePolicy.shouldIncrementResumeCount(freshSession = false))
    }

    @Test
    fun wakeup_afterUserStop_doesNotResume() {
        assertFalse(
            MiningResumePolicy.shouldAttemptResume(
                miningRequested = false,
                engineRunning = false,
                startInProgress = false,
            ),
        )
    }

    @Test
    fun wakeup_whileEngineRunning_doesNotResume() {
        assertFalse(
            MiningResumePolicy.shouldAttemptResume(
                miningRequested = true,
                engineRunning = true,
                startInProgress = false,
            ),
        )
    }

    @Test
    fun wakeup_whileStartInProgress_doesNotDoubleStart() {
        assertFalse(
            MiningResumePolicy.shouldAttemptResume(
                miningRequested = true,
                engineRunning = false,
                startInProgress = true,
            ),
        )
    }

    @Test
    fun chargingConstraint_clearsRequested() {
        assertTrue(
            MiningResumePolicy.shouldClearRequestedBecauseChargingConstraint(
                mineOnlyWhenCharging = true,
                chargingOk = false,
            ),
        )
        assertFalse(
            MiningResumePolicy.shouldClearRequestedBecauseChargingConstraint(
                mineOnlyWhenCharging = true,
                chargingOk = true,
            ),
        )
        assertFalse(
            MiningResumePolicy.shouldClearRequestedBecauseChargingConstraint(
                mineOnlyWhenCharging = false,
                chargingOk = false,
            ),
        )
    }
}
