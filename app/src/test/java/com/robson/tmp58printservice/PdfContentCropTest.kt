package com.robson.tmp58printservice

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfContentCropTest {
    private val renderer = PdfDocumentRenderer(widthDots = 4, threshold = 160)

    @Test
    fun findContentHeight_removesAllWhiteRowsAfterLastInkRow() {
        val pixels = IntArray(4 * 6) { WHITE }
        pixels[2 * 4 + 1] = BLACK

        assertEquals(3, renderer.findContentHeight(pixels, 4, 6, 160))
    }

    @Test
    fun findContentHeight_keepsOneRowForBlankPage() {
        val pixels = IntArray(4 * 6) { WHITE }

        assertEquals(1, renderer.findContentHeight(pixels, 4, 6, 160))
    }

    @Test
    fun findContentHeight_treatsTransparentPixelsAsWhitePaper() {
        val pixels = IntArray(4 * 6) { TRANSPARENT_BLACK }

        assertEquals(1, renderer.findContentHeight(pixels, 4, 6, 160))
    }

    private companion object {
        const val BLACK = -0x1000000
        const val WHITE = -0x1
        const val TRANSPARENT_BLACK = 0x00000000
    }
}
