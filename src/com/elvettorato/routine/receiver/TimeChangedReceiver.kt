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
import android.util.Log
import com.elvettorato.routine.RoutineApp

/** Re-evaluate scheduled time triggers when the wall clock or locale changes. */
class TimeChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Time/locale changed: ${intent.action}")
        val app = context.applicationContext as RoutineApp
        app.triggerScheduler.cancelAll()
        app.triggerScheduler.scheduleAllEnabled()
    }

    companion object { private const val TAG = "Routine.TimeChanged" }
}
