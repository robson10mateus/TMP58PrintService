package com.robson.tmp58printservice

import org.junit.Assert.assertEquals
import org.junit.Test

class PaperProfileTest {
    @Test
    fun presets_matchCommonEscPosPrintheadWidths() {
        assertEquals(58, PaperProfile.PAPER_58_MM.paperWidthMm)
        assertEquals(384, PaperProfile.PAPER_58_MM.widthDots)
        assertEquals(80, PaperProfile.PAPER_80_MM.paperWidthMm)
        assertEquals(576, PaperProfile.PAPER_80_MM.widthDots)
    }

    @Test
    fun customProfile_acceptsSupportedValues() {
        val profile = PaperProfile(
            type = PaperProfileType.CUSTOM,
            paperWidthMm = 112,
            widthDots = 832,
            dpi = 203,
            threshold = 170
        )

        assertEquals(832, profile.widthDots)
    }

    @Test(expected = IllegalArgumentException::class)
    fun customProfile_rejectsWidthBeyondRendererLimit() {
        PaperProfile(
            type = PaperProfileType.CUSTOM,
            paperWidthMm = 80,
            widthDots = 2049,
            dpi = 203,
            threshold = 160
        )
    }
}
