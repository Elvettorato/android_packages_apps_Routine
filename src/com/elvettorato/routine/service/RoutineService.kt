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
package com.elvettorato.routine.service

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.elvettorato.routine.R
import com.elvettorato.routine.RoutineApp
import com.elvettorato.routine.action.ActionExecutor
import com.elvettorato.routine.data.Trigger
import com.elvettorato.routine.ui.MainActivity

/**
 * Long-lived foreground service that:
 *  - keeps the app process alive so AlarmManager/LocationManager callbacks can
 *    cheaply re-enter the same JVM;
 *  - listens to charging changes (the Trigger.Charging trigger is intentionally
 *    handled here, not in the manifest, to avoid manifest-only receiver throttling).
 *
 * The persistent notification is opt-out via Options → "Show ongoing notification".
 */
class RoutineService : Service() {

    private lateinit var app: RoutineApp

    private val chargingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isConnected = when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> true
                Intent.ACTION_POWER_DISCONNECTED -> false
                else -> return
            }
            app.repository.enabledRoutines().forEach { r ->
                r.triggers.filterIsInstance<Trigger.Charging>()
                    .filter { it.connected == isConnected }
                    .forEach { _ ->
                        ActionExecutor(context).apply(r)
                        app.repository.touchLastRun(r.id, System.currentTimeMillis())
                    }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = applicationContext as RoutineApp
        startForeground(NOTIF_ID, buildNotification(app.repository.enabledRoutines().size))
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(chargingReceiver, filter, Context.RECEIVER_EXPORTED)
        Log.i(TAG, "RoutineService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Refresh the ongoing notification each time we are explicitly poked
        startForeground(NOTIF_ID, buildNotification(app.repository.enabledRoutines().size))
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(chargingReceiver) }
        Log.i(TAG, "RoutineService destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(enabledCount: Int): android.app.Notification {
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, RoutineApp.CHANNEL_SERVICE)
            .setContentTitle(getString(R.string.service_running_title))
            .setContentText(getString(R.string.service_running_summary, enabledCount))
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(tap)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        return builder.build()
    }

    companion object {
        const val TAG = "Routine.Service"
        const val NOTIF_ID = 1
    }
}
