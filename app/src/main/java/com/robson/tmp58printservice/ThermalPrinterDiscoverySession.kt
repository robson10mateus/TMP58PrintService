package com.robson.tmp58printservice

import android.print.PrintAttributes
import android.print.PrinterCapabilitiesInfo
import android.print.PrinterId
import android.print.PrinterInfo
import android.printservice.PrinterDiscoverySession
import android.util.Log

class ThermalPrinterDiscoverySession(
    private val printService: ThermalPrintService
) : PrinterDiscoverySession() {
    companion object {
        private const val TAG = "TMP58Discovery"
        private const val PRINTER_ID = "THERMAL_BLUETOOTH_58MM"
    }

    override fun onStartPrinterDiscovery(priorityList: MutableList<PrinterId>) {
        Log.d(TAG, "Iniciando descoberta")
        addConfiguredPrinter()
    }

    override fun onStopPrinterDiscovery() {
        Log.d(TAG, "Descoberta interrompida")
    }

    override fun onValidatePrinters(printerIds: MutableList<PrinterId>) {
        addConfiguredPrinter()
    }

    override fun onStartPrinterStateTracking(printerId: PrinterId) {
        addConfiguredPrinter()
    }

    override fun onStopPrinterStateTracking(printerId: PrinterId) {
        Log.d(TAG, "Monitoramento interrompido")
    }

    override fun onDestroy() {
        Log.d(TAG, "Sessão destruída")
    }

    private fun addConfiguredPrinter() {
        val printerId = printService.generatePrinterId(PRINTER_ID)
        val configuration = PrinterPreferences.load(printService)
        val available = BluetoothPrinter.isConfiguredDeviceAvailable(
            printService,
            configuration
        )
        val status = if (available) {
            PrinterInfo.STATUS_IDLE
        } else {
            PrinterInfo.STATUS_UNAVAILABLE
        }

        val paper58mm = PrintAttributes.MediaSize(
            "THERMAL_58MM",
            "Papel térmico 58 mm",
            2283,
            7874
        )
        val capabilities = PrinterCapabilitiesInfo.Builder(printerId)
            .addMediaSize(paper58mm, true)
            .addResolution(
                PrintAttributes.Resolution(
                    "203_DPI",
                    "203 DPI",
                    203,
                    203
                ),
                true
            )
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .setColorModes(
                PrintAttributes.COLOR_MODE_MONOCHROME,
                PrintAttributes.COLOR_MODE_MONOCHROME
            )
            .build()
        val description = if (available) {
            "Impressora térmica Bluetooth pareada"
        } else {
            "Impressora indisponível; verifique o Bluetooth e o pareamento"
        }
        val printerInfo = PrinterInfo.Builder(
            printerId,
            configuration.name,
            status
        )
            .setDescription(description)
            .setCapabilities(capabilities)
            .build()

        addPrinters(listOf(printerInfo))
        Log.d(TAG, "Impressora configurada atualizada")
    }
}
