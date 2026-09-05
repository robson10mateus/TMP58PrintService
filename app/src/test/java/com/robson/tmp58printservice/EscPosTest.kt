package com.robson.tmp58printservice

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class EscPosTest {
    @Test
    fun initialize_resetsPrinterAndCentersContent() {
        assertArrayEquals(
            byteArrayOf(0x1B, 0x40, 0x1B, 0x61, 0x01),
            EscPos.initialize()
        )
    }

    @Test
    fun rasterImage_packsBlackAndWhitePixelsIntoEscPosBits() {
        val pixels = intArrayOf(
            BLACK,
            WHITE,
            WHITE,
            WHITE,
            WHITE,
            WHITE,
            WHITE,
            WHITE
        )

        val result = EscPos.rasterImage(8, 1, pixels)

        assertArrayEquals(
            byteArrayOf(
                0x1D,
                0x76,
                0x30,
                0x00,
                0x01,
                0x00,
                0x01,
                0x00,
                0x80.toByte()
            ),
            result
        )
    }

    @Test
    fun rasterImage_padsWidthsThatAreNotMultiplesOfEight() {
        val pixels = IntArray(9) { WHITE }.also { it[8] = BLACK }

        val result = EscPos.rasterImage(9, 1, pixels)

        assertEquals(10, result.size)
        assertEquals(2, result[4].toInt())
        assertEquals(0, result[8].toInt())
        assertEquals(0x80.toByte(), result[9])
    }

    @Test
    fun rasterImage_encodesConfigured80mmWidthInHeader() {
        val result = EscPos.rasterImage(576, 1, IntArray(576) { WHITE })

        assertEquals(72, result[4].toInt())
        assertEquals(0, result[5].toInt())
    }

    @Test
    fun rasterImage_compositesTransparentPixelsOverWhitePaper() {
        val transparentBlack = 0x00000000

        val result = EscPos.rasterImage(8, 1, IntArray(8) { transparentBlack })

        assertEquals(0, result.last().toInt())
    }

    @Test
    fun testPage_encodesAccentsUsingCp860InsteadOfUtf8() {
        val result = EscPos.testPage("IMPRESSÃO")
        val cp860Text = "IMPRESSÃO".toByteArray(Charset.forName("CP860"))
        val utf8Text = "IMPRESSÃO".toByteArray(Charsets.UTF_8)

        assertTrue(result.containsSequence(cp860Text))
        assertFalse(result.containsSequence(utf8Text))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rasterImage_rejectsAnInvalidPixelCount() {
        EscPos.rasterImage(8, 2, IntArray(8))
    }

    private fun ByteArray.containsSequence(sequence: ByteArray): Boolean {
        if (sequence.isEmpty() || sequence.size > size) return false
        return (0..size - sequence.size).any { offset ->
            sequence.indices.all { index -> this[offset + index] == sequence[index] }
        }
    }

    private companion object {
        const val BLACK = -0x1000000
        const val WHITE = -0x1
    }
}
