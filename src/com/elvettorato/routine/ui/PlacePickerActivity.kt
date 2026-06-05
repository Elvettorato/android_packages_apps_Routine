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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.elvettorato.routine.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

/**
 * Lets the user define a geofence as { name, latitude, longitude, radius } and
 * returns the result to the calling [RoutineEditorActivity].
 *
 * Uses the AOSP [LocationManager.getCurrentLocation] API for the "use current
 * location" button so we do not depend on Google Play Services.
 */
class PlacePickerActivity : AppCompatActivity() {

    private lateinit var nameField: TextInputEditText
    private lateinit var latField: TextInputEditText
    private lateinit var lonField: TextInputEditText
    private lateinit var radiusSlider: Slider
    private lateinit var radiusLabel: TextView

    private var enter: Boolean = true

    private val requestPerm = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) fillFromCurrent() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_picker)

        enter = intent.getBooleanExtra(EXTRA_ENTER, true)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        nameField = findViewById(R.id.place_name)
        latField = findViewById(R.id.edit_lat)
        lonField = findViewById(R.id.edit_lon)
        radiusSlider = findViewById(R.id.slider_radius)
        radiusLabel = findViewById(R.id.label_radius)

        intent.getStringExtra(EXTRA_NAME)?.let { nameField.setText(it) }
        if (intent.hasExtra(EXTRA_LAT)) {
            latField.setText("%.6f".format(Locale.US, intent.getDoubleExtra(EXTRA_LAT, 0.0)))
        }
        if (intent.hasExtra(EXTRA_LON)) {
            lonField.setText("%.6f".format(Locale.US, intent.getDoubleExtra(EXTRA_LON, 0.0)))
        }
        radiusSlider.value = intent.getFloatExtra(EXTRA_RADIUS, 150f)
            .coerceIn(50f, 1000f)
        updateRadiusLabel(radiusSlider.value.toInt())
        radiusSlider.addOnChangeListener { _, v, _ -> updateRadiusLabel(v.toInt()) }

        findViewById<MaterialButton>(R.id.btn_current_location).setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                requestPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                fillFromCurrent()
            }
        }
        findViewById<MaterialButton>(R.id.btn_save_place).setOnClickListener { save() }
    }

    private fun updateRadiusLabel(meters: Int) {
        radiusLabel.text = getString(R.string.place_radius, meters)
    }

    @SuppressWarnings("MissingPermission")
    private fun fillFromCurrent() {
        val lm = getSystemService(LocationManager::class.java) ?: return
        val provider = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
        ).firstOrNull { lm.isProviderEnabled(it) } ?: return
        try {
            lm.getCurrentLocation(provider, null, mainExecutor) { fix ->
                if (fix != null) {
                    latField.setText("%.6f".format(Locale.US, fix.latitude))
                    lonField.setText("%.6f".format(Locale.US, fix.longitude))
                }
            }
        } catch (sec: SecurityException) {
            // ignored: caller must grant permission
        }
    }

    private fun save() {
        val name = nameField.text?.toString()?.trim().orEmpty()
        val lat = latField.text?.toString()?.toDoubleOrNull() ?: return
        val lon = lonField.text?.toString()?.toDoubleOrNull() ?: return
        val data = Intent().apply {
            putExtra(RESULT_NAME, name)
            putExtra(RESULT_LAT, lat)
            putExtra(RESULT_LON, lon)
            putExtra(RESULT_RADIUS, radiusSlider.value)
            putExtra(RESULT_ENTER, enter)
        }
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
        const val EXTRA_RADIUS = "radius"
        const val EXTRA_ENTER = "enter"

        const val RESULT_NAME = "result_name"
        const val RESULT_LAT = "result_lat"
        const val RESULT_LON = "result_lon"
        const val RESULT_RADIUS = "result_radius"
        const val RESULT_ENTER = "result_enter"
    }
}
