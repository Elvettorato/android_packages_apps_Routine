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

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.elvettorato.routine.R

/**
 * Per-routine options shown inline at the bottom of [RoutineEditorActivity].
 * Only contains options that vary between routines (currently
 * "require unlocked" — the global options live in [OptionsFragment]).
 */
class RoutineOptionsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = null
        setPreferencesFromResource(R.xml.routine_options_preferences, rootKey)

        val initial = requireArguments().getBoolean(KEY_REQUIRE_UNLOCKED, false)
        findPreference<SwitchPreferenceCompat>("pref_require_unlocked_local")?.apply {
            isChecked = initial
            setOnPreferenceChangeListener { _, value ->
                setFragmentResult(RESULT_KEY, bundleOf(KEY_REQUIRE_UNLOCKED to (value as Boolean)))
                true
            }
        }
    }

    companion object {
        const val RESULT_KEY = "routine_options_result"
        const val KEY_REQUIRE_UNLOCKED = "require_unlocked"

        fun newInstance(requireUnlocked: Boolean) = RoutineOptionsFragment().apply {
            arguments = bundleOf(KEY_REQUIRE_UNLOCKED to requireUnlocked)
        }
    }
}
