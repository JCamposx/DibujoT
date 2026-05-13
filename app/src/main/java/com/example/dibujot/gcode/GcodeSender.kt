package com.example.dibujot.gcode

import com.example.dibujot.serial.SerialPort
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

class GcodeSender(
    private val port: SerialPort,
    private val ack: String = "ok\n",
    private val ackTimeoutMs: Long = 5000L
) {

    suspend fun send(
        lines: List<String>,
        onProgress: ((linesSent: Int, total: Int) -> Unit)? = null
    ) {
        val total = lines.size
        for ((index, line) in lines.withIndex()) {
            port.send(line)
            withTimeout(ackTimeoutMs) {
                var response: String? = null
                while (response != ack) {
                    response = port.readLine()
                    if (response == null) {
                        delay(10)
                    }
                }
            }
            onProgress?.invoke(index + 1, total)
        }
    }
}
