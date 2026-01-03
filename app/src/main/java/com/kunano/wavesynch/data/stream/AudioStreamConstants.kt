package com.kunano.wavesynch.data.stream

import android.media.AudioFormat

object AudioStreamConstants {

    const val SAMPLE_RATE = 48_000
    const val CHANNEL_MASK_IN = AudioFormat.CHANNEL_IN_MONO
    const val CHANNEL_MASK_OUT = AudioFormat.CHANNEL_OUT_MONO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    // Network packet duration
    const val PACKET_MS = 10 // Using 10ms for a more standard packet size

    // 16-bit MONO => 2 bytes per frame
    const val BYTES_PER_FRAME = 2

    // 48k * 10ms = 480 frames; 480 * 2 = 960 bytes
    const val PAYLOAD_BYTES = (SAMPLE_RATE * PACKET_MS / 1000) * BYTES_PER_FRAME

    // How often the player writes one frame
    const val FRAME_MS = 10L
    const val FRAME_NS = FRAME_MS * 1_000_000L

    // Jitter buffer tuning
    const val PREBUFFER_FRAMES = 6            // startup buffer (e.g., 60ms if FRAME_MS=10)
    const val MAX_JITTER_FRAMES = 80          // cap memory/latency
    const val RESYNC_THRESHOLD_FRAMES = 12    // if we’re >120ms behind, jump forward

    const val UDP_PORT = 8989
    const val TCP_PORT = 8988
}
