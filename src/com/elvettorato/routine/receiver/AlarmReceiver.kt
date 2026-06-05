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
package com.elvettorato.routine.receiver

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.elvettorato.routine.RoutineApp
import com.elvettorato.routine.action.ActionExecutor
import com.elvettorato.routine.data.Trigger

/** Invoked by AlarmManager when a time trigger fires. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val routineId = intent.getStringExtra(EXTRA_ROUTINE_ID) ?: return
        val triggerId = intent.getStringExtra(EXTRA_TRIGGER_ID) ?: return
        Log.i(TAG, "Alarm fired: $routineId/$triggerId")

        val app = context.applicationContext as RoutineApp
        val routine = app.repository.byId(routineId) ?: return
        if (!routine.enabled) return

        if (routine.requireUnlocked) {
            val km = context.getSystemService(KeyguardManager::class.java)
            if (km?.isKeyguardLocked == true) {
                Log.i(TAG, "Routine ${routine.name} skipped: device locked")
                rescheduleOnly(app, routineId, triggerId)
                return
            }
        }

        ActionExecutor(context).apply(routine)
        app.repository.touchLastRun(routineId, System.currentTimeMillis())
        rescheduleOnly(app, routineId, triggerId)
    }

    private fun rescheduleOnly(app: RoutineApp, routineId: String, triggerId: String) {
        val r = app.repository.byId(routineId) ?: return
        val t = r.triggers.firstOrNull { it.id == triggerId } as? Trigger.Time ?: return
        app.triggerScheduler.schedule(r)
    }

    companion object {
        const val TAG = "Routine.Alarm"
        const val ACTION_FIRE = "com.elvettorato.routine.action.ALARM_FIRE"
        const val EXTRA_ROUTINE_ID = "routine_id"
        const val EXTRA_TRIGGER_ID = "trigger_id"
    }
}
