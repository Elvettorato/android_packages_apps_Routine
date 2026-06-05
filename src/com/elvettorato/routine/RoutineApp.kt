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
package com.elvettorato.routine

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.elvettorato.routine.data.RoutineRepository
import com.elvettorato.routine.service.RoutineService
import com.elvettorato.routine.trigger.TriggerScheduler

/**
 * Application singleton. Sets up notification channels and starts the foreground
 * service whenever the user has at least one enabled routine and has opted in
 * to the persistent notification (default true).
 */
class RoutineApp : Application() {

    val repository: RoutineRepository by lazy { RoutineRepository(this) }
    val triggerScheduler: TriggerScheduler by lazy { TriggerScheduler(this, repository) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        // Re-schedule on first launch in case the user upgraded the system app.
        triggerScheduler.scheduleAllEnabled()
        if (repository.hasAnyEnabledRoutine()) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, RoutineService::class.java)
            )
        }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val service = NotificationChannel(
            CHANNEL_SERVICE,
            getString(R.string.channel_service),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.channel_service_description)
            setShowBadge(false)
        }
        val user = NotificationChannel(
            CHANNEL_USER,
            getString(R.string.channel_user),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.channel_user_description)
        }
        nm.createNotificationChannels(listOf(service, user))
    }

    companion object {
        const val CHANNEL_SERVICE = "routine_service"
        const val CHANNEL_USER = "routine_user"

        lateinit var instance: RoutineApp
            private set
    }
}
