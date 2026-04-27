package com.example.csor.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.csor.viewmodel.MotionViewModel
import kotlin.math.abs

class MotionMapper(context: Context, private val viewModel: MotionViewModel) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var lastPitch = 0f
    private var lastRoll = 0f
    private var isFirstRead = true

    // Sensitivity determines how fast the cursor moves relative to phone tilt.
    private val sensitivity = 4000f

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        isFirstRead = true
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        val orientationVals = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientationVals)

        // orientationVals[1] = Pitch (x-axis tilt)
        // orientationVals[2] = Roll (y-axis tilt)
        val pitch = orientationVals[1]
        val roll = orientationVals[2]

        if (isFirstRead) {
            lastPitch = pitch
            lastRoll = roll
            isFirstRead = false
            return
        }

        var deltaPitch = pitch - lastPitch
        var deltaRoll = roll - lastRoll

        // Handle gimbal lock / wrapping edge cases
        if (abs(deltaPitch) > Math.PI) deltaPitch = 0f
        if (abs(deltaRoll) > Math.PI) deltaRoll = 0f

        lastPitch = pitch
        lastRoll = roll

        // Roll controls X-axis movement
        // Pitch controls Y-axis movement (inverted to match screen coords)
        val deltaX = deltaRoll * sensitivity
        val deltaY = -deltaPitch * sensitivity

        viewModel.addDeltas(deltaX, deltaY)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
