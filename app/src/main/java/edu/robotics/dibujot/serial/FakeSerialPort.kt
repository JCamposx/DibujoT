package edu.robotics.dibujot.serial

import java.util.ArrayDeque

class FakeSerialPort(
    private val responses: List<String> = emptyList()
) : SerialPort {

    private val responseQueue = ArrayDeque<String>()
    val sentLines = mutableListOf<String>()

    override fun connect(baudRate: Int) {
        responseQueue.clear()
        responseQueue.addAll(responses)
    }

    override fun send(line: String) {
        sentLines.add(line)
    }

    override fun readLine(): String? = responseQueue.pollFirst()

    override fun disconnect() {
        sentLines.clear()
        responseQueue.clear()
    }
}
