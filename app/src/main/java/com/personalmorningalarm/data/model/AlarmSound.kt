package com.personalmorningalarm.data.model

import androidx.annotation.RawRes
import com.personalmorningalarm.R

/** A selectable bundled alarm tone (raw resource). [key] is what's stored in config. */
data class AlarmSound(
    val key: String,
    val displayName: String,
    @RawRes val resId: Int
)

/**
 * The bundled sound pools: gentle tones for Stage 1, urgent ones for the nuclear
 * alarm. Config stores a sound's [AlarmSound.key]; lookups fall back to the first
 * sound so a missing/blank key always resolves to a sensible default.
 */
object AlarmSounds {

    val stage1: List<AlarmSound> = listOf(
        AlarmSound("gentle_soft_chime", "Soft Chime", R.raw.gentle_soft_chime),
        AlarmSound("gentle_bells", "Gentle Bells", R.raw.gentle_bells),
        AlarmSound("gentle_warm_waves", "Warm Waves", R.raw.gentle_warm_waves),
        AlarmSound("gentle_morning_glow", "Morning Glow", R.raw.gentle_morning_glow)
    )

    val nuclear: List<AlarmSound> = listOf(
        AlarmSound("urgent_rapid_beep", "Rapid Beep", R.raw.urgent_rapid_beep),
        AlarmSound("urgent_siren", "Siren", R.raw.urgent_siren),
        AlarmSound("urgent_klaxon", "Klaxon", R.raw.urgent_klaxon),
        AlarmSound("urgent_alarm_pulse", "Alarm Pulse", R.raw.urgent_alarm_pulse)
    )

    val defaultStage1: AlarmSound get() = stage1.first()
    val defaultNuclear: AlarmSound get() = nuclear.first()

    fun stage1ByKey(key: String?): AlarmSound = stage1.firstOrNull { it.key == key } ?: defaultStage1
    fun nuclearByKey(key: String?): AlarmSound = nuclear.firstOrNull { it.key == key } ?: defaultNuclear
}
