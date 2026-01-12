#include <jni.h>
#include <android/log.h>
#include <vector>
#include <opus.h>
#include <opus_defines.h>

#define LOG_TAG "OpusJNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define GET_ENCODER(ptr) reinterpret_cast<OpusEncoder*>(ptr)

struct DecoderHandle {
    OpusDecoder* dec;
    int channels;
};

#define GET_DECODER_HANDLE(ptr) reinterpret_cast<DecoderHandle*>(ptr)

extern "C" {

// ---------------- Encoder ----------------

JNIEXPORT jlong JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Encoder_createEncoder(
        JNIEnv* /*env*/, jobject /*thiz*/, jint sampleRate, jint channels) {
    int err = 0;
    OpusEncoder* enc = opus_encoder_create(sampleRate, channels, OPUS_APPLICATION_AUDIO, &err);
    if (err != OPUS_OK || !enc) {
        LOGE("opus_encoder_create failed: %s", opus_strerror(err));
        return 0;
    }
    return reinterpret_cast<jlong>(enc);
}

JNIEXPORT jbyteArray JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Encoder_encodePcm16(
        JNIEnv* env, jobject /*thiz*/, jlong pointer, jshortArray pcm, jint frameSize, jint channels) {

    OpusEncoder* enc = GET_ENCODER(pointer);
    if (!enc) {
        LOGE("encodePcm16: encoder pointer is null");
        return nullptr;
    }

    if (pcm == nullptr) {
        LOGE("encodePcm16: pcm is null");
        return nullptr;
    }

    const jsize pcmLen = env->GetArrayLength(pcm);
    if (pcmLen < frameSize * channels) {
        LOGE("encodePcm16: pcmLen(%d) < frameSize(%d) * channels(%d)",
             (int)pcmLen, (int)frameSize, (int)channels);
        return nullptr;
    }

    jshort* pcmPtr = env->GetShortArrayElements(pcm, nullptr);
    if (!pcmPtr) {
        LOGE("encodePcm16: GetShortArrayElements returned null");
        return nullptr;
    }

    // If you ever increase bitrate or frame size, you may need larger.
    const int maxPacket = 1500;
    unsigned char out[maxPacket];

    int n = opus_encode(enc,
                        reinterpret_cast<const opus_int16*>(pcmPtr),
                        frameSize,
                        out,
                        maxPacket);

    env->ReleaseShortArrayElements(pcm, pcmPtr, JNI_ABORT);

    if (n < 0) {
        LOGE("opus_encode failed: %s", opus_strerror(n));
        return nullptr;
    }

    jbyteArray arr = env->NewByteArray(n);
    if (!arr) return nullptr;
    env->SetByteArrayRegion(arr, 0, n, reinterpret_cast<const jbyte*>(out));
    return arr;
}

JNIEXPORT void JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Encoder_destroyEncoder(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer) {
    OpusEncoder* enc = GET_ENCODER(pointer);
    if (enc) opus_encoder_destroy(enc);
}

// Enable FEC
JNIEXPORT void JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Encoder_setInbandFecEnabled(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer, jboolean enabled) {

    OpusEncoder* enc = GET_ENCODER(pointer);
    if (!enc) {
        LOGE("setInbandFecEnabled: encoder pointer is null");
        return;
    }

    int rc = opus_encoder_ctl(enc, OPUS_SET_INBAND_FEC(enabled ? 1 : 0));
    if (rc != OPUS_OK) {
        LOGE("OPUS_SET_INBAND_FEC failed: %s", opus_strerror(rc));
    }
}

// Set packet loss percentage
JNIEXPORT void JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Encoder_setExpectedPacketLossPercent(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer, jint lossPercent) {

    OpusEncoder* enc = GET_ENCODER(pointer);
    if (!enc) {
        LOGE("setExpectedPacketLossPercent: encoder pointer is null");
        return;
    }

    if (lossPercent < 0) lossPercent = 0;
    if (lossPercent > 100) lossPercent = 100;

    int rc = opus_encoder_ctl(enc, OPUS_SET_PACKET_LOSS_PERC(lossPercent));
    if (rc != OPUS_OK) {
        LOGE("OPUS_SET_PACKET_LOSS_PERC(%d) failed: %s", lossPercent, opus_strerror(rc));
    }
}

JNIEXPORT void JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Encoder_setSignalMusic(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer) {

    OpusEncoder* enc = GET_ENCODER(pointer);
    if (!enc) {
        LOGE("setSignalMusic: encoder pointer is null");
        return;
    }

    int rc = opus_encoder_ctl(enc, OPUS_SET_SIGNAL(OPUS_SIGNAL_MUSIC));
    if (rc != OPUS_OK) {
        LOGE("OPUS_SET_SIGNAL(OPUS_SIGNAL_MUSIC) failed: %s", opus_strerror(rc));
    }
}

JNIEXPORT void JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Encoder_setBitrate(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer, jint bitrate) {

    OpusEncoder* enc = GET_ENCODER(pointer);
    if (!enc) {
        LOGE("setBitrate: encoder pointer is null");
        return;
    }

    if (bitrate < 6000) bitrate = 6000;
    int rc = opus_encoder_ctl(enc, OPUS_SET_BITRATE(bitrate));
    if (rc != OPUS_OK) {
        LOGE("OPUS_SET_BITRATE(%d) failed: %s", bitrate, opus_strerror(rc));
    }
}

JNIEXPORT void JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Encoder_setComplexity(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer, jint complexity) {

    OpusEncoder* enc = GET_ENCODER(pointer);
    if (!enc) {
        LOGE("setComplexity: encoder pointer is null");
        return;
    }

    if (complexity < 0) complexity = 0;
    if (complexity > 10) complexity = 10;

    int rc = opus_encoder_ctl(enc, OPUS_SET_COMPLEXITY(complexity));
    if (rc != OPUS_OK) {
        LOGE("OPUS_SET_COMPLEXITY(%d) failed: %s", complexity, opus_strerror(rc));
    }
}

// ---------------- Decoder ----------------

JNIEXPORT jlong JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Decoder_createDecoder(
        JNIEnv* /*env*/, jobject /*thiz*/, jint sampleRate, jint channels) {

    int err = 0;
    OpusDecoder* dec = opus_decoder_create(sampleRate, channels, &err);
    if (err != OPUS_OK || !dec) {
        LOGE("opus_decoder_create failed: %s", opus_strerror(err));
        return 0;
    }

    auto* handle = new DecoderHandle{dec, channels};
    return reinterpret_cast<jlong>(handle);
}

// -------- Existing allocating decode --------

JNIEXPORT jshortArray JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Decoder_decodePcm16(
        JNIEnv* env, jobject /*thiz*/, jlong pointer, jbyteArray packet, jint frameSize) {

    DecoderHandle* h = GET_DECODER_HANDLE(pointer);
    if (!h || !h->dec) {
        LOGE("decodePcm16: decoder pointer is null");
        return nullptr;
    }

    const int channels = h->channels;
    if (channels <= 0) {
        LOGE("decodePcm16: invalid channels=%d", channels);
        return nullptr;
    }

    if (packet == nullptr) {
        LOGE("decodePcm16: packet is null");
        return nullptr;
    }

    const jsize packetLen = env->GetArrayLength(packet);
    jbyte* packetPtr = env->GetByteArrayElements(packet, nullptr);
    if (!packetPtr) {
        LOGE("decodePcm16: GetByteArrayElements returned null");
        return nullptr;
    }

    std::vector<opus_int16> outBuf((size_t)frameSize * (size_t)channels);

    int decodedSamples = opus_decode(
            h->dec,
            reinterpret_cast<const unsigned char*>(packetPtr),
            (opus_int32)packetLen,
            outBuf.data(),
            frameSize,
            0
    );

    env->ReleaseByteArrayElements(packet, packetPtr, JNI_ABORT);

    if (decodedSamples < 0) {
        LOGE("opus_decode failed: %s", opus_strerror(decodedSamples));
        return nullptr;
    }

    const int outCount = decodedSamples * channels;
    jshortArray pcm = env->NewShortArray(outCount);
    if (!pcm) return nullptr;
    env->SetShortArrayRegion(pcm, 0, outCount,
                             reinterpret_cast<const jshort*>(outBuf.data()));
    return pcm;
}

JNIEXPORT jshortArray JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Decoder_decodePlcPcm16(
        JNIEnv* env, jobject /*thiz*/, jlong pointer, jint frameSize) {

    DecoderHandle* h = GET_DECODER_HANDLE(pointer);
    if (!h || !h->dec) {
        LOGE("decodePlcPcm16: decoder pointer is null");
        return nullptr;
    }

    const int channels = h->channels;
    if (channels <= 0) {
        LOGE("decodePlcPcm16: invalid channels=%d", channels);
        return nullptr;
    }

    std::vector<opus_int16> outBuf((size_t)frameSize * (size_t)channels);

    int decodedSamples = opus_decode(
            h->dec,
            nullptr,
            0,
            outBuf.data(),
            frameSize,
            0
    );

    if (decodedSamples < 0) {
        LOGE("opus_decode (PLC) failed: %s", opus_strerror(decodedSamples));
        return nullptr;
    }

    const int outCount = decodedSamples * channels;
    jshortArray pcm = env->NewShortArray(outCount);
    if (!pcm) return nullptr;
    env->SetShortArrayRegion(pcm, 0, outCount,
                             reinterpret_cast<const jshort*>(outBuf.data()));
    return pcm;
}

JNIEXPORT jshortArray JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Decoder_decodeFecFromNextPcm16(
        JNIEnv* env, jobject /*thiz*/, jlong pointer, jbyteArray nextPacket, jint frameSize) {

    DecoderHandle* h = GET_DECODER_HANDLE(pointer);
    if (!h || !h->dec) {
        LOGE("decodeFecFromNextPcm16: decoder pointer is null");
        return nullptr;
    }

    const int channels = h->channels;
    if (channels <= 0) {
        LOGE("decodeFecFromNextPcm16: invalid channels=%d", channels);
        return nullptr;
    }

    if (nextPacket == nullptr) {
        LOGE("decodeFecFromNextPcm16: nextPacket is null");
        return nullptr;
    }

    const jsize packetLen = env->GetArrayLength(nextPacket);
    jbyte* packetPtr = env->GetByteArrayElements(nextPacket, nullptr);
    if (!packetPtr) {
        LOGE("decodeFecFromNextPcm16: GetByteArrayElements returned null");
        return nullptr;
    }

    std::vector<opus_int16> outBuf((size_t)frameSize * (size_t)channels);

    int decodedSamples = opus_decode(
            h->dec,
            reinterpret_cast<const unsigned char*>(packetPtr),
            (opus_int32)packetLen,
            outBuf.data(),
            frameSize,
            1
    );

    env->ReleaseByteArrayElements(nextPacket, packetPtr, JNI_ABORT);

    if (decodedSamples < 0) {
        LOGE("opus_decode (FEC) failed: %s", opus_strerror(decodedSamples));
        return nullptr;
    }

    const int outCount = decodedSamples * channels;
    jshortArray pcm = env->NewShortArray(outCount);
    if (!pcm) return nullptr;
    env->SetShortArrayRegion(pcm, 0, outCount,
                             reinterpret_cast<const jshort*>(outBuf.data()));
    return pcm;
}

// -------- NEW: ZERO-ALLOCATION decode into provided ShortArray --------
// Returns: shorts written (>=0) or negative opus error code / custom negative codes.

JNIEXPORT jint JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Decoder_decodePcm16Into(
        JNIEnv* env, jobject /*thiz*/, jlong pointer, jbyteArray packet, jint frameSize, jshortArray outShorts) {

    DecoderHandle* h = GET_DECODER_HANDLE(pointer);
    if (!h || !h->dec) return -1;

    const int channels = h->channels;
    if (channels <= 0) return -2;

    if (outShorts == nullptr) return -3;

    const int needed = frameSize * channels;
    const jsize outLen = env->GetArrayLength(outShorts);
    if (outLen < needed) return -4;

    if (packet == nullptr) return -5;

    const jsize packetLen = env->GetArrayLength(packet);
    if (packetLen <= 0) return -6;

    // Copy packet bytes (avoid pinning)
    std::vector<jbyte> packetBuf((size_t)packetLen);
    env->GetByteArrayRegion(packet, 0, packetLen, packetBuf.data());

    std::vector<opus_int16> tmp((size_t)needed);

    int decodedSamples = opus_decode(
            h->dec,
            reinterpret_cast<const unsigned char*>(packetBuf.data()),
            (opus_int32)packetLen,
            tmp.data(),
            frameSize,
            0
    );

    if (decodedSamples < 0) return decodedSamples;

    const int outCount = decodedSamples * channels;
    env->SetShortArrayRegion(outShorts, 0, outCount, reinterpret_cast<const jshort*>(tmp.data()));
    return outCount;
}

JNIEXPORT jint JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Decoder_decodePlcPcm16Into(
        JNIEnv* env, jobject /*thiz*/, jlong pointer, jint frameSize, jshortArray outShorts) {

    DecoderHandle* h = GET_DECODER_HANDLE(pointer);
    if (!h || !h->dec) return -1;

    const int channels = h->channels;
    if (channels <= 0) return -2;

    if (outShorts == nullptr) return -3;

    const int needed = frameSize * channels;
    const jsize outLen = env->GetArrayLength(outShorts);
    if (outLen < needed) return -4;

    std::vector<opus_int16> tmp((size_t)needed);

    int decodedSamples = opus_decode(h->dec, nullptr, 0, tmp.data(), frameSize, 0);
    if (decodedSamples < 0) return decodedSamples;

    const int outCount = decodedSamples * channels;
    env->SetShortArrayRegion(outShorts, 0, outCount, reinterpret_cast<const jshort*>(tmp.data()));
    return outCount;
}

JNIEXPORT jint JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Decoder_decodeFecFromNextPcm16Into(
        JNIEnv* env, jobject /*thiz*/, jlong pointer, jbyteArray nextPacket, jint frameSize, jshortArray outShorts) {

    DecoderHandle* h = GET_DECODER_HANDLE(pointer);
    if (!h || !h->dec) return -1;

    const int channels = h->channels;
    if (channels <= 0) return -2;

    if (outShorts == nullptr) return -3;

    const int needed = frameSize * channels;
    const jsize outLen = env->GetArrayLength(outShorts);
    if (outLen < needed) return -4;

    if (nextPacket == nullptr) return -5;

    const jsize packetLen = env->GetArrayLength(nextPacket);
    if (packetLen <= 0) return -6;

    std::vector<jbyte> packetBuf((size_t)packetLen);
    env->GetByteArrayRegion(nextPacket, 0, packetLen, packetBuf.data());

    std::vector<opus_int16> tmp((size_t)needed);

    int decodedSamples = opus_decode(
            h->dec,
            reinterpret_cast<const unsigned char*>(packetBuf.data()),
            (opus_int32)packetLen,
            tmp.data(),
            frameSize,
            1
    );

    if (decodedSamples < 0) return decodedSamples;

    const int outCount = decodedSamples * channels;
    env->SetShortArrayRegion(outShorts, 0, outCount, reinterpret_cast<const jshort*>(tmp.data()));
    return outCount;
}

// -------- NEW: Reset decoder state (useful after resync jumps) --------
// Returns 0 on success, or opus error code.
JNIEXPORT jint JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Decoder_resetDecoderState(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer) {

    DecoderHandle* h = GET_DECODER_HANDLE(pointer);
    if (!h || !h->dec) return -1;

    int rc = opus_decoder_ctl(h->dec, OPUS_RESET_STATE);
    if (rc != OPUS_OK) {
        LOGE("OPUS_RESET_STATE failed: %s", opus_strerror(rc));
    }
    return rc;
}

JNIEXPORT void JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Decoder_destroyDecoder(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer) {

    DecoderHandle* h = GET_DECODER_HANDLE(pointer);
    if (!h) return;
    if (h->dec) opus_decoder_destroy(h->dec);
    delete h;
}

} // extern "C"
