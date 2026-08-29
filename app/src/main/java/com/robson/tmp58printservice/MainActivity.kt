package com.robson.tmp58printservice

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

class MainActivity : Activity() {
    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST = 100
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContentView())
        requestBluetoothPermission()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST) updateStatus()
    }

    private fun createContentView(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
        }

        layout.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
        })
        layout.addView(TextView(this).apply {
            text = getString(R.string.app_description)
        })

        status = TextView(this)
        layout.addView(status)

        layout.addView(Button(this).apply {
            text = getString(R.string.select_printer)
            setOnClickListener { selectPrinter() }
        })
        layout.addView(Button(this).apply {
            text = getString(R.string.print_test)
            setOnClickListener { printTest() }
        })
        layout.addView(Button(this).apply {
            text = getString(R.string.print_settings)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_PRINT_SETTINGS))
            }
        })

        return layout
    }

    private fun selectPrinter() {
        if (!BluetoothPrinter.hasConnectPermission(this)) {
            requestBluetoothPermission()
            return
        }

        val devices = BluetoothPrinter.listPairedDevices(this)
        if (devices.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.no_paired_device),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val labels = devices.map {
            getString(R.string.paired_device_label, it.name, it.address)
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.select_printer_title)
            .setItems(labels) { _, position ->
                PrinterPreferences.saveDevice(this, devices[position])
                updateStatus()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun printTest() {
        if (!BluetoothPrinter.hasConnectPermission(this)) {
            requestBluetoothPermission()
            return
        }

        val configuration = PrinterPreferences.load(this)
        executor.execute {
            val printer = BluetoothPrinter(this)
            val success = try {
                printer.connect(configuration) &&
                    printer.send(EscPos.testPage(configuration.name))
            } catch (_: Exception) {
                false
            } finally {
                printer.disconnect()
            }

            runOnUiThread {
                Toast.makeText(
                    this,
                    if (success) R.string.test_sent else R.string.test_failed,
                    Toast.LENGTH_LONG
                ).show()
                updateStatus()
            }
        }
    }

    private fun updateStatus() {
        val configuration = PrinterPreferences.load(this)
        val available = BluetoothPrinter.isConfiguredDeviceAvailable(this, configuration)
        val statusLabel = getString(
            if (available) R.string.printer_available else R.string.printer_unavailable
        )
        val widthLabel = resources.getQuantityString(
            R.plurals.printer_width_dots,
            configuration.widthDots,
            configuration.widthDots
        )
        status.text = getString(
            R.string.printer_status,
            configuration.name,
            widthLabel,
            statusLabel
        )
    }

    private fun requestBluetoothPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                BLUETOOTH_PERMISSION_REQUEST
            )
        }
    }
}
