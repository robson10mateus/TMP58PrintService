package com.robson.tmp58printservice

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST = 100
    }

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
            text = getString(R.string.configure_paper)
            setOnClickListener { configurePaper() }
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
                configurePaper()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun configurePaper() {
        val configuration = PrinterPreferences.load(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.select_paper_title)
            .setItems(
                arrayOf(
                    getString(R.string.paper_profile_58),
                    getString(R.string.paper_profile_80),
                    getString(R.string.paper_profile_custom)
                )
            ) { _, position ->
                when (position) {
                    0 -> savePaperProfile(configuration, PaperProfile.PAPER_58_MM)
                    1 -> savePaperProfile(configuration, PaperProfile.PAPER_80_MM)
                    else -> showCustomProfileDialog(configuration)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCustomProfileDialog(configuration: PrinterConfiguration) {
        val current = configuration.paperProfile
        val paperWidth = numericField(R.string.paper_width_mm, current.paperWidthMm)
        val widthDots = numericField(R.string.printable_width_dots, current.widthDots)
        val dpi = numericField(R.string.printer_dpi, current.dpi)
        val threshold = numericField(R.string.print_threshold, current.threshold)
        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
            addView(paperWidth)
            addView(widthDots)
            addView(dpi)
            addView(threshold)
        }
        val scrollView = ScrollView(this).apply { addView(fields) }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.custom_paper_title)
            .setView(scrollView)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                try {
                    val profile = PaperProfile(
                        type = PaperProfileType.CUSTOM,
                        paperWidthMm = paperWidth.requiredInt(),
                        widthDots = widthDots.requiredInt(),
                        dpi = dpi.requiredInt(),
                        threshold = threshold.requiredInt()
                    )
                    savePaperProfile(configuration, profile)
                    dialog.dismiss()
                } catch (error: IllegalArgumentException) {
                    Toast.makeText(
                        this,
                        error.message ?: getString(R.string.invalid_configuration),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        dialog.show()
    }

    private fun numericField(label: Int, value: Int): EditText {
        return EditText(this).apply {
            hint = getString(label)
            contentDescription = getString(label)
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(getString(R.string.integer_value, value))
            selectAll()
        }
    }

    private fun EditText.requiredInt(): Int {
        return text.toString().toIntOrNull()
            ?: throw IllegalArgumentException(getString(R.string.fill_all_fields))
    }

    private fun savePaperProfile(
        configuration: PrinterConfiguration,
        profile: PaperProfile
    ) {
        PrinterPreferences.saveProfile(this, configuration.address, profile)
        updateStatus()
        Toast.makeText(this, R.string.paper_configuration_saved, Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus() {
        val configuration = PrinterPreferences.load(this)
        val available = BluetoothPrinter.isConfiguredDeviceAvailable(this, configuration)
        val statusLabel = getString(
            if (available) R.string.printer_available else R.string.printer_unavailable
        )
        val profile = configuration.paperProfile
        val widthLabel = resources.getQuantityString(
            R.plurals.printer_width_dots,
            profile.widthDots,
            profile.widthDots
        )
        val paperLabel = getString(
            R.string.paper_configuration,
            profile.paperWidthMm,
            widthLabel,
            profile.dpi
        )
        status.text = getString(
            R.string.printer_status,
            configuration.name,
            paperLabel,
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
