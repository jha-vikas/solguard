package com.solguard.app.session

import com.solguard.app.uv.HourlyUV
import com.solguard.app.uv.UVCategory
import com.solguard.app.uv.UVEstimate

data class SessionState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitudeMeters: Double = 0.0,
    val uvIndex: Double = 0.0,
    val uvEstimate: UVEstimate? = null,
    val uvCategory: UVCategory? = null,
    val sunAltitude: Double = 0.0,
    val sunAzimuth: Double = 0.0,
    val solarZenithAngle: Double = 90.0,
    val safeExposureMinutes: Int = 0,
    val safeExposureWithSPF: Int = 0,
    val vitaminDMinutes: Int = 0,
    val hourlyCurve: List<HourlyUV> = emptyList(),
    val skinTypeIndex: Int = 2,
    val spf: Int = 30,
    val surfaceType: String = "grass",
    val hasAssessed: Boolean = false
)
