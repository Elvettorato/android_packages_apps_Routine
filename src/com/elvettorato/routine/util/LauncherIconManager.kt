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

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Enables/disables the activity-alias that exposes the MAIN/LAUNCHER icon.
 *
 * The alias is declared `enabled="false"` in the manifest so the icon is hidden
 * by default; flipping it via [PackageManager.setComponentEnabledSetting] makes
 * it visible in the launcher's app drawer without requiring a reinstall.
 */
object LauncherIconManager {

    private const val ALIAS_CLASS = "com.elvettorato.routine.ui.MainActivityLauncher"

    fun setVisible(context: Context, visible: Boolean) {
        val pm = context.packageManager
        val component = ComponentName(context.packageName, ALIAS_CLASS)
        val newState = if (visible) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        pm.setComponentEnabledSetting(component, newState, PackageManager.DONT_KILL_APP)
    }

    fun isVisible(context: Context): Boolean {
        val pm = context.packageManager
        val component = ComponentName(context.packageName, ALIAS_CLASS)
        return when (pm.getComponentEnabledSetting(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> false
            else -> false
        }
    }
}
