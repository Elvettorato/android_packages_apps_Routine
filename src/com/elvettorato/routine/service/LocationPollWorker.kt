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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.elvettorato.routine.RoutineApp
import com.elvettorato.routine.action.ActionExecutor
import com.elvettorato.routine.data.Trigger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Periodic fallback location polling used when system proximity alerts are
 * unavailable (e.g. battery saver throttled them). Runs every "Location check
 * interval" minutes from [com.elvettorato.routine.data.RoutineRepository].
 */
class LocationPollWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as RoutineApp
        val locationRoutines = app.repository.enabledRoutines()
            .flatMap { r -> r.triggers.filterIsInstance<Trigger.Location>().map { r to it } }
        if (locationRoutines.isEmpty()) return Result.success()

        if (!hasFineLocation()) {
            Log.w(TAG, "No fine location permission; skipping poll")
            return Result.success()
        }

        val loc = currentLocation() ?: return Result.success()
        Log.i(TAG, "Poll fix: ${loc.latitude},${loc.longitude}")
        val lastFixKey = "last_inside_${loc.provider}"
        val prefs = applicationContext.getSharedPreferences("routine_state", Context.MODE_PRIVATE)

        locationRoutines.forEach { (routine, trigger) ->
            val distance = haversineMeters(
                loc.latitude, loc.longitude, trigger.latitude, trigger.longitude
            )
            val inside = distance <= trigger.radiusMeters
            val key = "${routine.id}_${trigger.id}_inside"
            val wasInside = prefs.getBoolean(key, false)
            if (inside != wasInside) {
                prefs.edit().putBoolean(key, inside).apply()
                val matchesEdge = (trigger.enter && inside) || (!trigger.enter && !inside)
                if (matchesEdge) {
                    Log.i(TAG, "Poll edge fired for ${routine.name}")
                    ActionExecutor(applicationContext).apply(routine)
                    app.repository.touchLastRun(routine.id, System.currentTimeMillis())
                }
            }
        }
        return Result.success()
    }

    private fun hasFineLocation(): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Suppress("MissingPermission")
    private suspend fun currentLocation(): Location? {
        val lm = applicationContext.getSystemService(LocationManager::class.java) ?: return null
        val providers = lm.getProviders(true).ifEmpty { return null }
        // Prefer fused if exposed, fall back to network then GPS
        val provider = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER
        ).firstOrNull { providers.contains(it) } ?: return null

        return suspendCancellableCoroutine { cont ->
            try {
                lm.getCurrentLocation(
                    provider,
                    /* cancellationSignal = */ null,
                    applicationContext.mainExecutor,
                ) { fix -> cont.resume(fix) }
            } catch (sec: SecurityException) {
                cont.resume(null)
            } catch (t: Throwable) {
                Log.w(TAG, "Location request failed", t)
                cont.resume(null)
            }
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    companion object { const val TAG = "Routine.LocPoll" }
}
