package com.solguard.app.uv

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorReader(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val pressureSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    var currentLux: Float = 0f
        private set

    var currentHeading: Float = 0f
        private set

    /** Barometric pressure in hPa (millibars). 0 if sensor unavailable. */
    var currentPressureHpa: Float = 0f
        private set

    private val lightListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            currentLux = event.values[0]
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val rotationListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (azimuth < 0) azimuth += 360f
            currentHeading = azimuth
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val pressureListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            currentPressureHpa = event.values[0]
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun startListening() {
        lightSensor?.let {
            sensorManager.registerListener(lightListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        rotationSensor?.let {
            sensorManager.registerListener(rotationListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        pressureSensor?.let {
            sensorManager.registerListener(pressureListener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(lightListener)
        sensorManager.unregisterListener(rotationListener)
        sensorManager.unregisterListener(pressureListener)
    }

    fun hasLightSensor(): Boolean = lightSensor != null
    fun hasCompass(): Boolean = rotationSensor != null
    fun hasBarometer(): Boolean = pressureSensor != null

    /**
     * Estimate altitude in meters from barometric pressure using the
     * international barometric formula with standard sea-level pressure.
     * Returns 0 if barometer is unavailable.
     */
    fun estimateAltitudeMeters(): Float {
        if (currentPressureHpa <= 0f) return 0f
        return SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressureHpa)
    }
}
