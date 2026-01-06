package com.kunano.wavesynch.data.stream

import android.media.AudioFormat

object AudioStreamConstants {
    const val SAMPLE_RATE = 48_000
    const val CHANNEL_MASK_IN = AudioFormat.CHANNEL_IN_MONO
    const val CHANNEL_MASK_OUT = AudioFormat.CHANNEL_OUT_MONO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    const val CHANNELS = 1

    const val PACKET_MS = 10
    const val BYTES_PER_FRAME = 2
    const val PCM_FRAME_BYTES = (SAMPLE_RATE * PACKET_MS / 1000) * BYTES_PER_FRAME // 960

    const val SAMPLES_PER_PACKET = (SAMPLE_RATE * PACKET_MS / 1000) // 480

    const val FRAME_MS = PACKET_MS.toLong()
    const val FRAME_NS = FRAME_MS * 1_000_000L

    // Low-latency tuning
    const val PREBUFFER_FRAMES = 3          // 30ms startup
    const val MAX_JITTER_FRAMES = 10        // 100ms cap
    const val RESYNC_THRESHOLD_FRAMES = 5   // 50ms behind => resync

    const val UDP_PORT = 8989
    const val TCP_PORT = 8988
}