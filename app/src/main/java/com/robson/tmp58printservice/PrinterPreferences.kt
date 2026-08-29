package com.robson.tmp58printservice

import android.content.Context
import androidx.core.content.edit

data class PrinterConfiguration(
    val name: String,
    val address: String?,
    val widthDots: Int,
    val threshold: Int
)

object PrinterPreferences {
    private const val PREFERENCES_NAME = "printer_configuration"
    private const val KEY_NAME = "printer_name"
    private const val KEY_ADDRESS = "printer_address"
    private const val KEY_WIDTH_DOTS = "printer_width_dots"
    private const val KEY_THRESHOLD = "printer_threshold"

    const val DEFAULT_NAME = "IMP-TMP58ABT"
    const val DEFAULT_WIDTH_DOTS = 384
    const val DEFAULT_THRESHOLD = 160

    fun load(context: Context): PrinterConfiguration {
        val preferences = context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

        return PrinterConfiguration(
            name = preferences.getString(KEY_NAME, DEFAULT_NAME) ?: DEFAULT_NAME,
            address = preferences.getString(KEY_ADDRESS, null),
            widthDots = preferences.getInt(KEY_WIDTH_DOTS, DEFAULT_WIDTH_DOTS),
            threshold = preferences.getInt(KEY_THRESHOLD, DEFAULT_THRESHOLD)
        )
    }

    fun saveDevice(context: Context, device: PairedPrinter) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_NAME, device.name)
                putString(KEY_ADDRESS, device.address)
            }
    }
}
