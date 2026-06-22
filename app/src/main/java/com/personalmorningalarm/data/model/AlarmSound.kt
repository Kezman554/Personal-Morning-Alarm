package com.personalmorningalarm.data.model

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.provider.Settings
import androidx.annotation.RawRes
import com.personalmorningalarm.R

/**
 * A selectable alarm tone. [key] is what's stored in config. A [resId] of 0 marks
 * the special "system default alarm" entry, which resolves to the device's default
 * TYPE_ALARM ringtone at playback time rather than a bundled raw resource.
 */
data class AlarmSound(
    val key: String,
    val displayName: String,
    @RawRes val resId: Int
) {
    val isSystemDefault: Boolean get() = resId == 0
}

/**
 * The selectable sound pools: gentle tones for Stage 1, urgent ones for the nuclear
 * alarm. Config stores a sound's [AlarmSound.key]; lookups fall back to the first
 * sound so a missing/blank key always resolves to a sensible default.
 */
object AlarmSounds {

    /** Key for the "use the device's default alarm sound" Stage 1 option. */
    const val SYSTEM_DEFAULT_KEY = "system_default"

    val stage1: List<AlarmSound> = listOf(
        // The original alarm tone — the device's default alarm ringtone. Kept first
        // so it's the default for new/blank config.
        AlarmSound(SYSTEM_DEFAULT_KEY, "Default (system alarm)", 0),
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

    /**
     * Sets [player]'s data source for [sound] — the device default alarm ringtone
     * for the system-default entry, otherwise the bundled raw resource. The caller
     * is responsible for calling [MediaPlayer.prepare].
     */
    fun setDataSource(player: MediaPlayer, context: Context, sound: AlarmSound) {
        if (sound.isSystemDefault) {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: Settings.System.DEFAULT_ALARM_ALERT_URI
            player.setDataSource(context, uri)
        } else {
            context.resources.openRawResourceFd(sound.resId)?.use {
                player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
        }
    }
}
