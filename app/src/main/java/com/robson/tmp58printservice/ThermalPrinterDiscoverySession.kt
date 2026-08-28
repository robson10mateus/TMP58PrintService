package com.robson.tmp58printservice

import android.content.Context
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
        private const val PRINTER_ID = "IMP_TMP58ABT"
    }

    override fun onStartPrinterDiscovery(
        priorityList: MutableList<PrinterId>
    ) {
        Log.d(TAG, "Iniciando descoberta")

        adicionarImpressora()
    }

    private fun adicionarImpressora() {

        val printerId =
            printService.generatePrinterId(PRINTER_ID)

        if (printerId == null) {
            Log.e(TAG, "Não foi possível gerar PrinterId")
            return
        }

        val paper58mm = PrintAttributes.MediaSize(
            "THERMAL_58MM",
            "Papel térmico 58 mm",
            2283,
            7874
        )

        val capabilities =
            PrinterCapabilitiesInfo.Builder(printerId)
                .addMediaSize(
                    paper58mm,
                    true
                )
                .addResolution(
                    PrintAttributes.Resolution(
                        "203_DPI",
                        "203 DPI",
                        203,
                        203
                    ),
                    true
                )
                .setMinMargins(
                    PrintAttributes.Margins(
                        0,
                        0,
                        0,
                        0
                    )
                )
                .setColorModes(
                    PrintAttributes.COLOR_MODE_MONOCHROME,
                    PrintAttributes.COLOR_MODE_MONOCHROME
                )
                .build()

        val printerInfo =
            PrinterInfo.Builder(
                printerId,
                "IMP-TMP58ABT",
                PrinterInfo.STATUS_IDLE
            )
                .setDescription(
                    "Impressora térmica Bluetooth 58 mm"
                )
                .setCapabilities(capabilities)
                .build()

        addPrinters(
            listOf(printerInfo)
        )

        Log.d(TAG, "IMP-TMP58ABT adicionada")
    }

    override fun onStopPrinterDiscovery() {
        Log.d(TAG, "Descoberta interrompida")
    }

    override fun onValidatePrinters(
        printerIds: MutableList<PrinterId>
    ) {
        adicionarImpressora()
    }

    override fun onStartPrinterStateTracking(
        printerId: PrinterId
    ) {
        Log.d(TAG, "Monitorando ${printerId.localId}")
    }

    override fun onStopPrinterStateTracking(
        printerId: PrinterId
    ) {
        Log.d(TAG, "Parando monitoramento ${printerId.localId}")
    }

    override fun onDestroy() {
        Log.d(TAG, "Sessão destruída")
    }
}