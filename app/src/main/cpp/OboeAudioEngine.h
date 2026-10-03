#pragma once

#include <oboe/Oboe.h>
#include <vector>
#include <atomic>
#include <mutex>
#include <cmath>

struct SampleVoice {
    int sampleId = -1;
    int playbackPos = 0;
    int totalSamples = 0;
    const float* data = nullptr;
    float velocity = 1.0f;
    float pitchRatio = 1.0f;
    bool active = false;
    int chokeGroup = 0;
};

struct StepNote {
    bool active = false;
    int sampleId = 0;
    float velocity = 0.8f;
    int pitchOffset = 0;
};

class OboeAudioEngine : public oboe::AudioStreamDataCallback {
public:
    OboeAudioEngine();
    virtual ~OboeAudioEngine();

    bool start();
    void stop();
    bool isRunning() const { return mIsRunning.load(); }

    void triggerSample(int sampleId, float velocity, float pitchSemitones, int chokeGroup);
    void loadSampleBuffer(int sampleId, const float* data, int numSamples);

    void setBpm(int bpm);
    void setSchwung(int swingPercent);
    void setSequencerPlaying(bool playing);
    void setStepData(int trackId, int stepIndex, bool active, int sampleId, float velocity, int pitchOffset);
    int getCurrentStep() const { return mCurrentStep.load(); }

    void setMasterVolume(float vol);
    void setFilterCutoff(float cutoff);
    void setFilterResonance(float q);

    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream *audioStream,
        void *audioData,
        int32_t numFrames) override;

private:
    std::shared_ptr<oboe::AudioStream> mStream;
    std::atomic<bool> mIsRunning{false};

    static constexpr int kMaxVoices = 32;
    SampleVoice mVoices[kMaxVoices];
    std::mutex mVoiceMutex;

    static constexpr int kMaxLoadedSamples = 64;
    std::vector<float> mSampleBuffers[kMaxLoadedSamples];

    // Sequencer
    std::atomic<bool> mSequencerPlaying{false};
    std::atomic<int> mBpm{120};
    std::atomic<int> mSwingPercent{30};
    std::atomic<int> mCurrentStep{0};
    int64_t mFramesSinceLastStep{0};

    StepNote mPatternSteps[4][16];

    // Master DSP
    std::atomic<float> mMasterVolume{0.9f};
    std::atomic<float> mFilterCutoff{20000.0f};
    std::atomic<float> mFilterResonance{1.0f};

    float mFilterL_x1{0.0f}, mFilterL_x2{0.0f}, mFilterL_y1{0.0f}, mFilterL_y2{0.0f};
    float mFilterR_x1{0.0f}, mFilterR_x2{0.0f}, mFilterR_y1{0.0f}, mFilterR_y2{0.0f};

    void processStep();
    void renderVoice(SampleVoice& voice, float* outL, float* outR, int32_t numFrames);
    void applyFilter(float* bufL, float* bufR, int32_t numFrames);
};
