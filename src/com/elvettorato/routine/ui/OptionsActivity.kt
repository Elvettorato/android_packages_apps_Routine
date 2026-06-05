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
package com.elvettorato.routine.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.elvettorato.routine.R
import com.elvettorato.routine.RoutineApp
import com.elvettorato.routine.util.LauncherIconManager
import com.google.android.material.appbar.MaterialToolbar

/** App-wide options screen, accessible from the app's overflow menu. */
class OptionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.options_root, OptionsFragment())
                .commit()
        }
    }
}

class OptionsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.options_preferences, rootKey)
        val app = requireContext().applicationContext as RoutineApp
        val repo = app.repository

        findPreference<SwitchPreferenceCompat>("pref_show_in_launcher")?.apply {
            isChecked = repo.showInLauncher() && LauncherIconManager.isVisible(requireContext())
            setOnPreferenceChangeListener { _, value ->
                val on = value as Boolean
                repo.setShowInLauncher(on)
                LauncherIconManager.setVisible(requireContext(), on)
                true
            }
        }

        findPreference<SwitchPreferenceCompat>("pref_persistent_notification")?.apply {
            isChecked = repo.persistentNotification()
            setOnPreferenceChangeListener { _, value ->
                repo.setPersistentNotification(value as Boolean)
                true
            }
        }

        findPreference<SwitchPreferenceCompat>("pref_require_unlocked")?.apply {
            // Global default for new routines.
            isVisible = false
        }

        findPreference<SeekBarPreference>("pref_location_interval_min")?.apply {
            value = repo.locationPollMinutes()
            setOnPreferenceChangeListener { _, v ->
                repo.setLocationPollMinutes(v as Int)
                app.triggerScheduler.scheduleAllEnabled()
                true
            }
        }

        findPreference<Preference>("pref_about")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), MainActivity::class.java)
                .setAction("com.elvettorato.routine.action.SHOW_ABOUT"))
            true
        }
    }
}
