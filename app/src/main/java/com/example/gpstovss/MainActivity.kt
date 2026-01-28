package com.example.gpstovss

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.*
import java.io.OutputStream
import java.util.UUID
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    /* ========= UI ========= */
    private lateinit var statusText: TextView
    private lateinit var speedText: TextView
    private lateinit var btnPick: Button
    private lateinit var btnConnect: Button

    /* ========= GPS ========= */
    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    /* ========= Bluetooth ========= */
    private val btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var selectedDevice: BluetoothDevice? = null
    private var socket: BluetoothSocket? = null
    private var out: OutputStream? = null

    // Standard Bluetooth Serial Port Profile UUID
    private val SPP_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /* ========= Permissions ========= */
    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            if (perms.values.all { it }) startGps()
            else status("Permissions denied")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep your edge-to-edge boilerplate
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // UI refs
        statusText = findViewById(R.id.statusText)
        speedText = findViewById(R.id.speedText)
        btnPick = findViewById(R.id.btnPick)
        btnConnect = findViewById(R.id.btnConnect)
        btnConnect.text = "Connect"

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        btnPick.setOnClickListener { pickBluetoothDevice() }
        btnConnect.setOnClickListener { connectBluetooth() }

        requestPermissions()
    }

    /* ========= Permissions ========= */
    private fun requestPermissions() {
        val perms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT
        )

        val missing = perms.any { perm ->
            ActivityCompat.checkSelfPermission(
                this,
                perm
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (missing) {
            permissionLauncher.launch(perms)
        } else {
            startGps()
        }
    }

    /* ========= GPS ========= */
    private fun startGps() {
        status("GPS running")

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            100L
        )
            .setMinUpdateIntervalMillis(100L)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return

                val mphRaw = max(0f, loc.speed * 2.23694f)

                // Deadband: ignore GPS noise below ~0.5 mph
                val mph = if (mphRaw < 0.5f) 0f else mphRaw

                speedText.text = "Speed: %.2f mph".format(mph)
                try {
                    out?.write("SPEED_MPH:%.2f\r\n".format(mph).toByteArray())
                } catch (e: Exception) {
                    status("BT write failed: ${e.message}")
                    disconnectBluetooth()
                }
           }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedClient.requestLocationUpdates(
            request,
            locationCallback!!,
            mainLooper
        )
    }

    /* ========= Bluetooth ========= */

    @Volatile private var isConnecting = false
    @Volatile private var isConnected = false

    private fun pickBluetoothDevice() {
        val devices = btAdapter?.bondedDevices?.toList() ?: return

        if (devices.isEmpty()) {
            status("No paired devices")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Pick Bluetooth device")
            .setItems(devices.map { it.name }.toTypedArray()) { _, idx ->
                selectedDevice = devices[idx]
                btnConnect.isEnabled = true
                status("Selected ${selectedDevice?.name}")
            }
            .show()
    }

    private fun connectBluetooth() {
        val device = selectedDevice ?: return

        // Prevent double-tap / re-entry
        if (isConnecting) return

        // If already connected, cleanly disconnect first (acts like a toggle)
        if (isConnected) {
            disconnectBluetooth()
            return
        }

        isConnecting = true
        runOnUiThread {
            btnConnect.isEnabled = false
            status("Connecting to ${device.name}…")
        }

        Thread {
            try {
                // Always nuke any stale socket before a new connect attempt
                disconnectBluetooth()

                val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
                s.connect()

                socket = s
                out = s.outputStream
                isConnected = true
                isConnecting = false

                runOnUiThread {
                    status("Bluetooth connected")
                    btnConnect.isEnabled = true
                    btnConnect.text = "Disconnect"
                }
            } catch (e: Exception) {
                isConnected = false
                isConnecting = false
                runOnUiThread {
                    status("BT failed: ${e.message}")
                    btnConnect.isEnabled = true
                    btnConnect.text = "Connect"
                }
                disconnectBluetooth()
            }
        }.start()
    }


    private fun disconnectBluetooth() {
        try { out?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        out = null
        socket = null
        isConnected = false
        isConnecting = false

        runOnUiThread {
            btnConnect.isEnabled = (selectedDevice != null)
            status("Bluetooth disconnected")
        }
    }


    private fun status(msg: String) {
        statusText.text = "Status: $msg"
    }
}
