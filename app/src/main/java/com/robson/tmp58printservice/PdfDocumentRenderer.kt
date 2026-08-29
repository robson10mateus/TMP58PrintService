package com.robson.tmp58printservice

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import java.util.concurrent.CancellationException
import kotlin.math.roundToInt

class PdfDocumentRenderer(
    private val widthDots: Int,
    private val threshold: Int
) {
    companion object {
        private const val MAX_PAGE_HEIGHT_DOTS = 12_000
    }

    fun render(
        descriptor: ParcelFileDescriptor,
        isCancelled: () -> Boolean,
        onPageReady: (pageNumber: Int, data: ByteArray) -> Unit
    ): Int {
        require(widthDots in 1..2048) { "Largura da impressora inválida" }

        val renderer = PdfRenderer(descriptor)
        try {
            if (renderer.pageCount == 0) {
                throw IllegalArgumentException("O documento não contém páginas")
            }

            for (pageIndex in 0 until renderer.pageCount) {
                ensureNotCancelled(isCancelled)
                val page = renderer.openPage(pageIndex)
                try {
                    val height = (
                        page.height.toDouble() * widthDots / page.width.toDouble()
                    ).roundToInt().coerceIn(1, MAX_PAGE_HEIGHT_DOTS)
                    val bitmap = createBitmap(
                        widthDots,
                        height,
                        Bitmap.Config.ARGB_8888
                    )

                    try {
                        bitmap.eraseColor(Color.WHITE)
                        page.render(
                            bitmap,
                            null,
                            Matrix().apply {
                                setScale(
                                    widthDots.toFloat() / page.width,
                                    height.toFloat() / page.height
                                )
                            },
                            PdfRenderer.Page.RENDER_MODE_FOR_PRINT
                        )
                        ensureNotCancelled(isCancelled)

                        val pixels = IntArray(widthDots * height)
                        bitmap.getPixels(
                            pixels,
                            0,
                            widthDots,
                            0,
                            0,
                            widthDots,
                            height
                        )
                        onPageReady(
                            pageIndex + 1,
                            EscPos.rasterImage(widthDots, height, pixels, threshold)
                        )
                    } finally {
                        bitmap.recycle()
                    }
                } finally {
                    page.close()
                }
            }

            return renderer.pageCount
        } finally {
            renderer.close()
        }
    }

    private fun ensureNotCancelled(isCancelled: () -> Boolean) {
        if (isCancelled() || Thread.currentThread().isInterrupted) {
            throw CancellationException("Impressão cancelada")
        }
    }
}
