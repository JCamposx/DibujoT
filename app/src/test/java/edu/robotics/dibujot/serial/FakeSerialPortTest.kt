package edu.robotics.dibujot.serial

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FakeSerialPortTest {

    // Scenario: Programmed responses returned in order
    @Test
    fun `readLine returns programmed responses in order`() {
        val fake = FakeSerialPort(responses = listOf("ok\n", "ok\n"))
        fake.connect()
        assertEquals("ok\n", fake.readLine())
        assertEquals("ok\n", fake.readLine())
    }

    // Triangulation: different responses in different order
    @Test
    fun `readLine returns distinct responses in insertion order`() {
        val fake = FakeSerialPort(responses = listOf("ok\n", "error\n", "ok\n"))
        fake.connect()
        assertEquals("ok\n", fake.readLine())
        assertEquals("error\n", fake.readLine())
        assertEquals("ok\n", fake.readLine())
    }

    // Edge case: readLine returns null when queue is exhausted
    @Test
    fun `readLine returns null when no more responses are queued`() {
        val fake = FakeSerialPort(responses = listOf("ok\n"))
        fake.connect()
        fake.readLine() // consume the only response
        assertNull(fake.readLine())
    }

    // Scenario: Written bytes are captured
    @Test
    fun `send records lines written`() {
        val fake = FakeSerialPort()
        fake.connect()
        fake.send("G28\n")
        assertEquals(listOf("G28\n"), fake.sentLines)
    }

    // Triangulation: multiple sends accumulate
    @Test
    fun `send accumulates multiple lines in order`() {
        val fake = FakeSerialPort()
        fake.connect()
        fake.send("G28\n")
        fake.send("G1 X10\n")
        assertEquals(listOf("G28\n", "G1 X10\n"), fake.sentLines)
    }

    // Scenario: disconnect resets state
    @Test
    fun `disconnect clears sentLines and resets response queue`() {
        val fake = FakeSerialPort(responses = listOf("ok\n"))
        fake.connect()
        fake.send("G28\n")
        fake.disconnect()
        assertEquals(emptyList<String>(), fake.sentLines)
        assertNull(fake.readLine())
    }

    // connect() is idempotent — calling it again after disconnect restores original responses
    @Test
    fun `connect after disconnect restores original responses`() {
        val fake = FakeSerialPort(responses = listOf("ok\n"))
        fake.connect()
        fake.readLine()  // consume
        fake.disconnect()
        fake.connect()   // re-connect should restore responses
        assertEquals("ok\n", fake.readLine())
    }
}
