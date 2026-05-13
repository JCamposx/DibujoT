package com.example.dibujot.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build

class UsbPermissionHelper(
    private val context: Context,
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
) {

    companion object {
        const val ACTION_USB_PERMISSION = "com.example.dibujot.USB_PERMISSION"

        /** Extract granted flag from a USB permission result Intent. Pure function — easy to unit test. */
        fun extractGranted(intent: Intent): Boolean =
            intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
    }

    fun requestPermission(device: UsbDevice, callback: (granted: Boolean) -> Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (ACTION_USB_PERMISSION == intent.action) {
                    context.unregisterReceiver(this)
                    callback(extractGranted(intent))
                }
            }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            flags
        )

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        usbManager.requestPermission(device, pendingIntent)
    }
}
