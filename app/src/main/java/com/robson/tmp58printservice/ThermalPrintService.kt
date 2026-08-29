package com.robson.tmp58printservice

import android.print.PrintJobId
import android.printservice.PrintJob
import android.printservice.PrintService
import android.printservice.PrinterDiscoverySession
import android.util.Log
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

class ThermalPrintService : PrintService() {
    companion object {
        private const val TAG = "TMP58PrintService"
    }

    private class ActiveJob {
        val cancelled = AtomicBoolean(false)

        @Volatile
        var printer: BluetoothPrinter? = null

        @Volatile
        var future: Future<*>? = null
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val activeJobs = ConcurrentHashMap<PrintJobId, ActiveJob>()

    override fun onCreatePrinterDiscoverySession(): PrinterDiscoverySession {
        Log.d(TAG, "Criando sessão de descoberta")
        return ThermalPrinterDiscoverySession(this)
    }

    override fun onPrintJobQueued(printJob: PrintJob) {
        Log.d(TAG, "Trabalho recebido: ${printJob.info.label}")

        val activeJob = ActiveJob()
        activeJobs[printJob.id] = activeJob
        activeJob.future = executor.submit {
            processPrintJob(printJob, activeJob)
        }
    }

    override fun onRequestCancelPrintJob(printJob: PrintJob) {
        Log.d(TAG, "Cancelando trabalho")
        activeJobs[printJob.id]?.let { activeJob ->
            activeJob.cancelled.set(true)
            activeJob.printer?.disconnect()
            activeJob.future?.cancel(true)
            activeJobs.remove(printJob.id, activeJob)
        }
        cancelSafely(printJob)
    }

    override fun onDestroy() {
        activeJobs.values.forEach { activeJob ->
            activeJob.cancelled.set(true)
            activeJob.printer?.disconnect()
            activeJob.future?.cancel(true)
        }
        activeJobs.clear()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun processPrintJob(printJob: PrintJob, activeJob: ActiveJob) {
        val printer = BluetoothPrinter(this)
        activeJob.printer = printer

        try {
            ensureNotCancelled(activeJob)
            if (!printJob.start()) return

            val configuration = PrinterPreferences.load(this)
            if (!printer.connect(configuration)) {
                throw IOException(
                    "Não foi possível conectar à impressora ${configuration.name}"
                )
            }

            ensureSent(printer, EscPos.initialize(), activeJob)
            val descriptor = printJob.document?.data
                ?: throw IOException("O trabalho não contém um documento legível")

            val renderer = PdfDocumentRenderer(
                widthDots = configuration.widthDots,
                threshold = configuration.threshold
            )
            renderer.render(
                descriptor = descriptor,
                isCancelled = { activeJob.cancelled.get() }
            ) { pageNumber, data ->
                Log.d(TAG, "Enviando página $pageNumber")
                ensureSent(printer, data, activeJob)
                ensureSent(printer, byteArrayOf(0x0A), activeJob)
            }
            ensureSent(printer, EscPos.finishDocument(), activeJob)
            ensureNotCancelled(activeJob)
            printJob.complete()
            Log.d(TAG, "Impressão concluída")
        } catch (_: CancellationException) {
            cancelSafely(printJob)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            cancelSafely(printJob)
        } catch (error: Exception) {
            Log.e(TAG, "Erro durante impressão", error)
            if (activeJob.cancelled.get()) {
                cancelSafely(printJob)
            } else {
                printJob.fail(error.message ?: "Erro desconhecido durante a impressão")
            }
        } finally {
            printer.disconnect()
            activeJob.printer = null
            activeJobs.remove(printJob.id, activeJob)
        }
    }

    private fun ensureSent(
        printer: BluetoothPrinter,
        data: ByteArray,
        activeJob: ActiveJob
    ) {
        ensureNotCancelled(activeJob)
        if (!printer.send(data) { activeJob.cancelled.get() }) {
            throw IOException("Falha ao enviar dados para a impressora")
        }
    }

    private fun ensureNotCancelled(activeJob: ActiveJob) {
        if (activeJob.cancelled.get() || Thread.currentThread().isInterrupted) {
            throw CancellationException("Impressão cancelada")
        }
    }

    private fun cancelSafely(printJob: PrintJob) {
        if (!printJob.isCancelled && !printJob.isCompleted && !printJob.isFailed) {
            printJob.cancel()
        }
    }
}
