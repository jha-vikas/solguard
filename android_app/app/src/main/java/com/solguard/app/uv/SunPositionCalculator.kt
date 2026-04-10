package com.solguard.app.uv

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

data class SunPosition(
    val altitude: Double,
    val azimuth: Double,
    val isDaytime: Boolean
)

object SunPositionCalculator {

    fun calculate(lat: Double, lon: Double, calendar: Calendar): SunPosition {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = calendar.timeInMillis
        }

        val jd = julianDay(utcCal)
        val jc = (jd - 2451545.0) / 36525.0

        val geomMeanLon = (280.46646 + jc * (36000.76983 + 0.0003032 * jc)) % 360
        val geomMeanAnom = Math.toRadians(357.52911 + jc * (35999.05029 - 0.0001537 * jc))
        val ecc = 0.016708634 - jc * (0.000042037 + 0.0000001267 * jc)

        val sunEqCtr = sin(geomMeanAnom) * (1.914602 - jc * (0.004817 + 0.000014 * jc)) +
                sin(2 * geomMeanAnom) * (0.019993 - 0.000101 * jc) +
                sin(3 * geomMeanAnom) * 0.000289

        val sunTrueLon = geomMeanLon + sunEqCtr
        val omega = Math.toRadians(125.04 - 1934.136 * jc)
        val sunAppLon = Math.toRadians(sunTrueLon - 0.00569 - 0.00478 * sin(omega))

        val meanObliq = 23 + (26 + (21.448 - jc * (46.815 + jc * (0.00059 - jc * 0.001813))) / 60) / 60
        val obliqCorr = Math.toRadians(meanObliq + 0.00256 * cos(omega))

        val sinDec = sin(obliqCorr) * sin(sunAppLon)
        val declination = asin(sinDec)

        val varY = tan(obliqCorr / 2).pow(2)
        val geomMeanLonRad = Math.toRadians(geomMeanLon)
        val eqOfTime = 4 * Math.toDegrees(
            varY * sin(2 * geomMeanLonRad) -
                    2 * ecc * sin(geomMeanAnom) +
                    4 * ecc * varY * sin(geomMeanAnom) * cos(2 * geomMeanLonRad) -
                    0.5 * varY * varY * sin(4 * geomMeanLonRad) -
                    1.25 * ecc * ecc * sin(2 * geomMeanAnom)
        )

        val timeOffset = eqOfTime + 4 * lon
        val hoursFromMidnight = utcCal.get(Calendar.HOUR_OF_DAY) +
                utcCal.get(Calendar.MINUTE) / 60.0 +
                utcCal.get(Calendar.SECOND) / 3600.0
        val trueSolarTime = (hoursFromMidnight * 60 + timeOffset) % 1440

        val hourAngle = if (trueSolarTime / 4 < 0) trueSolarTime / 4 + 180
        else trueSolarTime / 4 - 180
        val haRad = Math.toRadians(hourAngle)

        val latRad = Math.toRadians(lat)
        var cosZenith = sin(latRad) * sin(declination) +
                cos(latRad) * cos(declination) * cos(haRad)
        cosZenith = cosZenith.coerceIn(-1.0, 1.0)
        val zenith = Math.toDegrees(acos(cosZenith))
        val altitude = 90.0 - zenith

        val zenithRad = Math.toRadians(zenith)
        val azimuth = if (zenithRad != 0.0) {
            var cosAz = (sin(declination) - sin(latRad) * cos(zenithRad)) /
                    (cos(latRad) * sin(zenithRad))
            cosAz = cosAz.coerceIn(-1.0, 1.0)
            val az = Math.toDegrees(acos(cosAz))
            if (hourAngle > 0) (360 - az) % 360 else az
        } else {
            if (lat >= 0) 0.0 else 180.0
        }

        return SunPosition(
            altitude = (altitude * 100).roundToInt() / 100.0,
            azimuth = (azimuth * 100).roundToInt() / 100.0,
            isDaytime = altitude > 0
        )
    }

    fun getCompassDirection(azimuth: Double): String {
        return when {
            azimuth < 22.5 -> "N"
            azimuth < 67.5 -> "NE"
            azimuth < 112.5 -> "E"
            azimuth < 157.5 -> "SE"
            azimuth < 202.5 -> "S"
            azimuth < 247.5 -> "SW"
            azimuth < 292.5 -> "W"
            azimuth < 337.5 -> "NW"
            else -> "N"
        }
    }

    private fun julianDay(cal: Calendar): Double {
        var y = cal.get(Calendar.YEAR)
        var m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH) +
                cal.get(Calendar.HOUR_OF_DAY) / 24.0 +
                cal.get(Calendar.MINUTE) / 1440.0 +
                cal.get(Calendar.SECOND) / 86400.0

        if (m <= 2) { y -= 1; m += 12 }
        val a = y / 100
        val b = 2 - a + a / 4

        return (365.25 * (y + 4716)).toInt() +
                (30.6001 * (m + 1)).toInt() + d + b - 1524.5
    }

    private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
}
