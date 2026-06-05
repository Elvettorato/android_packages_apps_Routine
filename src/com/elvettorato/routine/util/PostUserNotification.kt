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

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.elvettorato.routine.R
import com.elvettorato.routine.RoutineApp
import com.elvettorato.routine.data.RoutineAction
import com.elvettorato.routine.ui.MainActivity

/** Posts a user-defined notification action to the [RoutineApp.CHANNEL_USER] channel. */
object PostUserNotification {

    private var nextId = 1_000

    fun post(context: Context, a: RoutineAction.Notification) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val tap = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, RoutineApp.CHANNEL_USER)
            .setContentTitle(a.title.ifBlank { context.getString(R.string.app_name) })
            .setContentText(a.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(a.body))
            .setSmallIcon(R.drawable.ic_action_notification)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        nm.notify(nextId++, n)
    }
}
