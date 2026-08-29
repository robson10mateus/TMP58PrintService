package com.robson.tmp58printservice

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class PdfDocumentRendererTest {
    @Test
    fun render_convertsPdfPageToEscPosRasterAtConfiguredWidth() {
        val file = createPdf()
        var pageNumber = 0
        var raster = ByteArray(0)

        try {
            val descriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val count = PdfDocumentRenderer(widthDots = 16, threshold = 160)
                .render(descriptor, isCancelled = { false }) { page, data ->
                    pageNumber = page
                    raster = data
                }

            assertEquals(1, count)
            assertEquals(1, pageNumber)
            assertEquals(0x1D, raster[0].toInt())
            assertEquals(2, raster[4].toInt())
            assertEquals(8, raster[6].toInt())
            assertTrue(raster.drop(8).any { it.toInt() != 0 })
            assertTrue(raster.drop(8).any { it.toInt() != -1 })
        } finally {
            file.delete()
        }
    }

    private fun createPdf(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "renderer-test.pdf")
        val document = PdfDocument()
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(200, 100, 1).create()
        )
        page.canvas.drawColor(Color.WHITE)
        page.canvas.drawRect(
            0f,
            0f,
            100f,
            100f,
            Paint().apply { color = Color.BLACK }
        )
        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }
}
