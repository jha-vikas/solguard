package com.solguard.app.uv

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.pow

data class UVResult(
    val uvIndex: Double,
    val category: String,
    val categoryColor: Long,
    val baseUV: Double,
    val altitudeCorrection: Double,
    val cloudFactor: Double,
    val ozoneFactor: Double,
    val surfaceFactor: Double,
    val solarZenithAngle: Double,
    val safeExposureMinutes: Int,
    val safeExposureWithSPF: Int,
    val vitaminDMinutes: Int,
    val shortAdvice: String,
    val detailedActions: List<String>
)

data class HourlyUV(val hour: Int, val uvIndex: Double)

object UVIndexCalculator {

    // Ozone correction factors: [latitude band index 0-5][month 0-11]
    // Bands: 0-15N, 15-30N, 30-45N, 45-60N, 60-75N, 75-90N (southern mirrored by shifting 6 months)
    private val ozoneFactors = arrayOf(
        doubleArrayOf(1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00), // tropical
        doubleArrayOf(0.98, 0.97, 0.96, 0.95, 0.95, 0.96, 0.97, 0.98, 0.99, 1.00, 1.00, 0.99), // subtropical
        doubleArrayOf(0.95, 0.93, 0.91, 0.90, 0.90, 0.92, 0.94, 0.96, 0.98, 0.99, 0.98, 0.96), // mid-lat
        doubleArrayOf(0.92, 0.90, 0.87, 0.85, 0.85, 0.87, 0.90, 0.93, 0.95, 0.96, 0.95, 0.93), // upper mid
        doubleArrayOf(0.90, 0.87, 0.84, 0.82, 0.82, 0.84, 0.87, 0.90, 0.93, 0.94, 0.93, 0.91), // sub-arctic
        doubleArrayOf(0.88, 0.85, 0.82, 0.80, 0.80, 0.82, 0.85, 0.88, 0.91, 0.92, 0.91, 0.89)  // arctic
    )

    private val surfaceReflection = mapOf(
        "grass" to 1.0,
        "sand" to 1.15,
        "water" to 1.10,
        "snow" to 1.25,
        "concrete" to 1.08
    )

    // MED in Standard Erythemal Doses for Fitzpatrick skin types I-VI
    private val medBySkinType = doubleArrayOf(2.0, 2.5, 3.5, 4.5, 6.0, 10.0)

    // Approximate IU of Vitamin D produced per minute at UV index 5, face+arms exposed
    private val vitdRateBySkinType = doubleArrayOf(6.0, 5.0, 3.5, 2.5, 1.5, 1.0)

    fun estimateUVIndex(
        solarZenithAngle: Double,
        altitudeMeters: Double,
        ambientLux: Float,
        month: Int,
        latitude: Double,
        surfaceType: String = "grass"
    ): UVEstimate {
        if (solarZenithAngle >= 90.0) {
            return UVEstimate(0.0, 1.0, 1.0, 1.0, 1.0)
        }

        val cosZenith = cos(Math.toRadians(solarZenithAngle))
        val baseUV = 12.0 * cosZenith.pow(2.4)

        val altCorr = 1.0 + 0.06 * (altitudeMeters / 1000.0)

        val expectedClearLux = (107_527.0 * cosZenith).coerceAtLeast(1.0)
        val cloudFactor = (ambientLux / expectedClearLux).coerceIn(0.25, 1.0)

        val bandIndex = getLatitudeBand(latitude)
        val effectiveMonth = if (latitude < 0) (month + 6) % 12 else month
        val ozoneFactor = ozoneFactors[bandIndex][effectiveMonth]

        val surfFactor = surfaceReflection[surfaceType] ?: 1.0

        val uv = (baseUV * altCorr * cloudFactor * ozoneFactor * surfFactor).coerceIn(0.0, 15.0)

        return UVEstimate(uv, altCorr, cloudFactor, ozoneFactor, surfFactor)
    }

    fun safeExposureMinutes(uvIndex: Double, skinTypeIndex: Int, spf: Int = 0): Int {
        if (uvIndex <= 0.0) return 999
        val med = medBySkinType[skinTypeIndex.coerceIn(0, 5)]
        val baseMinutes = (med * 40.0) / uvIndex
        val effectiveSPF = if (spf > 1) spf.coerceAtMost(50) else 1
        return (baseMinutes * effectiveSPF).toInt().coerceIn(1, 1440)
    }

    fun vitaminDMinutes(uvIndex: Double, skinTypeIndex: Int): Int {
        if (uvIndex <= 0.5) return 999
        val rateAtUV5 = vitdRateBySkinType[skinTypeIndex.coerceIn(0, 5)]
        val adjustedRate = rateAtUV5 * (uvIndex / 5.0)
        val targetIU = 1000.0
        return (targetIU / adjustedRate).toInt().coerceIn(1, 999)
    }

    fun uvCategory(uvIndex: Double): UVCategory {
        return when {
            uvIndex < 3.0 -> UVCategory("Low", 0xFF4CAF50,
                "Minimal protection needed",
                listOf("Wear sunglasses on bright days", "Use sunscreen if you burn easily"))
            uvIndex < 6.0 -> UVCategory("Moderate", 0xFFFFEB3B,
                "Some protection recommended",
                listOf("Stay in shade near midday", "Wear protective clothing", "Apply SPF 15+ sunscreen"))
            uvIndex < 8.0 -> UVCategory("High", 0xFFFF9800,
                "Protection essential",
                listOf("Reduce sun exposure 10am-4pm", "Seek shade", "Wear hat and sunglasses", "Apply SPF 30+ sunscreen"))
            uvIndex < 11.0 -> UVCategory("Very High", 0xFFFF5722,
                "Extra protection needed",
                listOf("Avoid sun exposure 10am-4pm", "Must seek shade", "Wear SPF 50+", "Protective clothing essential"))
            else -> UVCategory("Extreme", 0xFF9C27B0,
                "Maximum protection required",
                listOf("Avoid ALL outdoor sun exposure", "Stay indoors if possible", "Full protective clothing required", "SPF 50+ reapply every 90 min"))
        }
    }

    /**
     * Compute UV index for each hour of the day at the given location.
     * Uses clear-sky estimates (cloudFactor = 1.0) since we can't predict future clouds.
     */
    fun todayUVCurve(
        lat: Double,
        lon: Double,
        altitudeMeters: Double,
        month: Int
    ): List<HourlyUV> {
        val results = mutableListOf<HourlyUV>()
        val today = Calendar.getInstance()

        for (hour in 5..20) {
            val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
            }
            val sunPos = SunPositionCalculator.calculate(lat, lon, cal)
            val sza = 90.0 - sunPos.altitude
            if (sza < 90.0) {
                val est = estimateUVIndex(sza, altitudeMeters, 100_000f, month, lat)
                results.add(HourlyUV(hour, (est.uvIndex * 10).toInt() / 10.0))
            } else {
                results.add(HourlyUV(hour, 0.0))
            }
        }
        return results
    }

    private fun getLatitudeBand(latitude: Double): Int {
        val absLat = Math.abs(latitude)
        return when {
            absLat < 15 -> 0
            absLat < 30 -> 1
            absLat < 45 -> 2
            absLat < 60 -> 3
            absLat < 75 -> 4
            else -> 5
        }
    }
}

data class UVEstimate(
    val uvIndex: Double,
    val altitudeCorrection: Double,
    val cloudFactor: Double,
    val ozoneFactor: Double,
    val surfaceFactor: Double
)

data class UVCategory(
    val name: String,
    val colorLong: Long,
    val shortAdvice: String,
    val detailedActions: List<String>
)
