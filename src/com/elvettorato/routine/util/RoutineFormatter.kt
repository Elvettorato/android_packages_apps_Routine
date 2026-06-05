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
package com.elvettorato.routine.util

import android.content.Context
import android.text.format.DateUtils
import com.elvettorato.routine.R
import com.elvettorato.routine.data.Routine
import com.elvettorato.routine.data.RoutineAction
import com.elvettorato.routine.data.Trigger
import java.text.DateFormat
import java.util.Date

/** Pretty-print helpers shared by the list and editor. */
object RoutineFormatter {

    fun summary(context: Context, routine: Routine): String {
        val parts = mutableListOf<String>()
        routine.triggers.firstOrNull()?.let { parts += triggerSummary(context, it) }
        if (routine.triggers.size > 1) {
            parts += "+${routine.triggers.size - 1}"
        }
        routine.actions.firstOrNull()?.let { parts += actionSummary(context, it) }
        if (routine.actions.size > 1) {
            parts += "+${routine.actions.size - 1}"
        }
        return parts.joinToString(" • ")
    }

    fun triggerSummary(context: Context, trigger: Trigger): String = when (trigger) {
        is Trigger.Time -> {
            val time = "%02d:%02d".format(trigger.hour, trigger.minute)
            val days = daysSummary(context, trigger.daysOfWeek)
            context.getString(R.string.trigger_time_at, time) + " " +
                context.getString(R.string.trigger_time_days, days)
        }
        is Trigger.Location -> {
            val verb = if (trigger.enter)
                context.getString(R.string.trigger_location_enter)
            else context.getString(R.string.trigger_location_exit)
            "$verb: ${trigger.placeName}"
        }
        is Trigger.Charging -> if (trigger.connected) {
            context.getString(R.string.trigger_charging) + " (${context.getString(R.string.state_on)})"
        } else {
            context.getString(R.string.trigger_charging) + " (${context.getString(R.string.state_off)})"
        }
    }

    fun actionSummary(context: Context, action: RoutineAction): String = when (action) {
        is RoutineAction.Dnd -> context.getString(R.string.action_dnd) + ": " + dndName(context, action.mode)
        is RoutineAction.Volume -> context.getString(R.string.action_volume) + " " +
            streamName(context, action.stream) + " " +
            context.getString(R.string.volume_level, action.level)
        is RoutineAction.RingerMode -> context.getString(R.string.action_ringer_mode) + ": " +
            ringerName(context, action.ringer)
        is RoutineAction.Brightness -> if (action.auto)
            context.getString(R.string.brightness_auto)
        else context.getString(R.string.action_brightness) + " " +
            context.getString(R.string.brightness_level, action.level)
        is RoutineAction.Wifi -> context.getString(R.string.action_wifi) + ": " + toggleName(context, action.toggle)
        is RoutineAction.Bluetooth -> context.getString(R.string.action_bluetooth) + ": " + toggleName(context, action.toggle)
        is RoutineAction.Airplane -> context.getString(R.string.action_airplane) + ": " + toggleName(context, action.toggle)
        is RoutineAction.Notification -> context.getString(R.string.action_notification) + ": " + action.title
    }

    fun lastRun(context: Context, routine: Routine): String {
        if (routine.lastRunMillis <= 0L) return context.getString(R.string.never_run)
        val formatted = DateUtils.getRelativeTimeSpanString(
            routine.lastRunMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
        return context.getString(R.string.last_run, formatted)
    }

    private fun daysSummary(context: Context, mask: Int): String {
        val all = 0b1111111
        val weekdays = 0b0011111 // Mon..Fri
        val weekends = 0b1100000 // Sat..Sun
        return when (mask) {
            all -> context.getString(R.string.trigger_days_all)
            weekdays -> context.getString(R.string.trigger_days_weekdays)
            weekends -> context.getString(R.string.trigger_days_weekends)
            else -> {
                val names = listOf(
                    R.string.day_mon, R.string.day_tue, R.string.day_wed, R.string.day_thu,
                    R.string.day_fri, R.string.day_sat, R.string.day_sun
                )
                buildList {
                    for (i in 0..6) if ((mask and (1 shl i)) != 0) add(context.getString(names[i]))
                }.joinToString(" ")
            }
        }
    }

    private fun dndName(context: Context, mode: RoutineAction.DndMode): String =
        context.getString(when (mode) {
            RoutineAction.DndMode.OFF -> R.string.dnd_off
            RoutineAction.DndMode.PRIORITY -> R.string.dnd_priority
            RoutineAction.DndMode.ALARMS -> R.string.dnd_alarms
            RoutineAction.DndMode.TOTAL -> R.string.dnd_total
        })

    private fun streamName(context: Context, stream: RoutineAction.Stream): String =
        context.getString(when (stream) {
            RoutineAction.Stream.RING -> R.string.stream_ring
            RoutineAction.Stream.MEDIA -> R.string.stream_media
            RoutineAction.Stream.ALARM -> R.string.stream_alarm
            RoutineAction.Stream.NOTIFICATION -> R.string.stream_notification
            RoutineAction.Stream.CALL -> R.string.stream_call
        })

    private fun ringerName(context: Context, r: RoutineAction.Ringer): String =
        context.getString(when (r) {
            RoutineAction.Ringer.NORMAL -> R.string.ringer_normal
            RoutineAction.Ringer.VIBRATE -> R.string.ringer_vibrate
            RoutineAction.Ringer.SILENT -> R.string.ringer_silent
        })

    private fun toggleName(context: Context, t: RoutineAction.Toggle): String =
        context.getString(when (t) {
            RoutineAction.Toggle.ON -> R.string.state_on
            RoutineAction.Toggle.OFF -> R.string.state_off
            RoutineAction.Toggle.TOGGLE -> R.string.state_toggle
        })
}
