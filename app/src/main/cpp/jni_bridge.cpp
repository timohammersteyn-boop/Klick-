#include <jni.h>
#include "OboeAudioEngine.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_audio_OboeAudioEngine_initNativeEngine(JNIEnv *env, jobject thiz) {
    auto *engine = new OboeAudioEngine();
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_destroyNativeEngine(JNIEnv *env, jobject thiz, jlong engine_ptr) {
    auto *engine = reinterpret_cast<OboeAudioEngine*>(engine_ptr);
    if (engine) {
        delete engine;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_example_audio_OboeAudioEngine_startNativeStream(JNIEnv *env, jobject thiz, jlong engine_ptr) {
    auto *engine = reinterpret_cast<OboeAudioEngine*>(engine_ptr);
    if (engine) {
        return static_cast<jboolean>(engine->start());
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_stopNativeStream(JNIEnv *env, jobject thiz, jlong engine_ptr) {
    auto *engine = reinterpret_cast<OboeAudioEngine*>(engine_ptr);
    if (engine) {
        engine->stop();
    }
}

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_triggerSample(JNIEnv *env, jobject thiz, jlong engine_ptr,
                                                    jint sample_id, jfloat velocity,
                                                    jfloat pitch_semitones, jint choke_group) {
    auto *engine = reinterpret_cast<OboeAudioEngine*>(engine_ptr);
    if (engine) {
        engine->triggerSample(sample_id, velocity, pitch_semitones, choke_group);
    }
}

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_loadSampleBuffer(JNIEnv *env, jobject thiz, jlong engine_ptr,
                                                       jint sample_id, jfloatArray data) {
    auto *engine = reinterpret_cast<OboeAudioEngine*>(engine_ptr);
    if (!engine || !data) return;

    jsize len = env->GetArrayLength(data);
    jfloat *elements = env->GetFloatArrayElements(data, nullptr);
    if (elements) {
        engine->loadSampleBuffer(sample_id, elements, len);
        env->ReleaseFloatArrayElements(data, elements, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_setBpm(JNIEnv *env, jobject thiz, jlong engine_ptr, jint bpm) {
    auto *engine = reinterpret_cast<OboeAudioEngine*>(engine_ptr);
    if (engine) {
        engine->setBpm(bpm);
    }
}

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_setSchwung(JNIEnv *env, jobject thiz, jlong engine_ptr, jint swing_percent) {
    auto *engine = reinterpret_cast<OboeAudioEngine*>(engine_ptr);
    if (engine) {
        engine->setSchwung(swing_percent);
    }
}

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_setSequencerPlaying(JNIEnv *env, jobject thiz, jlong engine_ptr, jboolean playing) {
    auto *engine = reinterpret_cast<OboeAudioEngine*>(engine_ptr);
    if (engine) {
        engine->setSequencerPlaying(playing);
    }
}

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_setStepData(JNIEnv *env, jobject thiz, jlong engine_ptr,
                                                  jint track_id, jint step_index, jboolean active,
                                                  jint sample_id, jfloat velocity, jint pitch_offset) {
    auto *engine = reinterpret_cast<OboeAudioEngine*>(engine_ptr);
    if (engine) {
        engine->setStepData(track_id, step_index, active, sample_id, velocity, pitch_offset);
    }
}

JNIEXPORT jint JNICALL
Java_com_example_audio_OboeAudioEngine_getCurrentStep(JNIEnv *env, jobject thiz, jlong engine_ptr) {
    auto *engine = reinterpret_cast<OboeAudioEngine*>(engine_ptr);
    if (engine) {
        return engine->getCurrentStep();
    }
    return 0;
}

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_setMasterVolume(JNIEnv *env, jobject thiz, jlong engine_ptr, jfloat vol) {
    auto *engine = reinterpret_cast<OboeAudioEngine*>(engine_ptr);
    if (engine) {
        engine->setMasterVolume(vol);
    }
}

JNIEXPORT void JNICALL
Java_com_example_audio_OboeAudioEngine_setFilter(JNIEnv *env, jobject thiz, jlong engine_ptr, jfloat cutoff, jfloat q) {
    auto *engine = reinterpret_cast<OboeAudioEngine*>(engine_ptr);
    if (engine) {
        engine->setFilterCutoff(cutoff);
        engine->setFilterResonance(q);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_example_audio_OboeAudioEngine_isNativeRunning(JNIEnv *env, jobject thiz, jlong engine_ptr) {
    auto *engine = reinterpret_cast<OboeAudioEngine*>(engine_ptr);
    if (engine) {
        return static_cast<jboolean>(engine->isRunning());
    }
    return JNI_FALSE;
}

} // extern "C"
