package edu.robotics.dibujot.ui.send

import edu.robotics.dibujot.data.DrawingItem
import edu.robotics.dibujot.serial.FakeSerialPort
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SendViewModelTest {

    private val testItem = DrawingItem(
        id = 1,
        name = "Drawing 01",
        imageResId = 0,
        gcodeAssetPath = "gcode/drawing_01.gcode"
    )

    // --- Initial state ---

    @Test
    fun `initial state is Idle`() = runTest {
        val vm = SendViewModel(testDispatcher = StandardTestDispatcher(testScheduler))
        assertEquals(UiState.Idle, vm.uiState.value)
    }

    // --- connect success ---

    @Test
    fun `connect success transitions Idle to Connecting to Ready`() = runTest {
        val fake = FakeSerialPort(emptyList())
        val vm = SendViewModel(testDispatcher = StandardTestDispatcher(testScheduler))
        vm.connect(fake)
        advanceUntilIdle()
        assertEquals(UiState.Ready, vm.uiState.value)
    }

    // --- connect failure ---

    @Test
    fun `connect failure transitions to Error with message`() = runTest {
        val fake = object : edu.robotics.dibujot.serial.SerialPort {
            override fun connect(baudRate: Int) = throw RuntimeException("USB open failed")
            override fun send(line: String) {}
            override fun readLine(): String? = null
            override fun disconnect() {}
        }
        val vm = SendViewModel(testDispatcher = StandardTestDispatcher(testScheduler))
        vm.connect(fake)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is UiState.Error)
        assertTrue((state as UiState.Error).message.isNotEmpty())
    }

    // --- startSend success ---

    @Test
    fun `startSend sends all lines and transitions to Done`() = runTest {
        val lines = listOf("G0 X0", "G1 X10")
        val fake = FakeSerialPort(listOf("ok\n", "ok\n"))
        fake.connect()
        val vm = SendViewModel(testDispatcher = StandardTestDispatcher(testScheduler))
        // Inject connected state
        vm.connectWithPort(fake)
        vm.startSend(lines)
        advanceUntilIdle()
        assertEquals(UiState.Done, vm.uiState.value)
    }

    // --- Sending state progress ---

    @Test
    fun `startSend emits Sending states before Done`() = runTest {
        val lines = listOf("A", "B")
        val fake = FakeSerialPort(listOf("ok\n", "ok\n"))
        fake.connect()
        val vm = SendViewModel(testDispatcher = StandardTestDispatcher(testScheduler))
        vm.connectWithPort(fake)
        vm.startSend(lines)
        advanceUntilIdle()
        // Final state must be Done after 2 successful ACKs
        assertEquals(UiState.Done, vm.uiState.value)
    }

    // --- startSend timeout → Error ---

    @Test
    fun `startSend with no ack transitions to Error`() = runTest {
        val lines = listOf("G0 X0")
        val fake = FakeSerialPort(emptyList())
        fake.connect()
        val vm = SendViewModel(
            testDispatcher = StandardTestDispatcher(testScheduler),
            ackTimeoutMs = 50
        )
        vm.connectWithPort(fake)
        vm.startSend(lines)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is UiState.Error)
        assertTrue((state as UiState.Error).message.isNotEmpty())
    }
}
