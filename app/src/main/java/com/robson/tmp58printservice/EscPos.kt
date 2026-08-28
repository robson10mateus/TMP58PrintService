package com.robson.tmp58printservice

import java.io.ByteArrayOutputStream

object EscPos {

    private const val ESC = 0x1B

    fun teste(): ByteArray {

        val out =
            ByteArrayOutputStream()

        // ESC @
        // Inicializa impressora
        out.write(
            byteArrayOf(
                ESC.toByte(),
                0x40
            )
        )

        // ESC a 1
        // Centralizado
        out.write(
            byteArrayOf(
                ESC.toByte(),
                0x61,
                0x01
            )
        )

        // ESC E 1
        // Negrito
        out.write(
            byteArrayOf(
                ESC.toByte(),
                0x45,
                0x01
            )
        )

        out.write(
            "TESTE DE IMPRESSAO\n"
                .toByteArray(Charsets.UTF_8)
        )

        // Negrito OFF
        out.write(
            byteArrayOf(
                ESC.toByte(),
                0x45,
                0x00
            )
        )

        out.write(
            "\n"
                .toByteArray()
        )

        out.write(
            "IMP-TMP58ABT\n"
                .toByteArray()
        )

        out.write(
            "Android Print Service\n"
                .toByteArray()
        )

        out.write(
            "\n"
                .toByteArray()
        )

        // Alinhamento esquerda
        out.write(
            byteArrayOf(
                ESC.toByte(),
                0x61,
                0x00
            )
        )

        out.write(
            "Bluetooth: OK\n"
                .toByteArray()
        )

        out.write(
            "ESC/POS: OK\n"
                .toByteArray()
        )

        out.write(
            "\n\n\n\n"
                .toByteArray()
        )

        return out.toByteArray()
    }
}