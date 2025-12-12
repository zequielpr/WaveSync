package com.kunano.wavesynch.data.stream

import android.media.AudioFormat

object AudioStreamConstants {
    const val SAMPLE_RATE = 44100
    const val CHANNEL_IN = AudioFormat.CHANNEL_IN_STEREO
    const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_STEREO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    const val PORT = 52_000   // arbitrary but high
}
