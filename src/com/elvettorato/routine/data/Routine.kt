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

import java.util.UUID

/**
 * A user-defined automation rule. When any of its [triggers] fires, every action
 * in [actions] is applied in order.
 *
 * Storage format is JSON via [JsonCodec]; we deliberately keep the schema simple
 * to make data forward/backward compatible across app updates.
 */
data class Routine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enabled: Boolean = true,
    val triggers: List<Trigger> = emptyList(),
    val actions: List<RoutineAction> = emptyList(),
    val requireUnlocked: Boolean = false,
    val iconKey: String = ICON_DEFAULT,
    val lastRunMillis: Long = 0L,
) {
    companion object {
        const val ICON_DEFAULT = "time"
    }
}

/** Sealed hierarchy of triggers. */
sealed class Trigger {
    abstract val id: String

    /** Time-of-day trigger. [daysOfWeek] is a bitmask where bit 0 = Monday. */
    data class Time(
        override val id: String = UUID.randomUUID().toString(),
        val hour: Int,
        val minute: Int,
        val daysOfWeek: Int = 0b1111111, // every day by default
    ) : Trigger()

    /** Geofence trigger. [enter] true → fires on enter, false → on exit. */
    data class Location(
        override val id: String = UUID.randomUUID().toString(),
        val placeName: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Float,
        val enter: Boolean,
    ) : Trigger()

    /** Charging trigger. [connected] true → fires when cable plugged. */
    data class Charging(
        override val id: String = UUID.randomUUID().toString(),
        val connected: Boolean,
    ) : Trigger()
}

/** Sealed hierarchy of actions. */
sealed class RoutineAction {
    abstract val id: String

    enum class Toggle { ON, OFF, TOGGLE }
    enum class DndMode { OFF, PRIORITY, ALARMS, TOTAL }
    enum class Ringer { NORMAL, VIBRATE, SILENT }
    enum class Stream { RING, MEDIA, ALARM, NOTIFICATION, CALL }

    data class Dnd(
        override val id: String = UUID.randomUUID().toString(),
        val mode: DndMode,
    ) : RoutineAction()

    data class Volume(
        override val id: String = UUID.randomUUID().toString(),
        val stream: Stream,
        /** 0..100 percent. */
        val level: Int,
    ) : RoutineAction()

    data class RingerMode(
        override val id: String = UUID.randomUUID().toString(),
        val ringer: Ringer,
    ) : RoutineAction()

    data class Brightness(
        override val id: String = UUID.randomUUID().toString(),
        /** -1 means "enable auto-brightness". Otherwise 0..100. */
        val level: Int,
        val auto: Boolean = false,
    ) : RoutineAction()

    data class Wifi(
        override val id: String = UUID.randomUUID().toString(),
        val toggle: Toggle,
    ) : RoutineAction()

    data class Bluetooth(
        override val id: String = UUID.randomUUID().toString(),
        val toggle: Toggle,
    ) : RoutineAction()

    data class Airplane(
        override val id: String = UUID.randomUUID().toString(),
        val toggle: Toggle,
    ) : RoutineAction()

    data class Notification(
        override val id: String = UUID.randomUUID().toString(),
        val title: String,
        val body: String,
    ) : RoutineAction()
}
