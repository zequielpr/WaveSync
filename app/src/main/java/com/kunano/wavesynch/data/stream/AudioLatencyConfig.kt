package com.kunano.wavesynch.data.stream

object AudioLatencyConfig {
    // how many full packets must be queued before we start playing
    const val WARMUP_PACKETS = 2          // was 4

    // how many times minBuffer we reserve in AudioTrack
    const val TRACK_BUFFER_FACTOR = 2     // was 4

    // jitter buffer capacity in packets
    const val JITTER_CAPACITY = 8         // was 16
}
