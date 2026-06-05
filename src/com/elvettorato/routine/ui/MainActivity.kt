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
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elvettorato.routine.R
import com.elvettorato.routine.RoutineApp
import com.elvettorato.routine.action.ActionExecutor
import com.elvettorato.routine.data.Routine
import com.elvettorato.routine.ui.adapter.RoutineListAdapter
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Top-level activity: shows the list of [Routine]s, lets the user add/edit/run
 * them. Reachable either from the launcher (when the alias is enabled) or from
 * the Settings entry under Display / Modes.
 */
class MainActivity : AppCompatActivity() {

    private val app: RoutineApp by lazy { application as RoutineApp }
    private lateinit var adapter: RoutineListAdapter
    private lateinit var emptyState: View
    private lateinit var list: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar).title =
            getString(R.string.routines_title)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_options -> {
                    startActivity(Intent(this, OptionsActivity::class.java))
                    true
                }
                R.id.menu_about -> {
                    showAbout()
                    true
                }
                else -> false
            }
        }

        list = findViewById(R.id.routines_list)
        emptyState = findViewById(R.id.empty_state)
        adapter = RoutineListAdapter(
            onClick = { editRoutine(it) },
            onToggle = { r, enabled -> toggleEnabled(r, enabled) },
            onRun = { runNow(it) },
            onEdit = { editRoutine(it) },
            onDelete = { confirmDelete(it) },
        )
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        findViewById<ExtendedFloatingActionButton>(R.id.fab_add).setOnClickListener {
            editRoutine(null)
        }

        lifecycleScope.launch {
            app.repository.routines.collectLatest { render(it) }
        }
    }

    private fun render(routines: List<Routine>) {
        adapter.submit(routines)
        val empty = routines.isEmpty()
        emptyState.visibility = if (empty) View.VISIBLE else View.GONE
        list.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun editRoutine(routine: Routine?) {
        val intent = Intent(this, RoutineEditorActivity::class.java)
        if (routine != null) intent.putExtra(RoutineEditorActivity.EXTRA_ID, routine.id)
        startActivity(intent)
    }

    private fun toggleEnabled(routine: Routine, enabled: Boolean) {
        app.repository.setEnabled(routine.id, enabled)
        app.triggerScheduler.reschedule(routine.copy(enabled = enabled))
    }

    private fun runNow(routine: Routine) {
        ActionExecutor(this).apply(routine)
        app.repository.touchLastRun(routine.id, System.currentTimeMillis())
    }

    private fun confirmDelete(routine: Routine) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_routine_confirm, routine.name))
            .setMessage(R.string.delete_routine_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_routine) { _, _ ->
                app.triggerScheduler.cancel(routine)
                app.repository.delete(routine.id)
            }
            .show()
    }

    private fun showAbout() {
        val pkg = packageManager.getPackageInfo(packageName, 0)
        AlertDialog.Builder(this)
            .setTitle(R.string.about_title)
            .setMessage(
                getString(R.string.about_version, pkg.versionName ?: "1.0") +
                    "\n" + getString(R.string.about_copyright)
            )
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}
