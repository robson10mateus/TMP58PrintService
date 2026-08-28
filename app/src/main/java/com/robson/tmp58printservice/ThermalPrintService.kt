package com.robson.tmp58printservice

import android.printservice.PrintJob
import android.printservice.PrintService
import android.printservice.PrinterDiscoverySession
import android.util.Log

class ThermalPrintService : PrintService() {

    companion object {
        private const val TAG = "TMP58PrintService"
    }

    override fun onCreatePrinterDiscoverySession(): PrinterDiscoverySession {
        Log.d(TAG, "Criando sessão de descoberta")

        return ThermalPrinterDiscoverySession(this)
    }

    override fun onPrintJobQueued(
        printJob: PrintJob
    ) {

        Log.d(
            TAG,
            "Trabalho recebido: ${printJob.info.label}"
        )

        Thread {

            val printer =
                BluetoothPrinter(this)

            try {

                printJob.start()

                Log.d(
                    TAG,
                    "Conectando na IMP-TMP58ABT..."
                )

                val conectado =
                    printer.conectarPorNome(
                        "IMP-TMP58ABT"
                    )

                if (!conectado) {

                    printJob.fail(
                        "Não foi possível conectar à impressora Bluetooth"
                    )

                    return@Thread
                }

                Log.d(
                    TAG,
                    "Enviando impressão teste..."
                )

                val sucesso =
                    printer.enviar(
                        EscPos.teste()
                    )

                if (sucesso) {

                    Log.d(
                        TAG,
                        "Impressão realizada"
                    )

                    printJob.complete()

                } else {

                    printJob.fail(
                        "Erro enviando dados para a impressora"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Erro durante impressão",
                    e
                )

                printJob.fail(
                    e.message ?: "Erro desconhecido"
                )

            } finally {

                printer.desconectar()
            }

        }.start()
    }

    override fun onRequestCancelPrintJob(printJob: PrintJob) {
        Log.d(TAG, "Cancelando impressão")

        printJob.cancel()
    }
}