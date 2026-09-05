package com.robson.tmp58printservice

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

enum class PaperProfileType {
    PAPER_58_MM,
    PAPER_80_MM,
    CUSTOM
}

data class PaperProfile(
    val type: PaperProfileType,
    val paperWidthMm: Int,
    val widthDots: Int,
    val dpi: Int,
    val threshold: Int
) {
    init {
        require(paperWidthMm in MIN_PAPER_WIDTH_MM..MAX_PAPER_WIDTH_MM) {
            "A largura do papel deve estar entre $MIN_PAPER_WIDTH_MM e $MAX_PAPER_WIDTH_MM mm"
        }
        require(widthDots in MIN_WIDTH_DOTS..MAX_WIDTH_DOTS) {
            "A largura imprimível deve estar entre $MIN_WIDTH_DOTS e $MAX_WIDTH_DOTS pontos"
        }
        require(dpi in MIN_DPI..MAX_DPI) {
            "A resolução deve estar entre $MIN_DPI e $MAX_DPI DPI"
        }
        require(threshold in MIN_THRESHOLD..MAX_THRESHOLD) {
            "O limiar deve estar entre $MIN_THRESHOLD e $MAX_THRESHOLD"
        }
    }

    companion object {
        const val MIN_PAPER_WIDTH_MM = 20
        const val MAX_PAPER_WIDTH_MM = 300
        const val MIN_WIDTH_DOTS = 1
        const val MAX_WIDTH_DOTS = 2048
        const val MIN_DPI = 72
        const val MAX_DPI = 600
        const val MIN_THRESHOLD = 0
        const val MAX_THRESHOLD = 255

        val PAPER_58_MM = PaperProfile(
            PaperProfileType.PAPER_58_MM,
            paperWidthMm = 58,
            widthDots = 384,
            dpi = 203,
            threshold = 160
        )
        val PAPER_80_MM = PaperProfile(
            PaperProfileType.PAPER_80_MM,
            paperWidthMm = 80,
            widthDots = 576,
            dpi = 203,
            threshold = 160
        )
    }
}

data class PrinterConfiguration(
    val name: String,
    val address: String?,
    val paperProfile: PaperProfile
) {
    val widthDots: Int
        get() = paperProfile.widthDots

    val threshold: Int
        get() = paperProfile.threshold
}

object PrinterPreferences {
    private const val PREFERENCES_NAME = "printer_configuration"
    private const val KEY_NAME = "printer_name"
    private const val KEY_ADDRESS = "printer_address"
    private const val LEGACY_KEY_WIDTH_DOTS = "printer_width_dots"
    private const val LEGACY_KEY_THRESHOLD = "printer_threshold"
    private const val PROFILE_PREFIX = "paper_profile_"
    private const val KEY_PROFILE_TYPE = "type"
    private const val KEY_PAPER_WIDTH_MM = "paper_width_mm"
    private const val KEY_WIDTH_DOTS = "width_dots"
    private const val KEY_DPI = "dpi"
    private const val KEY_THRESHOLD = "threshold"
    private const val LEGACY_PROFILE_ID = "legacy"

    const val DEFAULT_NAME = "IMP-TMP58ABT"
    const val DEFAULT_WIDTH_DOTS = 384
    const val DEFAULT_THRESHOLD = 160

    fun load(context: Context): PrinterConfiguration {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val address = preferences.getString(KEY_ADDRESS, null)
        val profileId = profileId(address)
        val profile = if (preferences.contains(profileKey(profileId, KEY_PROFILE_TYPE))) {
            loadProfile(preferences, profileId)
        } else {
            migratedLegacyProfile(preferences).also { saveProfile(context, address, it) }
        }

        return PrinterConfiguration(
            name = preferences.getString(KEY_NAME, DEFAULT_NAME) ?: DEFAULT_NAME,
            address = address,
            paperProfile = profile
        )
    }

    fun saveDevice(context: Context, device: PairedPrinter) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.edit {
            putString(KEY_NAME, device.name)
            putString(KEY_ADDRESS, device.address)
        }
        val newProfileId = profileId(device.address)
        if (!preferences.contains(profileKey(newProfileId, KEY_PROFILE_TYPE))) {
            saveProfile(context, device.address, PaperProfile.PAPER_58_MM)
        }
    }

    fun saveProfile(context: Context, address: String?, profile: PaperProfile) {
        val profileId = profileId(address)
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
            putString(profileKey(profileId, KEY_PROFILE_TYPE), profile.type.name)
            putInt(profileKey(profileId, KEY_PAPER_WIDTH_MM), profile.paperWidthMm)
            putInt(profileKey(profileId, KEY_WIDTH_DOTS), profile.widthDots)
            putInt(profileKey(profileId, KEY_DPI), profile.dpi)
            putInt(profileKey(profileId, KEY_THRESHOLD), profile.threshold)
        }
    }

    private fun loadProfile(preferences: SharedPreferences, profileId: String): PaperProfile {
        val type = preferences.getString(profileKey(profileId, KEY_PROFILE_TYPE), null)
            ?.let { stored -> PaperProfileType.entries.firstOrNull { it.name == stored } }
            ?: PaperProfileType.PAPER_58_MM
        val defaults = when (type) {
            PaperProfileType.PAPER_80_MM -> PaperProfile.PAPER_80_MM
            PaperProfileType.PAPER_58_MM,
            PaperProfileType.CUSTOM -> PaperProfile.PAPER_58_MM
        }

        return runCatching {
            PaperProfile(
                type = type,
                paperWidthMm = preferences.getInt(
                    profileKey(profileId, KEY_PAPER_WIDTH_MM),
                    defaults.paperWidthMm
                ),
                widthDots = preferences.getInt(
                    profileKey(profileId, KEY_WIDTH_DOTS),
                    defaults.widthDots
                ),
                dpi = preferences.getInt(profileKey(profileId, KEY_DPI), defaults.dpi),
                threshold = preferences.getInt(
                    profileKey(profileId, KEY_THRESHOLD),
                    defaults.threshold
                )
            )
        }.getOrDefault(defaults)
    }

    private fun migratedLegacyProfile(preferences: SharedPreferences): PaperProfile {
        val widthDots = preferences.getInt(LEGACY_KEY_WIDTH_DOTS, DEFAULT_WIDTH_DOTS)
        val threshold = preferences.getInt(LEGACY_KEY_THRESHOLD, DEFAULT_THRESHOLD)
        return runCatching {
            PaperProfile.PAPER_58_MM.copy(widthDots = widthDots, threshold = threshold)
        }.getOrDefault(PaperProfile.PAPER_58_MM)
    }

    private fun profileId(address: String?): String {
        return address?.uppercase()?.replace(":", "") ?: LEGACY_PROFILE_ID
    }

    private fun profileKey(profileId: String, key: String): String {
        return "${PROFILE_PREFIX}${profileId}_$key"
    }
}
