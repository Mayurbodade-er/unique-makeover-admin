package com.uniquemakeover.admin.core.security

import com.uniquemakeover.admin.feature.auth.domain.model.LockoutState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockoutPolicy @Inject constructor() {

    fun evaluate(
        failCount: Int,
        lastFailElapsedRealtime: Long,
        currentElapsedRealtime: Long
    ): LockoutState {
        if (failCount < MAX_FAIL_ATTEMPTS) {
            return LockoutState.Unlocked
        }

        // EC-AUTH-02.2: Monotonic reboot recovery
        val elapsedSinceLock = if (currentElapsedRealtime < lastFailElapsedRealtime) {
            currentElapsedRealtime
        } else {
            currentElapsedRealtime - lastFailElapsedRealtime
        }

        val remainingMillis = LOCKOUT_DURATION_MILLIS - elapsedSinceLock
        return if (remainingMillis > 0) {
            val remainingSeconds = ((remainingMillis + MILLIS_ROUND_UP) / MILLIS_PER_SECOND).toInt()
            LockoutState.Locked(remainingSeconds.coerceAtLeast(1))
        } else {
            LockoutState.Unlocked
        }
    }

    companion object {
        const val MAX_FAIL_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MILLIS = 30_000L
        const val LOCKOUT_SECONDS = 30
        private const val MILLIS_PER_SECOND = 1000L
        private const val MILLIS_ROUND_UP = 999L
    }
}
