package com.example.dibujot.serial

import android.content.Context
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException

/**
 * Real SerialPort implementation using usb-serial-for-android.
 *
 * NOTE: This class wraps hardware — it is intentionally NOT covered by JVM unit tests.
 * The testability wall is [FakeSerialPort]. Test [GcodeSender] using [FakeSerialPort].
 */
class UsbSerialPort(
    private val context: Context,
    private val baudRate: Int = 115200
) : SerialPort {

    private var port: com.hoho.android.usbserial.driver.UsbSerialPort? = null
    private val buffer = StringBuilder()

    override fun connect(baudRate: Int) {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers: List<UsbSerialDriver> =
            UsbSerialProber.getDefaultProber().findAllDrivers(manager)

        if (availableDrivers.isEmpty()) {
            throw IOException("No USB serial device found")
        }

        val driver = availableDrivers.first()
        val connection = manager.openDevice(driver.device)
            ?: throw IOException("Could not open USB device — permission not granted?")

        val serialPort = driver.ports.first()
        serialPort.open(connection)
        serialPort.setParameters(baudRate, 8, com.hoho.android.usbserial.driver.UsbSerialPort.STOPBITS_1, com.hoho.android.usbserial.driver.UsbSerialPort.PARITY_NONE)
        this.port = serialPort
    }

    override fun send(line: String) {
        val data = if (line.endsWith("\n")) line else "$line\n"
        port?.write(data.toByteArray(), 1000)
            ?: throw IOException("Port not connected")
    }

    override fun readLine(): String? {
        val buf = ByteArray(64)
        val p = port ?: throw IOException("Port not connected")
        val len = p.read(buf, 1000)
        if (len > 0) {
            buffer.append(String(buf, 0, len))
        }
        val newlineIdx = buffer.indexOf('\n')
        if (newlineIdx < 0) return null
        val line = buffer.substring(0, newlineIdx + 1)
        buffer.delete(0, newlineIdx + 1)
        return line
    }

    override fun disconnect() {
        port?.close()
        port = null
        buffer.clear()
    }
}
