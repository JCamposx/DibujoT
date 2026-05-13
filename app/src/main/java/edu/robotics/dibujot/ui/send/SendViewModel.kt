package edu.robotics.dibujot.ui.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.robotics.dibujot.gcode.GcodeSender
import edu.robotics.dibujot.serial.SerialPort
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SendViewModel(
    private val testDispatcher: CoroutineDispatcher? = null,
    private val ackTimeoutMs: Long = 5000L
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var serialPort: SerialPort? = null

    private val dispatcher: CoroutineDispatcher
        get() = testDispatcher ?: Dispatchers.IO

    fun connect(port: SerialPort) {
        val scope = if (testDispatcher != null) {
            kotlinx.coroutines.CoroutineScope(testDispatcher)
        } else {
            viewModelScope
        }
        scope.launch {
            _uiState.value = UiState.Connecting
            try {
                port.connect()
                serialPort = port
                _uiState.value = UiState.Ready
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Connection failed")
            }
        }
    }

    /** Test helper: inject an already-connected port directly. */
    fun connectWithPort(port: SerialPort) {
        serialPort = port
        _uiState.value = UiState.Ready
    }

    fun startSend(lines: List<String>) {
        val port = serialPort ?: run {
            _uiState.value = UiState.Error("No port connected")
            return
        }
        val scope = if (testDispatcher != null) {
            kotlinx.coroutines.CoroutineScope(testDispatcher)
        } else {
            viewModelScope
        }
        scope.launch {
            val sender = GcodeSender(port, ackTimeoutMs = ackTimeoutMs)
            try {
                sender.send(lines) { sent, total ->
                    _uiState.value = UiState.Sending(sent, total)
                }
                _uiState.value = UiState.Done
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Send failed")
            }
        }
    }
}
