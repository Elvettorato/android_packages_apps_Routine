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
import com.elvettorato.routine.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Lightweight 7-day picker. Bit 0 = Monday, bit 6 = Sunday — matching
 * [com.elvettorato.routine.data.Trigger.Time.daysOfWeek].
 */
object DaysPicker {

    fun show(context: Context, currentMask: Int, onPicked: (Int) -> Unit) {
        val view = android.view.LayoutInflater.from(context)
            .inflate(R.layout.dialog_time_days, null)

        val buttons = listOf(
            R.id.day_mon to 0, R.id.day_tue to 1, R.id.day_wed to 2,
            R.id.day_thu to 3, R.id.day_fri to 4, R.id.day_sat to 5,
            R.id.day_sun to 6,
        ).map { (id, bit) -> view.findViewById<MaterialButton>(id) to bit }

        // Pre-select
        buttons.forEach { (btn, bit) ->
            btn.isChecked = (currentMask and (1 shl bit)) != 0
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.trigger_time_days)
            .setView(view)
            .setPositiveButton(R.string.ok) { _, _ ->
                var mask = 0
                buttons.forEach { (btn, bit) -> if (btn.isChecked) mask = mask or (1 shl bit) }
                if (mask == 0) mask = 0b1111111 // never allow zero
                onPicked(mask)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
