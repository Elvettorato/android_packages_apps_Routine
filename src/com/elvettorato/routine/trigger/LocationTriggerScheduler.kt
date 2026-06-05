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

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.elvettorato.routine.data.RoutineRepository
import com.elvettorato.routine.data.Trigger
import com.elvettorato.routine.receiver.GeofenceReceiver
import com.elvettorato.routine.service.LocationPollWorker
import java.util.concurrent.TimeUnit

/**
 * Schedules geofence-style triggers using the AOSP system
 * [LocationManager.addProximityAlert] API (no Google Play Services dep), with a
 * periodic location-polling fallback via WorkManager for devices where
 * proximity alerts are unavailable or unreliable.
 */
internal class LocationTriggerScheduler(
    private val context: Context,
    private val repo: RoutineRepository,
) {

    private val lm: LocationManager? =
        context.getSystemService(LocationManager::class.java)

    fun schedule(routineId: String, trigger: Trigger.Location) {
        val mgr = lm ?: return
        if (!hasLocationPermission()) return
        val pi = pendingIntentFor(routineId, trigger)
        try {
            // expiration -1 means: never expire
            mgr.addProximityAlert(
                trigger.latitude,
                trigger.longitude,
                trigger.radiusMeters,
                /* expiration = */ -1L,
                pi
            )
            Log.i(TriggerScheduler.TAG, "Proximity alert for ${trigger.placeName}")
        } catch (sec: SecurityException) {
            Log.w(TriggerScheduler.TAG, "Cannot add proximity alert (no perm)", sec)
        }
    }

    fun cancel(routineId: String, trigger: Trigger.Location) {
        val mgr = lm ?: return
        val pi = pendingIntentFor(routineId, trigger)
        try {
            mgr.removeProximityAlert(pi)
        } catch (sec: SecurityException) {
            Log.w(TriggerScheduler.TAG, "Cannot remove proximity alert", sec)
        }
        pi.cancel()
    }

    fun startPollingFallbackIfNeeded() {
        val locationRoutines = repo.enabledRoutines()
            .flatMap { it.triggers }
            .filterIsInstance<Trigger.Location>()
        if (locationRoutines.isEmpty()) {
            stopPolling()
            return
        }
        val intervalMin = repo.locationPollMinutes().toLong()
        val req = PeriodicWorkRequestBuilder<LocationPollWorker>(
            intervalMin, TimeUnit.MINUTES,
            // Use the flex period to allow batching
            (intervalMin / 3).coerceAtLeast(1), TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req
        )
    }

    fun stopPolling() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun pendingIntentFor(routineId: String, trigger: Trigger.Location): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java).apply {
            action = GeofenceReceiver.ACTION_PROXIMITY
            putExtra(GeofenceReceiver.EXTRA_ROUTINE_ID, routineId)
            putExtra(GeofenceReceiver.EXTRA_TRIGGER_ID, trigger.id)
            data = android.net.Uri.parse("routine://geo/$routineId/${trigger.id}")
        }
        val rc = (routineId + trigger.id + "geo").hashCode()
        return PendingIntent.getBroadcast(context, rc, intent, pendingIntentFlags())
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val WORK_NAME = "routine_location_poll"
        const val WORK_TAG = "routine_location"
    }
}
