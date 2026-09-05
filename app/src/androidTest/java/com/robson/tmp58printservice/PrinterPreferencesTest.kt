package com.robson.tmp58printservice

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrinterPreferencesTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    @Before
    fun clearPreferences() {
        preferences().edit().clear().commit()
    }

    @After
    fun cleanUp() {
        preferences().edit().clear().commit()
    }

    @Test
    fun profiles_areStoredIndependentlyByBluetoothAddress() {
        val first = PairedPrinter("Printer 58", "00:11:22:33:44:55")
        val second = PairedPrinter("Printer 80", "AA:BB:CC:DD:EE:FF")

        PrinterPreferences.saveDevice(context, first)
        PrinterPreferences.saveProfile(context, first.address, PaperProfile.PAPER_58_MM)
        PrinterPreferences.saveDevice(context, second)
        PrinterPreferences.saveProfile(context, second.address, PaperProfile.PAPER_80_MM)
        PrinterPreferences.saveDevice(context, first)

        assertEquals(PaperProfile.PAPER_58_MM, PrinterPreferences.load(context).paperProfile)

        PrinterPreferences.saveDevice(context, second)
        assertEquals(PaperProfile.PAPER_80_MM, PrinterPreferences.load(context).paperProfile)
    }

    @Test
    fun load_migratesLegacyWidthAndThresholdForSelectedPrinter() {
        preferences().edit()
            .putString("printer_name", "Legacy printer")
            .putString("printer_address", "12:34:56:78:90:AB")
            .putInt("printer_width_dots", 432)
            .putInt("printer_threshold", 175)
            .commit()

        val configuration = PrinterPreferences.load(context)

        assertEquals(432, configuration.paperProfile.widthDots)
        assertEquals(175, configuration.paperProfile.threshold)
        assertEquals(58, configuration.paperProfile.paperWidthMm)
    }

    private fun preferences() = context.getSharedPreferences(
        "printer_configuration",
        Context.MODE_PRIVATE
    )
}
