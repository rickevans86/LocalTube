package com.localtube.app.core

import com.localtube.app.data.SettingsStore
import com.localtube.app.data.db.WatchDao
import java.util.Calendar

sealed interface LockStatus {
    data class Allowed(val remainingMs: Long) : LockStatus
    data object TimeUp : LockStatus
    data object Bedtime : LockStatus
}

/** Daily watch-time accounting and the bedtime window. Fail-closed. */
class ScreenTimeManager(
    private val watchDao: WatchDao,
    private val settings: SettingsStore,
) {
    suspend fun status(): LockStatus {
        val s = settings.current()
        if (s.bedtimeEnabled && inBedtime(s.bedtimeStartMinutes, s.bedtimeEndMinutes)) {
            return LockStatus.Bedtime
        }
        val usedMs = watchDao.watchedMsSince(startOfToday())
        val limitMs = s.dailyLimitMinutes * 60_000L
        val remaining = (limitMs - usedMs).coerceAtLeast(0)
        return if (remaining <= 0) LockStatus.TimeUp else LockStatus.Allowed(remaining)
    }

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun inBedtime(startMin: Int, endMin: Int): Boolean {
        val cal = Calendar.getInstance()
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return if (startMin <= endMin) {
            now in startMin until endMin
        } else {
            // Window wraps past midnight, e.g. 19:30 -> 07:00.
            now >= startMin || now < endMin
        }
    }
}
