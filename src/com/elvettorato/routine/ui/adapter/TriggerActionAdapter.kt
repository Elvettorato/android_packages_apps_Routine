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
import androidx.recyclerview.widget.RecyclerView
import com.elvettorato.routine.R
import com.elvettorato.routine.data.RoutineAction
import com.elvettorato.routine.data.Trigger
import com.elvettorato.routine.util.RoutineFormatter
import com.google.android.material.button.MaterialButton

/**
 * Adapter that renders either triggers or actions as small Material 3 cards
 * inside the editor. Generic over [Any] because both lists share the same item
 * layout; we only differentiate when computing icon and label.
 */
class TriggerActionAdapter(
    private val triggers: Boolean,
    private val onClick: (Any) -> Unit,
    private val onRemove: (Any) -> Unit,
) : RecyclerView.Adapter<TriggerActionAdapter.VH>() {

    private val items = mutableListOf<Any>()

    fun submit(newItems: List<Any>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trigger_action, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val icon = view.findViewById<ImageView>(R.id.icon)
        private val title = view.findViewById<TextView>(R.id.title)
        private val summary = view.findViewById<TextView>(R.id.summary)
        private val btnRemove = view.findViewById<MaterialButton>(R.id.btn_remove)

        fun bind(item: Any) {
            val ctx = itemView.context
            when (item) {
                is Trigger -> {
                    icon.setImageResource(iconFor(item))
                    title.setText(triggerTitleRes(item))
                    summary.text = RoutineFormatter.triggerSummary(ctx, item)
                }
                is RoutineAction -> {
                    icon.setImageResource(iconFor(item))
                    title.setText(actionTitleRes(item))
                    summary.text = RoutineFormatter.actionSummary(ctx, item)
                }
            }
            itemView.setOnClickListener { onClick(item) }
            btnRemove.setOnClickListener { onRemove(item) }
        }
    }

    companion object {
        fun iconFor(trigger: Trigger): Int = when (trigger) {
            is Trigger.Time -> R.drawable.ic_trigger_time
            is Trigger.Location -> R.drawable.ic_trigger_location
            is Trigger.Charging -> R.drawable.ic_trigger_charging
        }

        fun iconFor(action: RoutineAction): Int = when (action) {
            is RoutineAction.Dnd -> R.drawable.ic_action_dnd
            is RoutineAction.Volume -> R.drawable.ic_action_volume
            is RoutineAction.RingerMode -> R.drawable.ic_action_ringer
            is RoutineAction.Brightness -> R.drawable.ic_action_brightness
            is RoutineAction.Wifi -> R.drawable.ic_action_wifi
            is RoutineAction.Bluetooth -> R.drawable.ic_action_bluetooth
            is RoutineAction.Airplane -> R.drawable.ic_action_airplane
            is RoutineAction.Notification -> R.drawable.ic_action_notification
        }

        fun triggerTitleRes(trigger: Trigger): Int = when (trigger) {
            is Trigger.Time -> R.string.trigger_time
            is Trigger.Location -> if (trigger.enter) R.string.trigger_location_enter else R.string.trigger_location_exit
            is Trigger.Charging -> R.string.trigger_charging
        }

        fun actionTitleRes(action: RoutineAction): Int = when (action) {
            is RoutineAction.Dnd -> R.string.action_dnd
            is RoutineAction.Volume -> R.string.action_volume
            is RoutineAction.RingerMode -> R.string.action_ringer_mode
            is RoutineAction.Brightness -> R.string.action_brightness
            is RoutineAction.Wifi -> R.string.action_wifi
            is RoutineAction.Bluetooth -> R.string.action_bluetooth
            is RoutineAction.Airplane -> R.string.action_airplane
            is RoutineAction.Notification -> R.string.action_notification
        }
    }
}
