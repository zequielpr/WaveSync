package com.kunano.wavesynch.data.stream

import android.media.AudioFormat

object AudioStreamConstants {


    const val SAMPLE_RATE = 48_000
    const val CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO

    const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    const val TCP_PORT = 8988 // Port for control signals
    const val UDP_PORT = 8989 // Port for audio streaming
}
