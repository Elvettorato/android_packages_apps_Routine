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
package com.elvettorato.routine.action

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import com.elvettorato.routine.RoutineApp
import com.elvettorato.routine.data.Routine
import com.elvettorato.routine.data.RoutineAction
import com.elvettorato.routine.util.PostUserNotification

/**
 * Applies every [RoutineAction] inside a [Routine] in order, catching per-action
 * failures so that one broken action does not stop the rest.
 */
class ActionExecutor(private val context: Context) {

    fun apply(routine: Routine) {
        Log.i(TAG, "Applying routine ${routine.name} (${routine.actions.size} actions)")
        routine.actions.forEach { action ->
            try {
                applyOne(action)
            } catch (t: Throwable) {
                Log.w(TAG, "Action ${action::class.simpleName} failed", t)
            }
        }
    }

    private fun applyOne(action: RoutineAction) {
        when (action) {
            is RoutineAction.Dnd -> applyDnd(action)
            is RoutineAction.Volume -> applyVolume(action)
            is RoutineAction.RingerMode -> applyRinger(action)
            is RoutineAction.Brightness -> applyBrightness(action)
            is RoutineAction.Wifi -> applyWifi(action)
            is RoutineAction.Bluetooth -> applyBluetooth(action)
            is RoutineAction.Airplane -> applyAirplane(action)
            is RoutineAction.Notification -> PostUserNotification.post(context, action)
        }
    }

    // ─── individual actions ────────────────────────────────────────────────

    private fun applyDnd(a: RoutineAction.Dnd) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (!nm.isNotificationPolicyAccessGranted) {
            Log.w(TAG, "DND policy access not granted; skipping")
            return
        }
        nm.setInterruptionFilter(when (a.mode) {
            RoutineAction.DndMode.OFF -> NotificationManager.INTERRUPTION_FILTER_ALL
            RoutineAction.DndMode.PRIORITY -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
            RoutineAction.DndMode.ALARMS -> NotificationManager.INTERRUPTION_FILTER_ALARMS
            RoutineAction.DndMode.TOTAL -> NotificationManager.INTERRUPTION_FILTER_NONE
        })
    }

    private fun applyVolume(a: RoutineAction.Volume) {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        val streamId = when (a.stream) {
            RoutineAction.Stream.RING -> AudioManager.STREAM_RING
            RoutineAction.Stream.MEDIA -> AudioManager.STREAM_MUSIC
            RoutineAction.Stream.ALARM -> AudioManager.STREAM_ALARM
            RoutineAction.Stream.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
            RoutineAction.Stream.CALL -> AudioManager.STREAM_VOICE_CALL
        }
        val max = am.getStreamMaxVolume(streamId)
        val target = (a.level.coerceIn(0, 100) * max / 100)
        am.setStreamVolume(streamId, target, 0)
    }

    private fun applyRinger(a: RoutineAction.RingerMode) {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        am.ringerMode = when (a.ringer) {
            RoutineAction.Ringer.NORMAL -> AudioManager.RINGER_MODE_NORMAL
            RoutineAction.Ringer.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
            RoutineAction.Ringer.SILENT -> AudioManager.RINGER_MODE_SILENT
        }
    }

    private fun applyBrightness(a: RoutineAction.Brightness) {
        val resolver = context.contentResolver
        if (a.auto) {
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            )
            return
        }
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        val value = (a.level.coerceIn(0, 100) * 255 / 100)
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, value)
    }

    @Suppress("DEPRECATION")
    private fun applyWifi(a: RoutineAction.Wifi) {
        val wm = context.getSystemService(WifiManager::class.java) ?: return
        val desired = when (a.toggle) {
            RoutineAction.Toggle.ON -> true
            RoutineAction.Toggle.OFF -> false
            RoutineAction.Toggle.TOGGLE -> !wm.isWifiEnabled
        }
        // setWifiEnabled requires the privileged NETWORK_SETTINGS permission on
        // recent Android versions, which we hold as a system priv-app.
        wm.setWifiEnabled(desired)
    }

    private fun applyBluetooth(a: RoutineAction.Bluetooth) {
        val bm = context.getSystemService(BluetoothManager::class.java) ?: return
        val adapter: BluetoothAdapter = bm.adapter ?: return
        val isOn = adapter.isEnabled
        val desired = when (a.toggle) {
            RoutineAction.Toggle.ON -> true
            RoutineAction.Toggle.OFF -> false
            RoutineAction.Toggle.TOGGLE -> !isOn
        }
        if (desired == isOn) return
        try {
            // BluetoothAdapter.enable()/disable() are deprecated for normal apps
            // but still functional for priv-apps that hold BLUETOOTH_PRIVILEGED.
            @Suppress("DEPRECATION")
            if (desired) adapter.enable() else adapter.disable()
        } catch (t: Throwable) {
            Log.w(TAG, "Bluetooth toggle failed", t)
        }
    }

    private fun applyAirplane(a: RoutineAction.Airplane) {
        val isOn = Settings.Global.getInt(
            context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0
        ) == 1
        val desired = when (a.toggle) {
            RoutineAction.Toggle.ON -> true
            RoutineAction.Toggle.OFF -> false
            RoutineAction.Toggle.TOGGLE -> !isOn
        }
        if (desired == isOn) return
        Settings.Global.putInt(
            context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, if (desired) 1 else 0
        )
        // Notify listeners (system radios) about the change
        val intent = android.content.Intent(android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED)
            .putExtra("state", desired)
        context.sendBroadcastAsUser(intent, android.os.UserHandle.ALL)
    }

    companion object {
        const val TAG = "Routine.Actions"
    }
}
