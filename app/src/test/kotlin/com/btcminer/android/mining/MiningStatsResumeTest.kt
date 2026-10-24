package com.btcminer.android.mining

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MiningStatsResumeTest {

    private lateinit var repository: MiningStatsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("mining_stats", Context.MODE_PRIVATE).edit().clear().apply()
        repository = MiningStatsRepository(context)
    }

    @Test
    fun resumeCount_defaultZero() {
        assertEquals(0L, repository.getResumeAttemptCount())
    }

    @Test
    fun resumeCount_increments() {
        assertEquals(1L, repository.incrementResumeAttemptCount())
        assertEquals(2L, repository.incrementResumeAttemptCount())
        assertEquals(2L, repository.getResumeAttemptCount())
    }

    @Test
    fun resumeCount_clearsOnNewSession() {
        repository.incrementResumeAttemptCount()
        repository.saveSessionAccumulatedHashedMs(5_000L)
        repository.clearResumeStateForNewSession()
        assertEquals(0L, repository.getResumeAttemptCount())
        assertEquals(0L, repository.getSessionAccumulatedHashedMs())
    }

    @Test
    fun accumulated_notResetWhenRequestedFlagStays() {
        repository.saveSessionAccumulatedHashedMs(12_000L)
        repository.setMiningRequested(true)
        assertTrue(repository.isMiningRequested())
        assertEquals(12_000L, repository.getSessionAccumulatedHashedMs())
        repository.setMiningRequested(false)
        assertFalse(repository.isMiningRequested())
        assertEquals(12_000L, repository.getSessionAccumulatedHashedMs())
    }

    @Test
    fun shareBaselines_roundTrip() {
        repository.saveShareSessionBaselines(
            MiningStatsRepository.ShareSessionBaselines(1, 2, 3, 4, 5, 6),
        )
        val b = repository.getShareSessionBaselines()
        assertEquals(1L, b.accepted)
        assertEquals(2L, b.rejected)
        assertEquals(3L, b.identified)
        assertEquals(4L, b.identifiedCpu)
        assertEquals(5L, b.identifiedGpu)
        assertEquals(6L, b.blockTemplates)
    }
}
