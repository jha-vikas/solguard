package com.solguard.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.solguard.app.data.UVDataRepository
import com.solguard.app.session.SessionState
import com.solguard.app.uv.SensorReader
import com.solguard.app.uv.SunPositionCalculator
import com.solguard.app.uv.UVIndexCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class SolGuardViewModel(application: Application) : AndroidViewModel(application) {

    val sensorReader = SensorReader(application)
    val dataRepository = UVDataRepository(application)

    private val _session = MutableStateFlow(loadPreferences(application))
    val session: StateFlow<SessionState> = _session.asStateFlow()

    fun setLocation(lat: Double, lon: Double, gpsAltitude: Double = 0.0) {
        _session.value = _session.value.copy(
            latitude = lat,
            longitude = lon,
            gpsAltitudeMeters = gpsAltitude
        )
    }

    fun setSkinType(index: Int) {
        _session.value = _session.value.copy(skinTypeIndex = index)
        savePreferences()
    }

    fun setSPF(spf: Int) {
        _session.value = _session.value.copy(spf = spf)
        savePreferences()
    }

    fun setSurfaceType(surface: String) {
        _session.value = _session.value.copy(surfaceType = surface)
        savePreferences()
    }

    fun assessUV() {
        val state = _session.value
        val now = Calendar.getInstance()

        val sunPos = SunPositionCalculator.calculate(state.latitude, state.longitude, now)
        val sza = 90.0 - sunPos.altitude

        val barometerAlt = sensorReader.estimateAltitudeMeters().toDouble()
        val altitude = when {
            barometerAlt > 0 -> barometerAlt
            state.gpsAltitudeMeters > 0 -> state.gpsAltitudeMeters
            else -> state.altitudeMeters
        }

        val month = now.get(Calendar.MONTH)

        val estimate = UVIndexCalculator.estimateUVIndex(
            solarZenithAngle = sza,
            altitudeMeters = altitude,
            ambientLux = sensorReader.currentLux,
            month = month,
            latitude = state.latitude,
            surfaceType = state.surfaceType
        )

        val category = UVIndexCalculator.uvCategory(estimate.uvIndex)
        val safeMin = UVIndexCalculator.safeExposureMinutes(estimate.uvIndex, state.skinTypeIndex, 0)
        val safeWithSPF = UVIndexCalculator.safeExposureMinutes(estimate.uvIndex, state.skinTypeIndex, state.spf)
        val vitD = UVIndexCalculator.vitaminDMinutes(estimate.uvIndex, state.skinTypeIndex)

        val curve = UVIndexCalculator.todayUVCurve(state.latitude, state.longitude, altitude, month)

        _session.value = state.copy(
            altitudeMeters = altitude,
            uvIndex = estimate.uvIndex,
            uvEstimate = estimate,
            uvCategory = category,
            sunAltitude = sunPos.altitude,
            sunAzimuth = sunPos.azimuth,
            solarZenithAngle = sza,
            safeExposureMinutes = safeMin,
            safeExposureWithSPF = safeWithSPF,
            vitaminDMinutes = vitD,
            hourlyCurve = curve,
            hasAssessed = true
        )
    }

    private fun savePreferences() {
        val prefs = getApplication<Application>().getSharedPreferences("solguard", Context.MODE_PRIVATE)
        val state = _session.value
        prefs.edit()
            .putInt("skin_type", state.skinTypeIndex)
            .putInt("spf", state.spf)
            .putString("surface", state.surfaceType)
            .apply()
    }

    override fun onCleared() {
        super.onCleared()
        sensorReader.stopListening()
    }

    companion object {
        private fun loadPreferences(app: Application): SessionState {
            val prefs = app.getSharedPreferences("solguard", Context.MODE_PRIVATE)
            return SessionState(
                skinTypeIndex = prefs.getInt("skin_type", 2),
                spf = prefs.getInt("spf", 30),
                surfaceType = prefs.getString("surface", "grass") ?: "grass"
            )
        }
    }
}
