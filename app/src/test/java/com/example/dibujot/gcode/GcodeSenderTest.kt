package com.example.dibujot.gcode

import com.example.dibujot.serial.FakeSerialPort
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GcodeSenderTest {

    // --- Happy path: all lines sent and ACKed ---

    @Test
    fun `send with N lines and N acks sends all lines`() = runTest {
        val fake = FakeSerialPort(listOf("ok\n", "ok\n", "ok\n"))
        fake.connect()
        val sender = GcodeSender(fake)
        sender.send(listOf("G0 X0", "G1 X10", "G2 X20"))
        assertEquals(listOf("G0 X0", "G1 X10", "G2 X20"), fake.sentLines)
    }

    @Test
    fun `send calls write and readLine once per line`() = runTest {
        val fake = FakeSerialPort(listOf("ok\n", "ok\n", "ok\n"))
        fake.connect()
        val sender = GcodeSender(fake)
        val progress = mutableListOf<Pair<Int, Int>>()
        sender.send(listOf("A", "B", "C")) { sent, total -> progress.add(sent to total) }
        assertEquals(3, fake.sentLines.size)
        // 3 readLine calls consumed all responses
        assertEquals(null, fake.readLine())
    }

    // --- Progress reporting ---

    @Test
    fun `send emits progress 1-2-3 of 3`() = runTest {
        val fake = FakeSerialPort(listOf("ok\n", "ok\n", "ok\n"))
        fake.connect()
        val sender = GcodeSender(fake)
        val events = mutableListOf<Pair<Int, Int>>()
        sender.send(listOf("L1", "L2", "L3")) { sent, total -> events.add(Pair(sent, total)) }
        assertEquals(listOf(Pair(1, 3), Pair(2, 3), Pair(3, 3)), events)
    }

    // --- Custom ACK ---

    @Test
    fun `send with custom ack string completes successfully`() = runTest {
        val fake = FakeSerialPort(listOf("OK\r\n", "OK\r\n"))
        fake.connect()
        val sender = GcodeSender(fake, ack = "OK\r\n")
        sender.send(listOf("X1", "X2"))
        assertEquals(2, fake.sentLines.size)
    }

    // --- ACK timeout ---

    @Test(expected = TimeoutCancellationException::class)
    fun `send throws TimeoutCancellationException when ack never comes`() = runTest {
        val fake = FakeSerialPort(emptyList()) // never returns ack
        fake.connect()
        val sender = GcodeSender(fake, ackTimeoutMs = 100)
        sender.send(listOf("G0 X0"))
    }

    // --- Empty list is a no-op ---

    @Test
    fun `send with empty list is a no-op`() = runTest {
        val fake = FakeSerialPort(emptyList())
        fake.connect()
        val sender = GcodeSender(fake)
        val events = mutableListOf<Pair<Int, Int>>()
        sender.send(emptyList()) { sent, total -> events.add(sent to total) }
        assertTrue(fake.sentLines.isEmpty())
        assertTrue(events.isEmpty())
    }
}
