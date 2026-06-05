/*
 * Copyright (C) 2026 Elvettorato
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.elvettorato.routine.trigger

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.elvettorato.routine.data.Trigger
import com.elvettorato.routine.receiver.AlarmReceiver
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Schedules time-of-day triggers via [AlarmManager.setExactAndAllowWhileIdle] so
 * they fire on Doze devices. We reschedule after every fire (one-shot pattern)
 * because [daysOfWeek] makes a true periodic alarm impractical.
 */
internal class TimeTriggerScheduler(private val context: Context) {

    private val am: AlarmManager = context.getSystemService(AlarmManager::class.java)
        ?: error("AlarmManager unavailable")

    fun schedule(routineId: String, trigger: Trigger.Time) {
        val nextMillis = nextOccurrenceMillis(trigger) ?: return
        val pi = pendingIntentFor(routineId, trigger)
        try {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMillis, pi)
            } else {
                // Fallback to inexact when SCHEDULE_EXACT_ALARM is revoked
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMillis, pi)
            }
            Log.i(TriggerScheduler.TAG, "Scheduled time trigger $routineId@${trigger.id} for $nextMillis")
        } catch (sec: SecurityException) {
            Log.w(TriggerScheduler.TAG, "Lost exact-alarm permission; using inexact", sec)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMillis, pi)
        }
    }

    fun cancel(routineId: String, trigger: Trigger.Time) {
        am.cancel(pendingIntentFor(routineId, trigger))
    }

    private fun pendingIntentFor(routineId: String, trigger: Trigger.Time): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            putExtra(AlarmReceiver.EXTRA_ROUTINE_ID, routineId)
            putExtra(AlarmReceiver.EXTRA_TRIGGER_ID, trigger.id)
            // Unique data URI so requests don't collapse
            data = android.net.Uri.parse("routine://time/$routineId/${trigger.id}")
        }
        val requestCode = (routineId + trigger.id).hashCode()
        return PendingIntent.getBroadcast(context, requestCode, intent, pendingIntentFlags())
    }

    /** Computes the next wall-clock instant matching this trigger, or null if none. */
    private fun nextOccurrenceMillis(t: Trigger.Time): Long? {
        if (t.daysOfWeek == 0) return null
        val now = LocalDateTime.now()
        val target = LocalTime.of(t.hour, t.minute)
        // Try today + next 7 days, pick the first whose day-of-week bit is set
        for (offset in 0..7) {
            val candidate = now.toLocalDate().plusDays(offset.toLong()).atTime(target)
            if (offset == 0 && !candidate.isAfter(now)) continue
            val dowBit = 1 shl ((candidate.dayOfWeek.value - 1)) // Mon=1 → bit0
            if ((t.daysOfWeek and dowBit) != 0) {
                return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
        return null
    }
}
