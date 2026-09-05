package com.robson.tmp58printservice

import android.print.PrintAttributes
import android.print.PrinterCapabilitiesInfo
import android.print.PrinterId
import android.print.PrinterInfo
import android.printservice.PrinterDiscoverySession
import android.util.Log
import kotlin.math.roundToInt

class ThermalPrinterDiscoverySession(
    private val printService: ThermalPrintService
) : PrinterDiscoverySession() {
    companion object {
        private const val TAG = "EscPosDiscovery"
        // Mantido para que atualizações não criem uma segunda impressora no Android.
        private const val PRINTER_ID = "THERMAL_BLUETOOTH_58MM"
        private const val VIRTUAL_PAGE_HEIGHT_MM = 200
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

        val profile = configuration.paperProfile
        val configuredPaper = PrintAttributes.MediaSize(
            "THERMAL_${profile.paperWidthMm}MM_${profile.widthDots}DOTS",
            "Papel térmico ${profile.paperWidthMm} mm",
            millimetersToMils(profile.paperWidthMm),
            millimetersToMils(VIRTUAL_PAGE_HEIGHT_MM)
        )
        val capabilities = PrinterCapabilitiesInfo.Builder(printerId)
            .addMediaSize(configuredPaper, true)
            .addResolution(
                PrintAttributes.Resolution(
                    "${profile.dpi}_DPI",
                    "${profile.dpi} DPI",
                    profile.dpi,
                    profile.dpi
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

    private fun millimetersToMils(millimeters: Int): Int {
        return (millimeters * 1_000.0 / 25.4).roundToInt()
    }
}
