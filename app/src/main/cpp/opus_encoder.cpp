#include <jni.h>
#include <android/log.h>
#include <vector>
#include <opus.h>

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
        JNIEnv* env, jobject /*thiz*/, jint sampleRate, jint channels) {
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

    const jsize pcmLen = env->GetArrayLength(pcm);
    if (pcmLen < frameSize * channels) {
        LOGE("encodePcm16: pcmLen(%d) < frameSize(%d) * channels(%d)",
             (int)pcmLen, (int)frameSize, (int)channels);
        return nullptr;
    }

    jshort* pcmPtr = env->GetShortArrayElements(pcm, nullptr);

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
    env->SetByteArrayRegion(arr, 0, n, reinterpret_cast<const jbyte*>(out));
    return arr;
}

JNIEXPORT void JNICALL
Java_com_kunano_wavesynch_data_stream_OpusNative_00024Encoder_destroyEncoder(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong pointer) {
OpusEncoder* enc = GET_ENCODER(pointer);
if (enc) opus_encoder_destroy(enc);
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

    const jsize packetLen = env->GetArrayLength(packet);
    jbyte* packetPtr = env->GetByteArrayElements(packet, nullptr);

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
    env->SetShortArrayRegion(pcm, 0, outCount,
                             reinterpret_cast<const jshort*>(outBuf.data()));
    return pcm;
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
