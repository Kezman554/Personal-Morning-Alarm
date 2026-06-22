package com.personalmorningalarm.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import com.personalmorningalarm.data.model.AlarmSound
import com.personalmorningalarm.data.model.AlarmSounds

/**
 * Plays a one-shot preview of a selectable sound for the settings pickers. Mirrors
 * the real alarm exactly: the sound rides the device ALARM stream (not media), and
 * the alarm stream is set to the chosen volume for the duration of the preview — so
 * the preview is a true rehearsal of how loud the alarm will be, unaffected by the
 * media/ring volume. The previous alarm-stream level is restored when the preview
 * ends. Reuses a single MediaPlayer, stopping any in-progress preview first. Call
 * [release] when the host view goes away.
 */
class SoundPreviewPlayer {

    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var savedAlarmLevel: Int? = null

    /**
     * Plays [sound] with the alarm stream set to [volumeFraction] (0f-1f) of its max,
     * matching what the real alarm does on fire.
     */
    fun play(context: Context, sound: AlarmSound, volumeFraction: Float) {
        stop() // restores any prior preview's alarm-stream change first
        val app = context.applicationContext
        runCatching {
            val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            savedAlarmLevel = am.getStreamVolume(AudioManager.STREAM_ALARM)
            val level = Math.round(volumeFraction.coerceIn(0f, 1f) * max).coerceIn(0, max)
            am.setStreamVolume(AudioManager.STREAM_ALARM, level, 0)
            audioManager = am

            player = MediaPlayer().apply {
                AlarmSounds.setDataSource(this, app, sound)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                prepare()
                setVolume(1f, 1f) // loudness comes from the alarm stream level set above
                setOnCompletionListener { stop() }
                start()
            }
        }.onFailure { stop() }
    }

    fun stop() {
        player?.run { runCatching { if (isPlaying) stop() }; release() }
        player = null
        // Restore the alarm stream to where the user had it before the preview.
        val am = audioManager
        val saved = savedAlarmLevel
        if (am != null && saved != null) {
            runCatching { am.setStreamVolume(AudioManager.STREAM_ALARM, saved, 0) }
        }
        savedAlarmLevel = null
        audioManager = null
    }

    /** Alias for [stop]; clearer intent at teardown. */
    fun release() = stop()
}
