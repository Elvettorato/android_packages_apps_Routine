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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elvettorato.routine.R
import com.elvettorato.routine.RoutineApp
import com.elvettorato.routine.data.Routine
import com.elvettorato.routine.data.RoutineAction
import com.elvettorato.routine.data.Trigger
import com.elvettorato.routine.ui.adapter.TriggerActionAdapter
import com.elvettorato.routine.ui.picker.ActionPickers
import com.elvettorato.routine.ui.picker.TriggerPickers
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

/**
 * Edits one [Routine]. Brand new routines are created from the FAB on
 * [MainActivity] (with no [EXTRA_ID]); existing ones are passed by id.
 *
 * State lives in-memory until the user taps Save — pressing Back with unsaved
 * changes prompts a Material 3 confirmation dialog.
 */
class RoutineEditorActivity : AppCompatActivity() {

    private val app: RoutineApp by lazy { application as RoutineApp }

    private lateinit var nameField: TextInputEditText
    private lateinit var triggerAdapter: TriggerActionAdapter
    private lateinit var actionAdapter: TriggerActionAdapter
    private lateinit var triggerList: RecyclerView
    private lateinit var actionList: RecyclerView

    private var working: Routine = Routine(name = "")
    private var pristine: Routine = working
    private var isNew = true

    private val pickPlace = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val trigger = Trigger.Location(
                placeName = data.getStringExtra(PlacePickerActivity.RESULT_NAME).orEmpty(),
                latitude = data.getDoubleExtra(PlacePickerActivity.RESULT_LAT, 0.0),
                longitude = data.getDoubleExtra(PlacePickerActivity.RESULT_LON, 0.0),
                radiusMeters = data.getFloatExtra(PlacePickerActivity.RESULT_RADIUS, 150f),
                enter = data.getBooleanExtra(PlacePickerActivity.RESULT_ENTER, true),
            )
            working = working.copy(triggers = working.triggers + trigger)
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val id = intent.getStringExtra(EXTRA_ID)
        if (id != null) {
            app.repository.byId(id)?.let {
                working = it
                pristine = it
                isNew = false
            }
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = if (isNew) getString(R.string.new_routine_title)
        else getString(R.string.edit_routine_title)
        toolbar.setNavigationOnClickListener { confirmBack() }
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.menu_delete) {
                confirmDelete()
                true
            } else false
        }
        // Hide delete in the new-routine state
        toolbar.menu.findItem(R.id.menu_delete)?.isVisible = !isNew

        nameField = findViewById(R.id.edit_name)
        nameField.setText(working.name)

        triggerList = findViewById(R.id.triggers_list)
        actionList = findViewById(R.id.actions_list)
        triggerList.layoutManager = LinearLayoutManager(this)
        actionList.layoutManager = LinearLayoutManager(this)

        triggerAdapter = TriggerActionAdapter(
            triggers = true,
            onClick = { item -> if (item is Trigger) editTrigger(item) },
            onRemove = { item -> if (item is Trigger) removeTrigger(item) },
        )
        actionAdapter = TriggerActionAdapter(
            triggers = false,
            onClick = { item -> if (item is RoutineAction) editAction(item) },
            onRemove = { item -> if (item is RoutineAction) removeAction(item) },
        )
        triggerList.adapter = triggerAdapter
        actionList.adapter = actionAdapter

        findViewById<MaterialButton>(R.id.btn_add_trigger).setOnClickListener { showTriggerPicker() }
        findViewById<MaterialButton>(R.id.btn_add_action).setOnClickListener { showActionPicker() }

        findViewById<ExtendedFloatingActionButton>(R.id.fab_save).setOnClickListener { save() }

        // Options inline view → host the per-routine OptionsFragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.options_container, RoutineOptionsFragment.newInstance(working.requireUnlocked))
            .commit()
        supportFragmentManager.setFragmentResultListener(
            RoutineOptionsFragment.RESULT_KEY, this
        ) { _, bundle ->
            working = working.copy(
                requireUnlocked = bundle.getBoolean(RoutineOptionsFragment.KEY_REQUIRE_UNLOCKED)
            )
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { confirmBack() }
        })

        refresh()
    }

    private fun refresh() {
        triggerAdapter.submit(working.triggers)
        actionAdapter.submit(working.actions)
    }

    // ─── pickers ───────────────────────────────────────────────────────────

    private fun showTriggerPicker() {
        TriggerPickers.pickType(this) { newTrigger ->
            if (newTrigger is Trigger.Location) {
                val intent = Intent(this, PlacePickerActivity::class.java)
                    .putExtra(PlacePickerActivity.EXTRA_ENTER, newTrigger.enter)
                pickPlace.launch(intent)
            } else {
                working = working.copy(triggers = working.triggers + newTrigger)
                refresh()
            }
        }
    }

    private fun showActionPicker() {
        ActionPickers.pickType(this) { newAction ->
            working = working.copy(actions = working.actions + newAction)
            refresh()
        }
    }

    private fun editTrigger(trigger: Trigger) {
        TriggerPickers.edit(this, trigger) { edited ->
            working = working.copy(
                triggers = working.triggers.map { if (it.id == trigger.id) edited else it }
            )
            refresh()
        }
    }

    private fun editAction(action: RoutineAction) {
        ActionPickers.edit(this, action) { edited ->
            working = working.copy(
                actions = working.actions.map { if (it.id == action.id) edited else it }
            )
            refresh()
        }
    }

    private fun removeTrigger(trigger: Trigger) {
        working = working.copy(triggers = working.triggers.filterNot { it.id == trigger.id })
        refresh()
    }

    private fun removeAction(action: RoutineAction) {
        working = working.copy(actions = working.actions.filterNot { it.id == action.id })
        refresh()
    }

    // ─── save / cancel / delete ────────────────────────────────────────────

    private fun save() {
        val name = nameField.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.error_no_name, Toast.LENGTH_SHORT).show()
            return
        }
        if (working.triggers.isEmpty()) {
            Toast.makeText(this, R.string.error_no_trigger, Toast.LENGTH_SHORT).show()
            return
        }
        if (working.actions.isEmpty()) {
            Toast.makeText(this, R.string.error_no_action, Toast.LENGTH_SHORT).show()
            return
        }
        working = working.copy(name = name)
        app.repository.save(working)
        app.triggerScheduler.reschedule(working)
        finish()
    }

    private fun confirmBack() {
        val current = working.copy(name = nameField.text?.toString().orEmpty())
        if (current == pristine) {
            finish(); return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.discard_changes)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.discard) { _, _ -> finish() }
            .show()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_routine_confirm, working.name))
            .setMessage(R.string.delete_routine_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_routine) { _, _ ->
                app.triggerScheduler.cancel(working)
                app.repository.delete(working.id)
                finish()
            }
            .show()
    }

    companion object {
        const val EXTRA_ID = "routine_id"
    }
}
