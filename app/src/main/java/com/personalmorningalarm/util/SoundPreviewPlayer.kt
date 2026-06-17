package com.personalmorningalarm.util

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes

/**
 * Plays a one-shot preview of a bundled sound for the settings pickers. Reuses a
 * single MediaPlayer, stopping any in-progress preview first. Call [release] when
 * the host view goes away.
 */
class SoundPreviewPlayer {

    private var player: MediaPlayer? = null

    fun play(context: Context, @RawRes resId: Int) {
        stop()
        player = MediaPlayer.create(context, resId)?.apply {
            setOnCompletionListener { release() }
            start()
        }
    }

    fun stop() {
        player?.run { runCatching { if (isPlaying) stop() }; release() }
        player = null
    }

    /** Alias for [stop]; clearer intent at teardown. */
    fun release() = stop()
}
