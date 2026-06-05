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

import org.json.JSONArray
import org.json.JSONObject

/**
 * Tiny JSON codec for [Routine] objects. We use [org.json] (no Gson dependency)
 * to keep this system app prebuilt-friendly and the storage human-readable so
 * `adb shell run-as` can inspect the prefs file during debugging.
 *
 * Schema version is stored as `"v"` at the top of each routine object so we can
 * add fields without breaking old data.
 */
internal object JsonCodec {

    private const val SCHEMA = 1

    fun encodeList(routines: List<Routine>): String {
        val arr = JSONArray()
        routines.forEach { arr.put(encode(it)) }
        return arr.toString()
    }

    fun decodeList(json: String?): List<Routine> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                runCatching { decode(arr.getJSONObject(i)) }.getOrNull()
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    // ─── encode ─────────────────────────────────────────────────────────────

    private fun encode(r: Routine): JSONObject = JSONObject().apply {
        put("v", SCHEMA)
        put("id", r.id)
        put("name", r.name)
        put("enabled", r.enabled)
        put("requireUnlocked", r.requireUnlocked)
        put("iconKey", r.iconKey)
        put("lastRunMillis", r.lastRunMillis)
        put("triggers", JSONArray().also { arr ->
            r.triggers.forEach { arr.put(encodeTrigger(it)) }
        })
        put("actions", JSONArray().also { arr ->
            r.actions.forEach { arr.put(encodeAction(it)) }
        })
    }

    private fun encodeTrigger(t: Trigger): JSONObject = JSONObject().apply {
        put("id", t.id)
        when (t) {
            is Trigger.Time -> {
                put("type", "time")
                put("hour", t.hour); put("minute", t.minute); put("days", t.daysOfWeek)
            }
            is Trigger.Location -> {
                put("type", "location")
                put("place", t.placeName); put("lat", t.latitude); put("lon", t.longitude)
                put("radius", t.radiusMeters.toDouble()); put("enter", t.enter)
            }
            is Trigger.Charging -> {
                put("type", "charging"); put("connected", t.connected)
            }
        }
    }

    private fun encodeAction(a: RoutineAction): JSONObject = JSONObject().apply {
        put("id", a.id)
        when (a) {
            is RoutineAction.Dnd -> {
                put("type", "dnd"); put("mode", a.mode.name)
            }
            is RoutineAction.Volume -> {
                put("type", "volume"); put("stream", a.stream.name); put("level", a.level)
            }
            is RoutineAction.RingerMode -> {
                put("type", "ringer"); put("ringer", a.ringer.name)
            }
            is RoutineAction.Brightness -> {
                put("type", "brightness"); put("level", a.level); put("auto", a.auto)
            }
            is RoutineAction.Wifi -> {
                put("type", "wifi"); put("toggle", a.toggle.name)
            }
            is RoutineAction.Bluetooth -> {
                put("type", "bluetooth"); put("toggle", a.toggle.name)
            }
            is RoutineAction.Airplane -> {
                put("type", "airplane"); put("toggle", a.toggle.name)
            }
            is RoutineAction.Notification -> {
                put("type", "notif"); put("title", a.title); put("body", a.body)
            }
        }
    }

    // ─── decode ─────────────────────────────────────────────────────────────

    private fun decode(o: JSONObject): Routine {
        return Routine(
            id = o.getString("id"),
            name = o.optString("name", ""),
            enabled = o.optBoolean("enabled", true),
            requireUnlocked = o.optBoolean("requireUnlocked", false),
            iconKey = o.optString("iconKey", Routine.ICON_DEFAULT),
            lastRunMillis = o.optLong("lastRunMillis", 0L),
            triggers = decodeArray(o.optJSONArray("triggers"), ::decodeTrigger),
            actions = decodeArray(o.optJSONArray("actions"), ::decodeAction),
        )
    }

    private inline fun <T> decodeArray(
        arr: JSONArray?,
        decoder: (JSONObject) -> T?,
    ): List<T> {
        if (arr == null) return emptyList()
        val out = ArrayList<T>(arr.length())
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { runCatching { decoder(it) }.getOrNull()?.let(out::add) }
        }
        return out
    }

    private fun decodeTrigger(o: JSONObject): Trigger? = when (o.optString("type")) {
        "time" -> Trigger.Time(
            id = o.getString("id"),
            hour = o.optInt("hour", 0),
            minute = o.optInt("minute", 0),
            daysOfWeek = o.optInt("days", 0b1111111),
        )
        "location" -> Trigger.Location(
            id = o.getString("id"),
            placeName = o.optString("place", ""),
            latitude = o.optDouble("lat", 0.0),
            longitude = o.optDouble("lon", 0.0),
            radiusMeters = o.optDouble("radius", 150.0).toFloat(),
            enter = o.optBoolean("enter", true),
        )
        "charging" -> Trigger.Charging(
            id = o.getString("id"),
            connected = o.optBoolean("connected", true),
        )
        else -> null
    }

    private fun decodeAction(o: JSONObject): RoutineAction? = when (o.optString("type")) {
        "dnd" -> RoutineAction.Dnd(
            id = o.getString("id"),
            mode = RoutineAction.DndMode.valueOf(
                o.optString("mode", RoutineAction.DndMode.OFF.name)
            ),
        )
        "volume" -> RoutineAction.Volume(
            id = o.getString("id"),
            stream = RoutineAction.Stream.valueOf(
                o.optString("stream", RoutineAction.Stream.MEDIA.name)
            ),
            level = o.optInt("level", 50),
        )
        "ringer" -> RoutineAction.RingerMode(
            id = o.getString("id"),
            ringer = RoutineAction.Ringer.valueOf(
                o.optString("ringer", RoutineAction.Ringer.NORMAL.name)
            ),
        )
        "brightness" -> RoutineAction.Brightness(
            id = o.getString("id"),
            level = o.optInt("level", 50),
            auto = o.optBoolean("auto", false),
        )
        "wifi" -> RoutineAction.Wifi(
            id = o.getString("id"),
            toggle = RoutineAction.Toggle.valueOf(
                o.optString("toggle", RoutineAction.Toggle.ON.name)
            ),
        )
        "bluetooth" -> RoutineAction.Bluetooth(
            id = o.getString("id"),
            toggle = RoutineAction.Toggle.valueOf(
                o.optString("toggle", RoutineAction.Toggle.ON.name)
            ),
        )
        "airplane" -> RoutineAction.Airplane(
            id = o.getString("id"),
            toggle = RoutineAction.Toggle.valueOf(
                o.optString("toggle", RoutineAction.Toggle.ON.name)
            ),
        )
        "notif" -> RoutineAction.Notification(
            id = o.getString("id"),
            title = o.optString("title", ""),
            body = o.optString("body", ""),
        )
        else -> null
    }
}
