package com.personalmorningalarm.util

import android.content.Context
import android.media.MediaPlayer
import com.personalmorningalarm.data.model.AlarmSound
import com.personalmorningalarm.data.model.AlarmSounds

/**
 * Plays a one-shot preview of a selectable sound for the settings pickers, at the
 * given volume so the preview reflects the chosen loudness. Reuses a single
 * MediaPlayer, stopping any in-progress preview first. Call [release] when the host
 * view goes away.
 */
class SoundPreviewPlayer {

    private var player: MediaPlayer? = null

    /** Plays [sound] at [volume] (0f-1f), reflecting the selected volume. */
    fun play(context: Context, sound: AlarmSound, volume: Float) {
        stop()
        val v = volume.coerceIn(0f, 1f)
        runCatching {
            player = MediaPlayer().apply {
                AlarmSounds.setDataSource(this, context, sound)
                prepare()
                setVolume(v, v)
                setOnCompletionListener { it.release() }
                start()
            }
        }
    }

    fun stop() {
        player?.run { runCatching { if (isPlaying) stop() }; release() }
        player = null
    }

    /** Alias for [stop]; clearer intent at teardown. */
    fun release() = stop()
}
