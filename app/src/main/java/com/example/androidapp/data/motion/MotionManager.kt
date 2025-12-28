package com.example.androidapp.data.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

class MotionManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private val _rotation = MutableStateFlow(Pair(0f, 0f)) // Pitch, Roll
    val rotation = _rotation.asStateFlow()

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            
            // Calculate Tilt
            val pitch = atan2(-x, sqrt(y*y + z*z)) * 57.29578f
            val roll = atan2(y, z) * 57.29578f
            
            // Clamp and Smooth (Subtle 10-degree range)
            _rotation.value = Pair(
                (pitch / 9f).coerceIn(-10f, 10f),
                (roll / 9f).coerceIn(-10f, 10f)
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
