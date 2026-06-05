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
import android.os.Build
import android.util.Log
import com.elvettorato.routine.data.Routine
import com.elvettorato.routine.data.RoutineRepository
import com.elvettorato.routine.data.Trigger
import com.elvettorato.routine.receiver.AlarmReceiver

/**
 * Top-level orchestrator that wires per-routine triggers into AlarmManager
 * (time triggers) and LocationManager proximity alerts / WorkManager fallback
 * polling (location triggers).
 */
class TriggerScheduler(
    private val context: Context,
    private val repo: RoutineRepository,
) {

    private val time = TimeTriggerScheduler(context)
    private val location = LocationTriggerScheduler(context, repo)

    fun scheduleAllEnabled() {
        repo.enabledRoutines().forEach { schedule(it) }
        location.startPollingFallbackIfNeeded()
    }

    fun cancelAll() {
        repo.all().forEach { cancel(it) }
        location.stopPolling()
    }

    fun schedule(routine: Routine) {
        if (!routine.enabled) {
            cancel(routine)
            return
        }
        routine.triggers.forEach { trigger ->
            when (trigger) {
                is Trigger.Time -> time.schedule(routine.id, trigger)
                is Trigger.Location -> location.schedule(routine.id, trigger)
                is Trigger.Charging -> Unit // handled by ChargingReceiver registered at runtime
            }
        }
    }

    fun cancel(routine: Routine) {
        routine.triggers.forEach { trigger ->
            when (trigger) {
                is Trigger.Time -> time.cancel(routine.id, trigger)
                is Trigger.Location -> location.cancel(routine.id, trigger)
                is Trigger.Charging -> Unit
            }
        }
    }

    fun reschedule(routine: Routine) {
        cancel(routine)
        if (routine.enabled) schedule(routine)
    }

    companion object {
        const val TAG = "Routine.Scheduler"
    }
}

internal fun pendingIntentFlags(): Int {
    return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
}
