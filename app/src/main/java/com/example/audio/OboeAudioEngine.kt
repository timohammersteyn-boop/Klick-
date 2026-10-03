package com.example.audio

import android.util.Log

class OboeAudioEngine : AutoCloseable {

    companion object {
        private const val TAG = "OboeAudioEngine"
        private var isLibraryLoaded = false

        init {
            try {
                System.loadLibrary("schwung_audio")
                isLibraryLoaded = true
            } catch (_: Throwable) {
                isLibraryLoaded = false
            }
        }

        fun isNativeAvailable(): Boolean = isLibraryLoaded
    }

    private var nativePtr: Long = 0L

    init {
        if (isLibraryLoaded) {
            try {
                nativePtr = initNativeEngine()
            } catch (_: Throwable) {
                nativePtr = 0L
            }
        }
    }

    fun isAvailable(): Boolean = isLibraryLoaded && nativePtr != 0L

    fun start(): Boolean {
        if (!isAvailable()) return false
        return try {
            startNativeStream(nativePtr)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting native Oboe stream", e)
            false
        }
    }

    fun stop() {
        if (!isAvailable()) return
        try {
            stopNativeStream(nativePtr)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping native Oboe stream", e)
        }
    }

    fun isRunning(): Boolean {
        if (!isAvailable()) return false
        return try {
            isNativeRunning(nativePtr)
        } catch (e: Exception) {
            false
        }
    }

    fun triggerSample(
        sampleId: Int,
        velocity: Float = 0.8f,
        pitchSemitones: Float = 0f,
        chokeGroup: Int = 0
    ) {
        if (!isAvailable()) return
        try {
            triggerSample(nativePtr, sampleId, velocity, pitchSemitones, chokeGroup)
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering sample $sampleId", e)
        }
    }

    fun loadSampleBuffer(sampleId: Int, data: FloatArray) {
        if (!isAvailable()) return
        try {
            loadSampleBuffer(nativePtr, sampleId, data)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sample buffer $sampleId", e)
        }
    }

    fun setBpm(bpm: Int) {
        if (!isAvailable()) return
        try {
            setBpm(nativePtr, bpm)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting bpm", e)
        }
    }

    fun setSchwung(swingPercent: Int) {
        if (!isAvailable()) return
        try {
            setSchwung(nativePtr, swingPercent)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting Schwung swing", e)
        }
    }

    fun setSequencerPlaying(playing: Boolean) {
        if (!isAvailable()) return
        try {
            setSequencerPlaying(nativePtr, playing)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting sequencer playing state", e)
        }
    }

    fun setStepData(
        trackId: Int,
        stepIndex: Int,
        active: Boolean,
        sampleId: Int = trackId,
        velocity: Float = 0.8f,
        pitchOffset: Int = 0
    ) {
        if (!isAvailable()) return
        try {
            setStepData(nativePtr, trackId, stepIndex, active, sampleId, velocity, pitchOffset)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting step data", e)
        }
    }

    fun getCurrentStep(): Int {
        if (!isAvailable()) return 0
        return try {
            getCurrentStep(nativePtr)
        } catch (e: Exception) {
            0
        }
    }

    fun setMasterVolume(vol: Float) {
        if (!isAvailable()) return
        try {
            setMasterVolume(nativePtr, vol)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting master volume", e)
        }
    }

    fun setFilter(cutoff: Float, q: Float = 1.0f) {
        if (!isAvailable()) return
        try {
            setFilter(nativePtr, cutoff, q)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting filter", e)
        }
    }

    override fun close() {
        if (nativePtr != 0L) {
            try {
                stop()
                destroyNativeEngine(nativePtr)
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying native engine", e)
            } finally {
                nativePtr = 0L
            }
        }
    }

    // Native JNI Declarations
    private external fun initNativeEngine(): Long
    private external fun destroyNativeEngine(enginePtr: Long)
    private external fun startNativeStream(enginePtr: Long): Boolean
    private external fun stopNativeStream(enginePtr: Long)
    private external fun triggerSample(
        enginePtr: Long,
        sampleId: Int,
        velocity: Float,
        pitchSemitones: Float,
        chokeGroup: Int
    )
    private external fun loadSampleBuffer(enginePtr: Long, sampleId: Int, data: FloatArray)
    private external fun setBpm(enginePtr: Long, bpm: Int)
    private external fun setSchwung(enginePtr: Long, swingPercent: Int)
    private external fun setSequencerPlaying(enginePtr: Long, playing: Boolean)
    private external fun setStepData(
        enginePtr: Long,
        trackId: Int,
        stepIndex: Int,
        active: Boolean,
        sampleId: Int,
        velocity: Float,
        pitchOffset: Int
    )
    private external fun getCurrentStep(enginePtr: Long): Int
    private external fun setMasterVolume(enginePtr: Long, vol: Float)
    private external fun setFilter(enginePtr: Long, cutoff: Float, q: Float)
    private external fun isNativeRunning(enginePtr: Long): Boolean
}
