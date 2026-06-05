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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.util.Log
import com.elvettorato.routine.RoutineApp
import com.elvettorato.routine.action.ActionExecutor
import com.elvettorato.routine.data.Trigger

/**
 * Receives [LocationManager.KEY_PROXIMITY_ENTERING] broadcasts and applies the
 * routine when the user enters or exits the configured area.
 */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PROXIMITY) return
        val routineId = intent.getStringExtra(EXTRA_ROUTINE_ID) ?: return
        val triggerId = intent.getStringExtra(EXTRA_TRIGGER_ID) ?: return
        val entering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)
        Log.i(TAG, "Geofence $routineId/$triggerId entering=$entering")

        val app = context.applicationContext as RoutineApp
        val routine = app.repository.byId(routineId) ?: return
        if (!routine.enabled) return

        val trigger = routine.triggers.firstOrNull { it.id == triggerId } as? Trigger.Location
            ?: return
        if (trigger.enter != entering) return // wrong edge, ignore

        ActionExecutor(context).apply(routine)
        app.repository.touchLastRun(routineId, System.currentTimeMillis())
    }

    companion object {
        const val TAG = "Routine.Geo"
        const val ACTION_PROXIMITY = "com.elvettorato.routine.action.PROXIMITY"
        const val EXTRA_ROUTINE_ID = "routine_id"
        const val EXTRA_TRIGGER_ID = "trigger_id"
    }
}
