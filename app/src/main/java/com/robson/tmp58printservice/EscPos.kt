package com.robson.tmp58printservice

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

object EscPos {
    private const val ESC = 0x1B
    private const val GS = 0x1D
    private const val CP860_PAGE = 3
    private val cp860: Charset = Charset.forName("CP860")

    fun initialize(): ByteArray = byteArrayOf(
        ESC.toByte(),
        0x40,
        ESC.toByte(),
        0x61,
        0x01
    )

    fun finishDocument(): ByteArray = byteArrayOf(
        ESC.toByte(),
        0x61,
        0x00,
        0x0A,
        0x0A,
        0x0A
    )

    fun testPage(printerName: String): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(initialize())
        output.write(
            byteArrayOf(
                ESC.toByte(),
                0x74,
                CP860_PAGE.toByte(),
                ESC.toByte(),
                0x45,
                0x01
            )
        )
        output.write("TESTE DE IMPRESSÃO\n".toByteArray(cp860))
        output.write(byteArrayOf(ESC.toByte(), 0x45, 0x00))
        output.write("\n$printerName\n".toByteArray(cp860))
        output.write("Android Print Service\n\n".toByteArray(cp860))
        output.write(byteArrayOf(ESC.toByte(), 0x61, 0x00))
        output.write("Bluetooth: OK\nESC/POS: OK\n".toByteArray(cp860))
        output.write(finishDocument())
        return output.toByteArray()
    }

    fun rasterImage(
        width: Int,
        height: Int,
        pixels: IntArray,
        threshold: Int = PrinterPreferences.DEFAULT_THRESHOLD
    ): ByteArray {
        require(width > 0) { "A largura deve ser positiva" }
        require(height > 0) { "A altura deve ser positiva" }
        require(height <= 0xFFFF) { "A imagem excede a altura suportada pelo ESC/POS" }
        require(pixels.size == width * height) { "Quantidade de pixels inválida" }
        require(threshold in 0..255) { "O limiar deve estar entre 0 e 255" }

        val widthBytes = (width + 7) / 8
        require(widthBytes <= 0xFFFF) { "A imagem excede a largura suportada pelo ESC/POS" }

        val luminance = FloatArray(pixels.size) { index ->
            val color = pixels[index]
            val alpha = color ushr 24 and 0xFF
            val red = color ushr 16 and 0xFF
            val green = color ushr 8 and 0xFF
            val blue = color and 0xFF
            val gray = (red * 299 + green * 587 + blue * 114) / 1000f
            255f - alpha / 255f * (255f - gray)
        }
        val raster = ByteArray(widthBytes * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val oldValue = luminance[index]
                val newValue = if (oldValue < threshold) 0f else 255f

                if (newValue == 0f) {
                    val byteIndex = y * widthBytes + x / 8
                    raster[byteIndex] = (
                        raster[byteIndex].toInt() or (0x80 ushr (x % 8))
                    ).toByte()
                }

                val error = oldValue - newValue
                distributeError(luminance, width, height, x + 1, y, error * 7f / 16f)
                distributeError(luminance, width, height, x - 1, y + 1, error * 3f / 16f)
                distributeError(luminance, width, height, x, y + 1, error * 5f / 16f)
                distributeError(luminance, width, height, x + 1, y + 1, error / 16f)
            }
        }

        return byteArrayOf(
            GS.toByte(),
            0x76,
            0x30,
            0x00,
            (widthBytes and 0xFF).toByte(),
            (widthBytes ushr 8 and 0xFF).toByte(),
            (height and 0xFF).toByte(),
            (height ushr 8 and 0xFF).toByte()
        ) + raster
    }

    private fun distributeError(
        luminance: FloatArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
        error: Float
    ) {
        if (x !in 0 until width || y !in 0 until height) return
        val index = y * width + x
        luminance[index] = (luminance[index] + error).coerceIn(0f, 255f)
    }
}
