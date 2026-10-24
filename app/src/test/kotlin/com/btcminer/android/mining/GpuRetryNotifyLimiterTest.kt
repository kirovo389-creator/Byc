package com.btcminer.android.mining

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpuRetryNotifyLimiterTest {

    @Test
    fun firstCall_notifiesImmediately() {
        var now = 1_000L
        val limiter = GpuRetryNotifyLimiter(notifyIntervalMs = 30_000L, nowMs = { now })
        assertTrue(limiter.shouldNotify())
    }

    @Test
    fun withinInterval_doesNotNotify() {
        var now = 1_000L
        val limiter = GpuRetryNotifyLimiter(notifyIntervalMs = 30_000L, nowMs = { now })
        assertTrue(limiter.shouldNotify())
        now = 10_000L
        assertFalse(limiter.shouldNotify())
        now = 30_999L
        assertFalse(limiter.shouldNotify())
    }

    @Test
    fun afterInterval_notifiesAgain() {
        var now = 1_000L
        val limiter = GpuRetryNotifyLimiter(notifyIntervalMs = 30_000L, nowMs = { now })
        assertTrue(limiter.shouldNotify())
        now = 31_000L
        assertTrue(limiter.shouldNotify())
    }

    @Test
    fun reset_allowsImmediateNotify() {
        var now = 1_000L
        val limiter = GpuRetryNotifyLimiter(notifyIntervalMs = 30_000L, nowMs = { now })
        assertTrue(limiter.shouldNotify())
        limiter.reset()
        now = 1_100L
        assertTrue(limiter.shouldNotify())
    }
}
