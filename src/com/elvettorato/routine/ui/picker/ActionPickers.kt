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
package com.elvettorato.routine.ui.picker

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.elvettorato.routine.R
import com.elvettorato.routine.data.RoutineAction
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Material 3 pickers for [RoutineAction]s. */
object ActionPickers {

    fun pickType(activity: AppCompatActivity, onPicked: (RoutineAction) -> Unit) {
        val labels = arrayOf(
            activity.getString(R.string.action_dnd),
            activity.getString(R.string.action_ringer_mode),
            activity.getString(R.string.action_volume),
            activity.getString(R.string.action_brightness),
            activity.getString(R.string.action_wifi),
            activity.getString(R.string.action_bluetooth),
            activity.getString(R.string.action_airplane),
            activity.getString(R.string.action_notification),
        )
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.action_add)
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> pickDnd(activity, null, onPicked)
                    1 -> pickRinger(activity, null, onPicked)
                    2 -> pickVolume(activity, null, onPicked)
                    3 -> pickBrightness(activity, null, onPicked)
                    4 -> pickToggle(activity, R.string.action_wifi, null) { t ->
                        onPicked(RoutineAction.Wifi(toggle = t))
                    }
                    5 -> pickToggle(activity, R.string.action_bluetooth, null) { t ->
                        onPicked(RoutineAction.Bluetooth(toggle = t))
                    }
                    6 -> pickToggle(activity, R.string.action_airplane, null) { t ->
                        onPicked(RoutineAction.Airplane(toggle = t))
                    }
                    7 -> pickNotification(activity, null, onPicked)
                }
            }
            .show()
    }

    fun edit(activity: AppCompatActivity, action: RoutineAction, onPicked: (RoutineAction) -> Unit) {
        when (action) {
            is RoutineAction.Dnd -> pickDnd(activity, action, onPicked)
            is RoutineAction.RingerMode -> pickRinger(activity, action, onPicked)
            is RoutineAction.Volume -> pickVolume(activity, action, onPicked)
            is RoutineAction.Brightness -> pickBrightness(activity, action, onPicked)
            is RoutineAction.Wifi -> pickToggle(activity, R.string.action_wifi, action.toggle) {
                onPicked(action.copy(toggle = it))
            }
            is RoutineAction.Bluetooth -> pickToggle(activity, R.string.action_bluetooth, action.toggle) {
                onPicked(action.copy(toggle = it))
            }
            is RoutineAction.Airplane -> pickToggle(activity, R.string.action_airplane, action.toggle) {
                onPicked(action.copy(toggle = it))
            }
            is RoutineAction.Notification -> pickNotification(activity, action, onPicked)
        }
    }

    private fun pickDnd(
        context: Context,
        existing: RoutineAction.Dnd?,
        onPicked: (RoutineAction) -> Unit,
    ) {
        val labels = arrayOf(
            context.getString(R.string.dnd_off),
            context.getString(R.string.dnd_priority),
            context.getString(R.string.dnd_alarms),
            context.getString(R.string.dnd_total),
        )
        val selected = existing?.mode?.ordinal ?: 1
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.action_dnd)
            .setSingleChoiceItems(labels, selected) { dlg, which ->
                onPicked(existing?.copy(mode = RoutineAction.DndMode.entries[which])
                    ?: RoutineAction.Dnd(mode = RoutineAction.DndMode.entries[which]))
                dlg.dismiss()
            }
            .show()
    }

    private fun pickRinger(
        context: Context,
        existing: RoutineAction.RingerMode?,
        onPicked: (RoutineAction) -> Unit,
    ) {
        val labels = arrayOf(
            context.getString(R.string.ringer_normal),
            context.getString(R.string.ringer_vibrate),
            context.getString(R.string.ringer_silent),
        )
        val selected = existing?.ringer?.ordinal ?: 0
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.action_ringer_mode)
            .setSingleChoiceItems(labels, selected) { dlg, which ->
                onPicked(existing?.copy(ringer = RoutineAction.Ringer.entries[which])
                    ?: RoutineAction.RingerMode(ringer = RoutineAction.Ringer.entries[which]))
                dlg.dismiss()
            }
            .show()
    }

    private fun pickVolume(
        context: Context,
        existing: RoutineAction.Volume?,
        onPicked: (RoutineAction) -> Unit,
    ) {
        val streams = arrayOf(
            context.getString(R.string.stream_ring),
            context.getString(R.string.stream_media),
            context.getString(R.string.stream_alarm),
            context.getString(R.string.stream_notification),
            context.getString(R.string.stream_call),
        )
        val selectedStream = existing?.stream?.ordinal ?: 1
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.volume_stream)
            .setSingleChoiceItems(streams, selectedStream) { dlg, which ->
                dlg.dismiss()
                pickPercentage(context, R.string.action_volume, existing?.level ?: 50) { pct ->
                    onPicked(existing?.copy(stream = RoutineAction.Stream.entries[which], level = pct)
                        ?: RoutineAction.Volume(
                            stream = RoutineAction.Stream.entries[which], level = pct
                        ))
                }
            }
            .show()
    }

    private fun pickBrightness(
        context: Context,
        existing: RoutineAction.Brightness?,
        onPicked: (RoutineAction) -> Unit,
    ) {
        val labels = arrayOf(
            context.getString(R.string.brightness_auto),
            context.getString(R.string.action_brightness),
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.action_brightness)
            .setItems(labels) { _, which ->
                if (which == 0) {
                    onPicked(existing?.copy(auto = true, level = -1)
                        ?: RoutineAction.Brightness(auto = true, level = -1))
                } else {
                    pickPercentage(context, R.string.action_brightness, existing?.level?.takeIf { it >= 0 } ?: 50) { pct ->
                        onPicked(existing?.copy(auto = false, level = pct)
                            ?: RoutineAction.Brightness(auto = false, level = pct))
                    }
                }
            }
            .show()
    }

    private fun pickToggle(
        context: Context,
        titleRes: Int,
        existing: RoutineAction.Toggle?,
        onPicked: (RoutineAction.Toggle) -> Unit,
    ) {
        val labels = arrayOf(
            context.getString(R.string.state_on),
            context.getString(R.string.state_off),
            context.getString(R.string.state_toggle),
        )
        val selected = existing?.ordinal ?: 0
        MaterialAlertDialogBuilder(context)
            .setTitle(titleRes)
            .setSingleChoiceItems(labels, selected) { dlg, which ->
                onPicked(RoutineAction.Toggle.entries[which])
                dlg.dismiss()
            }
            .show()
    }

    private fun pickNotification(
        context: Context,
        existing: RoutineAction.Notification?,
        onPicked: (RoutineAction) -> Unit,
    ) {
        val parent = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val padding = (context.resources.displayMetrics.density * 16).toInt()
            setPadding(padding, padding, padding, padding)
        }
        val titleInput = android.widget.EditText(context).apply {
            hint = context.getString(R.string.notification_title_hint)
            setSingleLine()
            setText(existing?.title.orEmpty())
        }
        val bodyInput = android.widget.EditText(context).apply {
            hint = context.getString(R.string.notification_body_hint)
            setText(existing?.body.orEmpty())
        }
        parent.addView(titleInput)
        parent.addView(bodyInput)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.action_notification)
            .setView(parent)
            .setPositiveButton(R.string.ok) { _, _ ->
                onPicked(existing?.copy(
                    title = titleInput.text.toString(),
                    body = bodyInput.text.toString(),
                ) ?: RoutineAction.Notification(
                    title = titleInput.text.toString(),
                    body = bodyInput.text.toString(),
                ))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun pickPercentage(
        context: Context,
        titleRes: Int,
        initial: Int,
        onPicked: (Int) -> Unit,
    ) {
        val slider = com.google.android.material.slider.Slider(context).apply {
            valueFrom = 0f
            valueTo = 100f
            stepSize = 5f
            value = initial.coerceIn(0, 100).toFloat()
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(titleRes)
            .setView(slider)
            .setPositiveButton(R.string.ok) { _, _ -> onPicked(slider.value.toInt()) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
