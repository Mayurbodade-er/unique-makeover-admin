package com.uniquemakeover.admin.core.security

import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.feature.auth.domain.model.LockoutState
import org.junit.Before
import org.junit.Test

class LockoutPolicyTest {

    private lateinit var policy: LockoutPolicy

    @Before
    fun setUp() {
        policy = LockoutPolicy()
    }

    @Test
    fun evaluate_underFiveAttempts_returnsUnlocked() {
        for (i in 0..4) {
            val state = policy.evaluate(
                failCount = i,
                lastFailElapsedRealtime = 1000L,
                currentElapsedRealtime = 2000L
            )
            assertThat(state).isEqualTo(LockoutState.Unlocked)
        }
    }

    @Test
    fun evaluate_atFiveAttempts_zeroElapsed_returnsLockedThirtySeconds() {
        val state = policy.evaluate(
            failCount = 5,
            lastFailElapsedRealtime = 5000L,
            currentElapsedRealtime = 5000L
        )
        assertThat(state).isEqualTo(LockoutState.Locked(30))
    }

    @Test
    fun evaluate_atFiveAttempts_fifteenSecondsElapsed_returnsLockedFifteenSeconds() {
        val state = policy.evaluate(
            failCount = 5,
            lastFailElapsedRealtime = 5000L,
            currentElapsedRealtime = 20000L
        )
        assertThat(state).isEqualTo(LockoutState.Locked(15))
    }

    @Test
    fun evaluate_atFiveAttempts_twentyNineSecondsElapsed_returnsLockedOneSecond() {
        val state = policy.evaluate(
            failCount = 5,
            lastFailElapsedRealtime = 1000L,
            currentElapsedRealtime = 30000L
        )
        assertThat(state).isEqualTo(LockoutState.Locked(1))
    }

    @Test
    fun evaluate_atFiveAttempts_thirtySecondsElapsed_returnsUnlocked() {
        val state = policy.evaluate(
            failCount = 5,
            lastFailElapsedRealtime = 1000L,
            currentElapsedRealtime = 31000L
        )
        assertThat(state).isEqualTo(LockoutState.Unlocked)
    }

    @Test
    fun evaluate_deviceReboot_currentLessThanLastFail_enforcesPenaltyFromBootTime() {
        // Last fail was at 50_000ms uptime before reboot. Reboot happened, current uptime is 5_000ms.
        val state = policy.evaluate(
            failCount = 5,
            lastFailElapsedRealtime = 50000L,
            currentElapsedRealtime = 5000L
        )
        // 30_000L - 5_000L = 25_000L remaining -> 25s
        assertThat(state).isEqualTo(LockoutState.Locked(25))
    }
}
