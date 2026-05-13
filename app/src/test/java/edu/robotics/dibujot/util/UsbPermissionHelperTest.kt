package edu.robotics.dibujot.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UsbPermissionHelperTest {

    // Test the ACTION_USB_PERMISSION constant value
    @Test
    fun `action string matches expected permission action`() {
        assertEquals(
            "edu.robotics.dibujot.USB_PERMISSION",
            UsbPermissionHelper.ACTION_USB_PERMISSION
        )
    }

    // Test: extractGranted returns true when EXTRA_PERMISSION_GRANTED is true
    @Test
    fun `extractGranted returns true when permission granted`() {
        val intent = Intent(UsbPermissionHelper.ACTION_USB_PERMISSION).apply {
            putExtra(UsbManager.EXTRA_PERMISSION_GRANTED, true)
        }
        assertTrue(UsbPermissionHelper.extractGranted(intent))
    }

    // Triangulation: extractGranted returns false when denied
    @Test
    fun `extractGranted returns false when permission denied`() {
        val intent = Intent(UsbPermissionHelper.ACTION_USB_PERMISSION).apply {
            putExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
        }
        assertFalse(UsbPermissionHelper.extractGranted(intent))
    }

    // Edge case: extractGranted defaults to false if extra absent
    @Test
    fun `extractGranted defaults to false when extra absent`() {
        val intent = Intent(UsbPermissionHelper.ACTION_USB_PERMISSION)
        assertFalse(UsbPermissionHelper.extractGranted(intent))
    }

    // Test: requestPermission calls UsbManager.requestPermission with correct action
    @Test
    fun `requestPermission delegates to UsbManager with correct PendingIntent action`() {
        val context = RuntimeEnvironment.getApplication()
        val usbManager = mock(UsbManager::class.java)
        val device = mock(UsbDevice::class.java)

        val helper = UsbPermissionHelper(context, usbManager)
        helper.requestPermission(device) { /* callback not invoked synchronously */ }

        verify(usbManager).requestPermission(
            org.mockito.ArgumentMatchers.eq(device),
            org.mockito.ArgumentMatchers.any(PendingIntent::class.java)
        )
    }
}
