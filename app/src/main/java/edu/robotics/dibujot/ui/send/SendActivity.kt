package edu.robotics.dibujot.ui.send

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import edu.robotics.dibujot.R
import edu.robotics.dibujot.data.DrawingItem
import edu.robotics.dibujot.gcode.GcodeParser
import edu.robotics.dibujot.serial.UsbSerialPort
import kotlinx.coroutines.launch

class SendActivity : AppCompatActivity() {

    private lateinit var tvDrawingName: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSend: Button
    private lateinit var ivPreview: android.widget.ImageView

    private val viewModel: SendViewModel by lazy {
        ViewModelProvider(this)[SendViewModel::class.java]
    }

    private var drawingItem: DrawingItem? = null

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (granted && device != null) {
                    connectToDevice(device)
                } else {
                    Toast.makeText(this@SendActivity, getString(R.string.error_usb_permission_denied), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        setContentView(R.layout.activity_send)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        tvDrawingName = findViewById(R.id.tv_drawing_name)
        tvStatus = findViewById(R.id.tv_status)
        progressBar = findViewById(R.id.progress_bar)
        btnSend = findViewById(R.id.btn_send)
        ivPreview = findViewById(R.id.iv_drawing_preview)

        drawingItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DRAWING_ITEM, DrawingItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_DRAWING_ITEM)
        }
        tvDrawingName.text = drawingItem?.name ?: ""
        drawingItem?.imageResId?.let { ivPreview.setImageResource(it) }

        btnSend.setOnClickListener { requestUsbPermissionOrConnect() }

        // Register USB permission receiver
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbPermissionReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usbPermissionReceiver, filter)
        }

        observeViewModel()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbPermissionReceiver)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                renderState(state)
            }
        }
    }

    private fun renderState(state: UiState) {
        when (state) {
            UiState.Idle -> {
                tvStatus.text = getString(R.string.status_idle)
                progressBar.visibility = View.GONE
                btnSend.isEnabled = true
            }
            UiState.Connecting -> {
                tvStatus.text = getString(R.string.status_connecting)
                progressBar.visibility = View.GONE
                btnSend.isEnabled = false
            }
            UiState.Ready -> {
                tvStatus.text = getString(R.string.status_ready)
                progressBar.visibility = View.GONE
                btnSend.isEnabled = true
                // Auto-start sending when port is ready
                sendGcode()
            }
            is UiState.Sending -> {
                val pct = if (state.total > 0) (state.linesSent * 100) / state.total else 0
                tvStatus.text = "${state.linesSent}/${state.total}"
                progressBar.progress = pct
                progressBar.visibility = View.VISIBLE
                btnSend.isEnabled = false
            }
            UiState.Done -> {
                tvStatus.text = getString(R.string.status_done)
                progressBar.visibility = View.GONE
                btnSend.isEnabled = false
            }
            is UiState.Error -> {
                tvStatus.text = state.message
                progressBar.visibility = View.GONE
                btnSend.isEnabled = true
            }
        }
    }

    private fun requestUsbPermissionOrConnect() {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val device = usbManager.deviceList.values.firstOrNull()
        if (device == null) {
            Toast.makeText(this, getString(R.string.error_no_usb_device), Toast.LENGTH_SHORT).show()
            return
        }
        if (usbManager.hasPermission(device)) {
            connectToDevice(device)
        } else {
            val intent = PendingIntent.getBroadcast(
                this, 0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device, intent)
        }
    }

    private fun connectToDevice(@Suppress("UNUSED_PARAMETER") device: UsbDevice) {
        val port = UsbSerialPort(applicationContext)
        viewModel.connect(port)
    }

    private fun sendGcode() {
        val item = drawingItem ?: return
        val rawLines = try {
            assets.open(item.gcodeAssetPath).bufferedReader().readLines()
        } catch (e: Exception) {
            viewModel.startSend(emptyList())
            return
        }
        val parsed = GcodeParser().parse(rawLines)
        viewModel.startSend(parsed)
    }

    companion object {
        const val EXTRA_DRAWING_ITEM = "extra_drawing_item"
        private const val ACTION_USB_PERMISSION = "edu.robotics.dibujot.USB_PERMISSION"
    }
}
