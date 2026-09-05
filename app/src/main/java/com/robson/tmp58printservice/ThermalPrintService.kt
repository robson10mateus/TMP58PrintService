package com.robson.tmp58printservice

import android.os.ParcelFileDescriptor
import android.os.Handler
import android.os.Looper
import android.print.PrintJobId
import android.printservice.PrintJob
import android.printservice.PrintService
import android.printservice.PrinterDiscoverySession
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

class ThermalPrintService : PrintService() {
    companion object {
        private const val TAG = "EscPosPrintService"
    }

    private class ActiveJob {
        val cancelled = AtomicBoolean(false)

        @Volatile
        var printer: BluetoothPrinter? = null

        @Volatile
        var future: Future<*>? = null
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeJobs = ConcurrentHashMap<PrintJobId, ActiveJob>()

    override fun onCreatePrinterDiscoverySession(): PrinterDiscoverySession {
        Log.d(TAG, "Criando sessao de descoberta")
        return ThermalPrinterDiscoverySession(this)
    }

    override fun onPrintJobQueued(printJob: PrintJob) {
        Log.d(TAG, "Trabalho recebido: ${printJob.info.label}")

        val jobId = printJob.id
        val activeJob = ActiveJob()
        activeJobs[jobId] = activeJob

        // PrintJob e PrintDocument so podem ser acessados pela thread principal.
        if (!printJob.start()) {
            activeJobs.remove(jobId, activeJob)
            Log.e(TAG, "Nao foi possivel iniciar o trabalho")
            return
        }

        val descriptor = try {
            printJob.document?.data
                ?: throw IOException("O trabalho nao contem um documento legivel")
        } catch (error: Exception) {
            activeJobs.remove(jobId, activeJob)
            failSafely(printJob, error.message ?: "Nao foi possivel abrir o documento")
            return
        }

        activeJob.future = executor.submit {
            processPrintJob(printJob, jobId, descriptor, activeJob)
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

    private fun processPrintJob(
        printJob: PrintJob,
        jobId: PrintJobId,
        descriptor: ParcelFileDescriptor,
        activeJob: ActiveJob
    ) {
        val printer = BluetoothPrinter(this)
        activeJob.printer = printer
        var temporaryPdf: File? = null

        try {
            ensureNotCancelled(activeJob)
            val configuration = PrinterPreferences.load(this)
            Log.d(TAG, "Conectando a ${configuration.name}")
            if (!printer.connect(configuration)) {
                throw IOException(
                    "Nao foi possivel conectar a impressora ${configuration.name}"
                )
            }

            ensureSent(printer, EscPos.initialize(), activeJob)
            val renderer = PdfDocumentRenderer(
                widthDots = configuration.widthDots,
                threshold = configuration.threshold
            )
            val seekablePdf = copyToSeekableFile(descriptor, activeJob)
            temporaryPdf = seekablePdf
            ParcelFileDescriptor.open(
                seekablePdf,
                ParcelFileDescriptor.MODE_READ_ONLY
            ).use {
                renderer.render(
                    descriptor = it,
                    isCancelled = { activeJob.cancelled.get() }
                ) { pageNumber, data ->
                    Log.d(TAG, "Enviando pagina $pageNumber")
                    ensureSent(printer, data, activeJob)
                }
            }
            ensureSent(printer, byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A, 0x0A), activeJob)
            ensureNotCancelled(activeJob)
            mainHandler.post {
                if (!activeJob.cancelled.get() && printJob.isStarted) {
                    printJob.complete()
                    Log.d(TAG, "Impressao concluida")
                }
            }
        } catch (_: CancellationException) {
            cancelOnMain(printJob)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            cancelOnMain(printJob)
        } catch (error: Exception) {
            Log.e(TAG, "Erro durante impressao", error)
            if (activeJob.cancelled.get()) {
                cancelOnMain(printJob)
            } else {
                failOnMain(
                    printJob,
                    error.message ?: "Erro desconhecido durante a impressao"
                )
            }
        } finally {
            try {
                descriptor.close()
            } catch (_: IOException) {
                // O descritor pode ter sido fechado pelo bloco use.
            }
            if (temporaryPdf?.delete() == false) {
                Log.w(TAG, "Nao foi possivel apagar o PDF temporario")
            }
            printer.disconnect()
            activeJob.printer = null
            activeJobs.remove(jobId, activeJob)
        }
    }

    private fun copyToSeekableFile(
        descriptor: ParcelFileDescriptor,
        activeJob: ActiveJob
    ): File {
        val temporaryPdf = File.createTempFile("print_job_", ".pdf", cacheDir)
        try {
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                temporaryPdf.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        ensureNotCancelled(activeJob)
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                }
            }
            Log.d(TAG, "PDF temporario preparado: ${temporaryPdf.length()} bytes")
            return temporaryPdf
        } catch (error: Exception) {
            temporaryPdf.delete()
            throw error
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
            throw CancellationException("Impressao cancelada")
        }
    }

    private fun cancelOnMain(printJob: PrintJob) {
        mainHandler.post { cancelSafely(printJob) }
    }

    private fun failOnMain(printJob: PrintJob, message: String) {
        mainHandler.post { failSafely(printJob, message) }
    }

    private fun cancelSafely(printJob: PrintJob) {
        if (!printJob.isCancelled && !printJob.isCompleted && !printJob.isFailed) {
            printJob.cancel()
        }
    }

    private fun failSafely(printJob: PrintJob, message: String) {
        if (!printJob.isCancelled && !printJob.isCompleted && !printJob.isFailed) {
            printJob.fail(message)
        }
    }
}
