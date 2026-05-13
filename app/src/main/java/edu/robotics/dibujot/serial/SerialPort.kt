package edu.robotics.dibujot.serial

interface SerialPort : java.io.Closeable {
    fun connect(baudRate: Int = 115200)
    fun send(line: String)
    fun readLine(): String?
    fun disconnect()
    override fun close() = disconnect()
}
