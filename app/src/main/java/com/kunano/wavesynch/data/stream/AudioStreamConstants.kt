package com.kunano.wavesynch.data.stream

import android.media.AudioFormat

object AudioStreamConstants {
    const val SAMPLE_RATE = 48_000

    // Stereo end-to-end
    const val CHANNELS = 2
    const val CHANNEL_MASK_IN = AudioFormat.CHANNEL_IN_STEREO
    const val CHANNEL_MASK_OUT = AudioFormat.CHANNEL_OUT_STEREO

    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    const val BYTES_PER_SAMPLE = 2

    const val MONO_BITRATE = 64_000
    const val STEREO_BITRATE = 128_000
    const val HOST_SPOT_COMPLEXITY = 5

    // Opus frame duration
    const val PACKET_MS = 20

    // PCM sizing (INTERLEAVED)
    const val SAMPLES_PER_CHANNEL = (SAMPLE_RATE * PACKET_MS / 1000) // 960
    const val SAMPLES_PER_PACKET = SAMPLES_PER_CHANNEL * CHANNELS     // 1920 shorts
    const val PCM_FRAME_BYTES = SAMPLES_PER_PACKET * BYTES_PER_SAMPLE // 3840 bytes

    const val FRAME_NS = PACKET_MS.toLong() * 1_000_000L

    // Tuning
    const val PREBUFFER_FRAMES = 10
    const val MAX_JITTER_FRAMES = 25
    const val RESYNC_THRESHOLD_FRAMES = 15

    const val UDP_PORT = 8989
    const val TCP_PORT = 8988
}