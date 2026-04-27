package com.example.csor.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MotionViewModel : ViewModel() {
    private val _completedStrokes = MutableStateFlow<List<List<Offset>>>(emptyList())
    val completedStrokes: StateFlow<List<List<Offset>>> = _completedStrokes.asStateFlow()

    private val _currentStroke = MutableStateFlow<List<Offset>>(emptyList())
    val currentStroke: StateFlow<List<Offset>> = _currentStroke.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private var cursorX = 0f
    private var cursorY = 0f
    private var isInitialized = false

    var canvasWidth = 1080f
    var canvasHeight = 1920f

    fun setCanvasSize(width: Float, height: Float) {
        canvasWidth = width
        canvasHeight = height
    }

    fun toggleTracking() {
        if (_isTracking.value) {
            if (_currentStroke.value.isNotEmpty()) {
                _completedStrokes.value = _completedStrokes.value + listOf(_currentStroke.value)
                _currentStroke.value = emptyList()
            }
            _isTracking.value = false
        } else {
            _isTracking.value = true
        }
    }

    fun clearCanvas() {
        _completedStrokes.value = emptyList()
        _currentStroke.value = emptyList()
        isInitialized = false
    }

    fun addDeltas(deltaX: Float, deltaY: Float) {
        if (!_isTracking.value) return

        if (!isInitialized) {
            cursorX = canvasWidth / 2f
            cursorY = canvasHeight / 2f
            isInitialized = true
        }
        
        cursorX = (cursorX + deltaX).coerceIn(0f, canvasWidth)
        cursorY = (cursorY + deltaY).coerceIn(0f, canvasHeight)
        
        _currentStroke.value = _currentStroke.value + Offset(cursorX, cursorY)
    }
}
