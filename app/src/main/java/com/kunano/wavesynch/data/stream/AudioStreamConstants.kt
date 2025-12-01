package com.kunano.wavesynch.data.stream

object AudioStreamConstants {
    const val SAMPLE_RATE = 48_000
    const val CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_IN_MONO
    const val AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT
    const val PORT = 52_000   // arbitrary but high
}
