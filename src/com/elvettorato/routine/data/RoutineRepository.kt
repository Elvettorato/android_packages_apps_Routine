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
package com.elvettorato.routine.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists all [Routine]s in a single SharedPreferences-backed JSON blob.
 *
 * We expose both a Kotlin [StateFlow] (for the UI) and a [MutableLiveData] mirror
 * (so older Java-style observers in injected SettingsLib widgets keep working).
 */
class RoutineRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _flow = MutableStateFlow(loadAll())
    val routines: StateFlow<List<Routine>> = _flow

    val liveRoutines: MutableLiveData<List<Routine>> = MutableLiveData(_flow.value)

    fun all(): List<Routine> = _flow.value

    fun byId(id: String): Routine? = _flow.value.firstOrNull { it.id == id }

    fun save(routine: Routine) {
        val newList = _flow.value.toMutableList().apply {
            val idx = indexOfFirst { it.id == routine.id }
            if (idx >= 0) set(idx, routine) else add(routine)
        }
        commit(newList)
    }

    fun delete(id: String) {
        commit(_flow.value.filterNot { it.id == id })
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val r = byId(id) ?: return
        save(r.copy(enabled = enabled))
    }

    fun touchLastRun(id: String, millis: Long) {
        val r = byId(id) ?: return
        save(r.copy(lastRunMillis = millis))
    }

    fun hasAnyEnabledRoutine(): Boolean = _flow.value.any { it.enabled }

    fun enabledRoutines(): List<Routine> = _flow.value.filter { it.enabled }

    // ─── options (SharedPreferences-level flags shared across the app) ─────

    fun setShowInLauncher(value: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_LAUNCHER, value).apply()
    }

    fun showInLauncher(): Boolean = prefs.getBoolean(KEY_SHOW_LAUNCHER, false)

    fun setPersistentNotification(value: Boolean) {
        prefs.edit().putBoolean(KEY_PERSISTENT_NOTIF, value).apply()
    }

    fun persistentNotification(): Boolean = prefs.getBoolean(KEY_PERSISTENT_NOTIF, true)

    fun setLocationPollMinutes(value: Int) {
        prefs.edit().putInt(KEY_LOCATION_POLL_MIN, value.coerceIn(5, 60)).apply()
    }

    fun locationPollMinutes(): Int = prefs.getInt(KEY_LOCATION_POLL_MIN, 15)

    // ─── internal ──────────────────────────────────────────────────────────

    private fun loadAll(): List<Routine> =
        JsonCodec.decodeList(prefs.getString(KEY_ROUTINES, null))

    private fun commit(routines: List<Routine>) {
        val json = JsonCodec.encodeList(routines)
        prefs.edit().putString(KEY_ROUTINES, json).apply()
        _flow.value = routines
        liveRoutines.postValue(routines)
    }

    companion object {
        private const val PREFS_NAME = "routine_prefs"
        private const val KEY_ROUTINES = "routines"
        private const val KEY_SHOW_LAUNCHER = "show_in_launcher"
        private const val KEY_PERSISTENT_NOTIF = "persistent_notification"
        private const val KEY_LOCATION_POLL_MIN = "location_poll_min"
    }
}
