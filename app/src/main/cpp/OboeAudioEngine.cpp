#include "OboeAudioEngine.h"
#include <android/log.h>
#include <algorithm>

#define TAG "SchwungOboeEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static constexpr int32_t kSampleRate = 44100;

OboeAudioEngine::OboeAudioEngine() {
    for (int t = 0; t < 4; ++t) {
        for (int s = 0; s < 16; ++s) {
            mPatternSteps[t][s].active = false;
            mPatternSteps[t][s].sampleId = t;
            mPatternSteps[t][s].velocity = 0.8f;
            mPatternSteps[t][s].pitchOffset = 0;
        }
    }
}

OboeAudioEngine::~OboeAudioEngine() {
    stop();
}

bool OboeAudioEngine::start() {
    if (mIsRunning.load()) {
        return true;
    }

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Exclusive)
           ->setFormat(oboe::AudioFormat::Float)
           ->setChannelCount(oboe::ChannelCount::Stereo)
           ->setSampleRate(kSampleRate)
           ->setDataCallback(this);

    oboe::Result result = builder.openStream(mStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open Oboe stream: %s", oboe::convertToText(result));
        return false;
    }

    result = mStream->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start Oboe stream: %s", oboe::convertToText(result));
        mStream->close();
        return false;
    }

    mIsRunning.store(true);
    LOGI("Oboe audio stream started successfully (sampleRate=%d, framesPerBurst=%d)",
         mStream->getSampleRate(), mStream->getFramesPerBurst());
    return true;
}

void OboeAudioEngine::stop() {
    if (!mIsRunning.load()) {
        return;
    }

    mIsRunning.store(false);
    if (mStream) {
        mStream->stop();
        mStream->close();
        mStream.reset();
    }
    LOGI("Oboe audio stream stopped");
}

void OboeAudioEngine::loadSampleBuffer(int sampleId, const float* data, int numSamples) {
    if (sampleId < 0 || sampleId >= kMaxLoadedSamples || !data || numSamples <= 0) {
        return;
    }
    std::lock_guard<std::mutex> lock(mVoiceMutex);
    mSampleBuffers[sampleId].assign(data, data + numSamples);
    LOGI("Loaded sample %d into native cache (%d frames)", sampleId, numSamples);
}

void OboeAudioEngine::triggerSample(int sampleId, float velocity, float pitchSemitones, int chokeGroup) {
    if (sampleId < 0 || sampleId >= kMaxLoadedSamples) {
        return;
    }

    std::lock_guard<std::mutex> lock(mVoiceMutex);

    if (mSampleBuffers[sampleId].empty()) {
        return;
    }

    // Handle choke group: mute any voices in the same choke group
    if (chokeGroup > 0) {
        for (int i = 0; i < kMaxVoices; ++i) {
            if (mVoices[i].active && mVoices[i].chokeGroup == chokeGroup) {
                mVoices[i].active = false;
            }
        }
    }

    // Find free voice
    int freeIdx = -1;
    for (int i = 0; i < kMaxVoices; ++i) {
        if (!mVoices[i].active) {
            freeIdx = i;
            break;
        }
    }

    if (freeIdx == -1) {
        freeIdx = 0; // steal oldest
    }

    SampleVoice& voice = mVoices[freeIdx];
    voice.sampleId = sampleId;
    voice.data = mSampleBuffers[sampleId].data();
    voice.totalSamples = static_cast<int>(mSampleBuffers[sampleId].size());
    voice.playbackPos = 0;
    voice.velocity = std::clamp(velocity, 0.05f, 1.2f);
    voice.pitchRatio = std::pow(2.0f, pitchSemitones / 12.0f);
    voice.chokeGroup = chokeGroup;
    voice.active = true;
}

void OboeAudioEngine::setBpm(int bpm) {
    mBpm.store(std::clamp(bpm, 40, 240));
}

void OboeAudioEngine::setSchwung(int swingPercent) {
    mSwingPercent.store(std::clamp(swingPercent, 0, 75));
}

void OboeAudioEngine::setSequencerPlaying(bool playing) {
    mSequencerPlaying.store(playing);
    if (!playing) {
        mCurrentStep.store(0);
        mFramesSinceLastStep = 0;
    }
}

void OboeAudioEngine::setStepData(int trackId, int stepIndex, bool active, int sampleId, float velocity, int pitchOffset) {
    if (trackId >= 0 && trackId < 4 && stepIndex >= 0 && stepIndex < 16) {
        mPatternSteps[trackId][stepIndex].active = active;
        mPatternSteps[trackId][stepIndex].sampleId = sampleId;
        mPatternSteps[trackId][stepIndex].velocity = velocity;
        mPatternSteps[trackId][stepIndex].pitchOffset = pitchOffset;
    }
}

void OboeAudioEngine::setMasterVolume(float vol) {
    mMasterVolume.store(std::clamp(vol, 0.0f, 1.0f));
}

void OboeAudioEngine::setFilterCutoff(float cutoff) {
    mFilterCutoff.store(std::clamp(cutoff, 100.0f, 20000.0f));
}

void OboeAudioEngine::setFilterResonance(float q) {
    mFilterResonance.store(std::clamp(q, 0.5f, 5.0f));
}

void OboeAudioEngine::processStep() {
    int step = mCurrentStep.load();
    for (int t = 0; t < 4; ++t) {
        const StepNote& note = mPatternSteps[t][step];
        if (note.active) {
            triggerSample(note.sampleId, note.velocity, static_cast<float>(note.pitchOffset), (t == 1) ? 1 : 0);
        }
    }
    mCurrentStep.store((step + 1) % 16);
}

oboe::DataCallbackResult OboeAudioEngine::onAudioReady(
    oboe::AudioStream *audioStream,
    void *audioData,
    int32_t numFrames) {

    auto *output = static_cast<float*>(audioData);
    std::fill_n(output, numFrames * 2, 0.0f);

    std::vector<float> bufL(numFrames, 0.0f);
    std::vector<float> bufR(numFrames, 0.0f);

    // 1. Process Sequencer Timing with Schwung Swing
    if (mSequencerPlaying.load()) {
        float bpm = static_cast<float>(mBpm.load());
        float swing = static_cast<float>(mSwingPercent.load()) / 100.0f;
        float baseStepFrames = (60.0f / bpm / 4.0f) * static_cast<float>(kSampleRate);

        int curStep = mCurrentStep.load();
        bool isOddStep = (curStep % 2 == 1);
        float swingRatio = isOddStep ? (1.0f + swing * 0.5f) : (1.0f - swing * 0.5f);
        int64_t targetStepFrames = static_cast<int64_t>(baseStepFrames * swingRatio);

        mFramesSinceLastStep += numFrames;
        if (mFramesSinceLastStep >= targetStepFrames) {
            mFramesSinceLastStep -= targetStepFrames;
            processStep();
        }
    }

    // 2. Mix active voices
    {
        std::lock_guard<std::mutex> lock(mVoiceMutex);
        for (int i = 0; i < kMaxVoices; ++i) {
            if (mVoices[i].active) {
                renderVoice(mVoices[i], bufL.data(), bufR.data(), numFrames);
            }
        }
    }

    // 3. Apply Filter if engaged
    float cutoff = mFilterCutoff.load();
    if (cutoff < 19500.0f) {
        applyFilter(bufL.data(), bufR.data(), numFrames);
    }

    // 4. Master volume, soft-clipping, and output interleaving
    float masterVol = mMasterVolume.load();
    for (int32_t i = 0; i < numFrames; ++i) {
        float l = bufL[i] * masterVol;
        float r = bufR[i] * masterVol;

        // Soft saturation
        l = std::tanh(l);
        r = std::tanh(r);

        output[i * 2] = l;
        output[i * 2 + 1] = r;
    }

    return oboe::DataCallbackResult::Continue;
}

void OboeAudioEngine::renderVoice(SampleVoice& voice, float* outL, float* outR, int32_t numFrames) {
    if (!voice.active || !voice.data) return;

    for (int32_t i = 0; i < numFrames; ++i) {
        if (voice.playbackPos >= voice.totalSamples) {
            voice.active = false;
            break;
        }

        float s = voice.data[voice.playbackPos] * voice.velocity;
        outL[i] += s;
        outR[i] += s;

        voice.playbackPos++;
    }
}

void OboeAudioEngine::applyFilter(float* bufL, float* bufR, int32_t numFrames) {
    float cutoff = mFilterCutoff.load();
    float q = mFilterResonance.load();

    float omega = 2.0f * M_PI * std::clamp(cutoff, 80.0f, 19000.0f) / static_cast<float>(kSampleRate);
    float sinO = std::sin(omega);
    float cosO = std::cos(omega);
    float alpha = sinO / (2.0f * std::clamp(q, 0.5f, 5.0f));

    float b0 = (1.0f - cosO) / 2.0f;
    float b1 = 1.0f - cosO;
    float b2 = (1.0f - cosO) / 2.0f;
    float a0 = 1.0f + alpha;
    float a1 = -2.0f * cosO;
    float a2 = 1.0f - alpha;

    float invA0 = 1.0f / a0;
    float nb0 = b0 * invA0;
    float nb1 = b1 * invA0;
    float nb2 = b2 * invA0;
    float na1 = a1 * invA0;
    float na2 = a2 * invA0;

    for (int32_t i = 0; i < numFrames; ++i) {
        float inL = bufL[i];
        float outL = nb0 * inL + nb1 * mFilterL_x1 + nb2 * mFilterL_x2 - na1 * mFilterL_y1 - na2 * mFilterL_y2;
        mFilterL_x2 = mFilterL_x1;
        mFilterL_x1 = inL;
        mFilterL_y2 = mFilterL_y1;
        mFilterL_y1 = outL;
        bufL[i] = outL;

        float inR = bufR[i];
        float outR = nb0 * inR + nb1 * mFilterR_x1 + nb2 * mFilterR_x2 - na1 * mFilterR_y1 - na2 * mFilterR_y2;
        mFilterR_x2 = mFilterR_x1;
        mFilterR_x1 = inR;
        mFilterR_y2 = mFilterR_y1;
        mFilterR_y1 = outR;
        bufR[i] = outR;
    }
}
