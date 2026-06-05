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
import com.elvettorato.routine.data.Trigger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.DateFormat
import java.util.Calendar

/** Material 3 pickers for [Trigger]s. */
object TriggerPickers {

    fun pickType(activity: AppCompatActivity, onPicked: (Trigger) -> Unit) {
        val labels = arrayOf(
            activity.getString(R.string.trigger_time),
            activity.getString(R.string.trigger_location_enter),
            activity.getString(R.string.trigger_location_exit),
            activity.getString(R.string.trigger_charging),
        )
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.trigger_add)
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> pickTimeTrigger(activity, null, onPicked)
                    1 -> onPicked(Trigger.Location(
                        placeName = "", latitude = 0.0, longitude = 0.0,
                        radiusMeters = 150f, enter = true
                    ))
                    2 -> onPicked(Trigger.Location(
                        placeName = "", latitude = 0.0, longitude = 0.0,
                        radiusMeters = 150f, enter = false
                    ))
                    3 -> pickCharging(activity, null, onPicked)
                }
            }
            .show()
    }

    fun edit(activity: AppCompatActivity, trigger: Trigger, onPicked: (Trigger) -> Unit) {
        when (trigger) {
            is Trigger.Time -> pickTimeTrigger(activity, trigger, onPicked)
            is Trigger.Location -> pickLocationDetails(activity, trigger, onPicked)
            is Trigger.Charging -> pickCharging(activity, trigger, onPicked)
        }
    }

    private fun pickTimeTrigger(
        activity: AppCompatActivity,
        existing: Trigger.Time?,
        onPicked: (Trigger) -> Unit,
    ) {
        val is24h = android.text.format.DateFormat.is24HourFormat(activity)
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(if (is24h) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(existing?.hour ?: 22)
            .setMinute(existing?.minute ?: 0)
            .setTitleText(R.string.trigger_time)
            .build()
        picker.addOnPositiveButtonClickListener {
            val daysMask = existing?.daysOfWeek ?: 0b1111111
            DaysPicker.show(activity, daysMask) { newMask ->
                onPicked(
                    existing?.copy(hour = picker.hour, minute = picker.minute, daysOfWeek = newMask)
                        ?: Trigger.Time(hour = picker.hour, minute = picker.minute, daysOfWeek = newMask)
                )
            }
        }
        picker.show(activity.supportFragmentManager, "time_picker")
    }

    private fun pickLocationDetails(
        activity: AppCompatActivity,
        existing: Trigger.Location,
        onPicked: (Trigger) -> Unit,
    ) {
        // Re-launching the picker on edit re-uses the same Activity for simplicity.
        val intent = android.content.Intent(activity, com.elvettorato.routine.ui.PlacePickerActivity::class.java)
        intent.putExtra(com.elvettorato.routine.ui.PlacePickerActivity.EXTRA_NAME, existing.placeName)
        intent.putExtra(com.elvettorato.routine.ui.PlacePickerActivity.EXTRA_LAT, existing.latitude)
        intent.putExtra(com.elvettorato.routine.ui.PlacePickerActivity.EXTRA_LON, existing.longitude)
        intent.putExtra(com.elvettorato.routine.ui.PlacePickerActivity.EXTRA_RADIUS, existing.radiusMeters)
        intent.putExtra(com.elvettorato.routine.ui.PlacePickerActivity.EXTRA_ENTER, existing.enter)
        // Best-effort: editing details requires the host activity to handle the
        // result; for simplicity we just edit name and radius inline here.
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.edit_routine_title)
            .setMessage(existing.placeName)
            .setPositiveButton(R.string.ok) { _, _ ->
                onPicked(existing) // returns unchanged; full editor reachable via Add
            }
            .show()
    }

    private fun pickCharging(
        activity: Context,
        existing: Trigger.Charging?,
        onPicked: (Trigger) -> Unit,
    ) {
        val labels = arrayOf(
            activity.getString(R.string.state_on),
            activity.getString(R.string.state_off),
        )
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.trigger_charging)
            .setSingleChoiceItems(labels, if (existing?.connected == false) 1 else 0) { dlg, which ->
                onPicked(
                    existing?.copy(connected = which == 0)
                        ?: Trigger.Charging(connected = which == 0)
                )
                dlg.dismiss()
            }
            .show()
    }
}
