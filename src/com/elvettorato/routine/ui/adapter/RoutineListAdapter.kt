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
package com.elvettorato.routine.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.elvettorato.routine.R
import com.elvettorato.routine.data.Routine
import com.elvettorato.routine.data.Trigger
import com.elvettorato.routine.util.RoutineFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

class RoutineListAdapter(
    private val onClick: (Routine) -> Unit,
    private val onToggle: (Routine, Boolean) -> Unit,
    private val onRun: (Routine) -> Unit,
    private val onEdit: (Routine) -> Unit,
    private val onDelete: (Routine) -> Unit,
) : ListAdapter<Routine, RoutineListAdapter.VH>(DIFF) {

    fun submit(items: List<Routine>) = submitList(items)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_routine, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val icon = view.findViewById<ImageView>(R.id.icon)
        private val name = view.findViewById<TextView>(R.id.name)
        private val summary = view.findViewById<TextView>(R.id.summary)
        private val sw = view.findViewById<MaterialSwitch>(R.id.enabled_switch)
        private val btnRun = view.findViewById<MaterialButton>(R.id.btn_run_now)
        private val btnEdit = view.findViewById<MaterialButton>(R.id.btn_edit)
        private val btnDelete = view.findViewById<MaterialButton>(R.id.btn_delete)

        fun bind(routine: Routine) {
            name.text = routine.name.ifBlank {
                itemView.context.getString(R.string.new_routine_title)
            }
            summary.text = RoutineFormatter.summary(itemView.context, routine)
            icon.setImageResource(iconRes(routine))
            sw.setOnCheckedChangeListener(null)
            sw.isChecked = routine.enabled
            sw.setOnCheckedChangeListener { _, isChecked -> onToggle(routine, isChecked) }
            itemView.setOnClickListener { onClick(routine) }
            btnRun.setOnClickListener { onRun(routine) }
            btnEdit.setOnClickListener { onEdit(routine) }
            btnDelete.setOnClickListener { onDelete(routine) }
        }

        private fun iconRes(routine: Routine): Int {
            val firstTrigger = routine.triggers.firstOrNull()
            return when (firstTrigger) {
                is Trigger.Time -> R.drawable.ic_trigger_time
                is Trigger.Location -> R.drawable.ic_trigger_location
                is Trigger.Charging -> R.drawable.ic_trigger_charging
                else -> R.drawable.ic_trigger_time
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Routine>() {
            override fun areItemsTheSame(oldItem: Routine, newItem: Routine) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Routine, newItem: Routine) = oldItem == newItem
        }
    }
}
