package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*
import java.util.concurrent.CopyOnWriteArrayList

class SchwungAudioEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 1024
    }

    private var audioTrack: AudioTrack? = null
    private var isRunning = false
    private var audioThread: Thread? = null

    // Master Parameters
    var masterVolume = 0.9f
    var filterCutoffHz = 20000f
    var filterResonance = 1.0f // Q factor
    var delayWet = 0.0f
    var delayFeedback = 0.4f
    var delaySamples = (SAMPLE_RATE * 0.25f).toInt() // 1/8 note approx
    var driveSaturation = 0.1f

    // Master FX Chain: Reverb, Bitcrusher
    var masterReverbWet = 0.15f
    var masterReverbRoomSize = 0.7f
    var masterBitcrusherWet = 0.0f
    var masterBitcrusherBits = 8
    var masterBitcrusherDownsample = 1

    // Reverb buffers (Freeverb tuning)
    private val combL1 = FloatArray(1116)
    private val combL2 = FloatArray(1188)
    private val combL3 = FloatArray(1277)
    private val combL4 = FloatArray(1356)
    private var combIdxL1 = 0
    private var combIdxL2 = 0
    private var combIdxL3 = 0
    private var combIdxL4 = 0

    private val combR1 = FloatArray(1139)
    private val combR2 = FloatArray(1211)
    private val combR3 = FloatArray(1300)
    private val combR4 = FloatArray(1379)
    private var combIdxR1 = 0
    private var combIdxR2 = 0
    private var combIdxR3 = 0
    private var combIdxR4 = 0

    // Bitcrusher hold registers
    private var crushHoldL = 0f
    private var crushHoldR = 0f
    private var crushCounter = 0

    // Performance FX (Tape Stop & Roll)
    var isTapeStopActive = false
    private var tapeStopRate = 1.0f
    var isRollActive = false
    var rollDivisionSamples = (SAMPLE_RATE * 0.125f).toInt()
    private var rollBufferIndex = 0

    // Recording
    var isRecording = false
    private val recordedPcmStream = ByteArrayOutputStream()

    // VU Meter output for UI
    private val _vuMeterLeft = MutableStateFlow(0f)
    val vuMeterLeft = _vuMeterLeft.asStateFlow()
    private val _vuMeterRight = MutableStateFlow(0f)
    val vuMeterRight = _vuMeterRight.asStateFlow()

    // Real-Time Output Signal Waveform Buffer for UI Visualizer
    private val waveformPoints = 64
    private val waveDownsampleBuffer = FloatArray(waveformPoints)
    private val _masterWaveform = MutableStateFlow(FloatArray(waveformPoints))
    val masterWaveform = _masterWaveform.asStateFlow()

    // Real-Time 16-Band Spectrum Analysis Buffer for UI Equalizer Spectrum Visualizer
    private val spectrumBandsCount = 16
    private val spectrumBuffer = FloatArray(spectrumBandsCount)
    private val _masterSpectrum = MutableStateFlow(FloatArray(spectrumBandsCount))
    val masterSpectrum = _masterSpectrum.asStateFlow()

    // Active Polyphonic Voices
    private val activeVoices = CopyOnWriteArrayList<SynthVoice>()

    // Delay buffer
    private val delayBufferLeft = FloatArray(SAMPLE_RATE * 2)
    private val delayBufferRight = FloatArray(SAMPLE_RATE * 2)
    private var delayWritePos = 0

    // Roll circular capture buffer
    private val rollBufferLeft = FloatArray(SAMPLE_RATE)
    private val rollBufferRight = FloatArray(SAMPLE_RATE)

    // Master filter states
    private var filterL_x1 = 0f
    private var filterL_x2 = 0f
    private var filterL_y1 = 0f
    private var filterL_y2 = 0f
    private var filterR_x1 = 0f
    private var filterR_x2 = 0f
    private var filterR_y1 = 0f
    private var filterR_y2 = 0f

    // Pre-synthesized Splice Loops for instant slicing & auditioning
    val preloadedLoops = HashMap<String, FloatArray>()

    // Oboe Low-Latency C++ Engine
    val oboeEngine = OboeAudioEngine()
    var isOboeActive = false
        private set

    init {
        generatePresetSampleLoops()
        loadPresetsIntoOboe()
    }

    private fun loadPresetsIntoOboe() {
        if (!oboeEngine.isAvailable()) return
        try {
            // Synthesize and load core one-shots into Oboe native buffers
            oboeEngine.loadSampleBuffer(0, synthesizeMcpSound("kick"))
            oboeEngine.loadSampleBuffer(1, synthesizeMcpSound("snare"))
            oboeEngine.loadSampleBuffer(2, synthesizeMcpSound("clap"))
            oboeEngine.loadSampleBuffer(3, synthesizeMcpSound("hat"))
            oboeEngine.loadSampleBuffer(4, synthesizeMcpSound("open hat"))
            oboeEngine.loadSampleBuffer(5, synthesizeMcpSound("808 sub bass"))
            oboeEngine.loadSampleBuffer(6, synthesizeMcpSound("lofi keys chord"))
            oboeEngine.loadSampleBuffer(7, synthesizeMcpSound("synth lead"))

            // Also load 16 slices of default loop
            val lofiLoop = preloadedLoops["lofi_guitar"]
            if (lofiLoop != null && lofiLoop.isNotEmpty()) {
                val sliceLen = lofiLoop.size / 16
                for (s in 0 until 16) {
                    val start = (s * sliceLen).coerceIn(0, lofiLoop.size - 1)
                    val end = min(lofiLoop.size, start + sliceLen)
                    val sliceBuf = lofiLoop.copyOfRange(start, end)
                    oboeEngine.loadSampleBuffer(8 + s, sliceBuf)
                }
            }
        } catch (e: Exception) {
            Log.e("SchwungAudioEngine", "Error pre-loading samples into Oboe", e)
        }
    }

    fun start() {
        if (isRunning) return

        // 1. Attempt to start low-latency native Oboe engine
        if (oboeEngine.isAvailable()) {
            val oboeStarted = oboeEngine.start()
            if (oboeStarted) {
                isOboeActive = true
                Log.i("SchwungAudioEngine", "Oboe native C++ audio stream active (ultra low-latency)")
            }
        }
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSizeInBytes = max(minBufSize, BUFFER_SIZE * 4)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSizeInBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isRunning = true

            audioThread = Thread {
                renderAudioLoop()
            }.apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        } catch (e: Exception) {
            Log.e("SchwungAudioEngine", "Error starting AudioTrack", e)
        }
    }

    fun stop() {
        isRunning = false
        if (isOboeActive) {
            oboeEngine.stop()
            isOboeActive = false
        }
        try {
            audioThread?.join(500)
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e("SchwungAudioEngine", "Error stopping AudioTrack", e)
        }
    }

    fun triggerSound(
        recipe: String,
        velocity: Float = 0.8f,
        pitchSemitones: Float = 0f,
        decayMultiplier: Float = 1.0f,
        cutoff: Float = 20000f,
        chokeGroup: Int = 0,
        sliceIndex: Int = -1,
        loopKey: String = "lofi_guitar"
    ) {
        // If native Oboe stream is active, trigger low-latency native voice
        if (isOboeActive) {
            val sampleId = when {
                sliceIndex in 0..15 -> 8 + sliceIndex
                recipe.contains("kick") -> 0
                recipe.contains("snare") -> 1
                recipe.contains("clap") -> 2
                recipe.contains("hat_closed") -> 3
                recipe.contains("hat_open") -> 4
                recipe.contains("808") || recipe.contains("bass") -> 5
                recipe.contains("keys") -> 6
                recipe.contains("synth") || recipe.contains("lead") -> 7
                else -> -1
            }
            if (sampleId >= 0) {
                oboeEngine.triggerSample(sampleId, velocity, pitchSemitones, chokeGroup)
            }
        }
        // Handle choke group: choke previous voices in same group
        if (chokeGroup > 0) {
            for (voice in activeVoices) {
                if (voice.chokeGroup == chokeGroup) {
                    voice.startRelease(0.005f) // fast fade out
                }
            }
        }

        val voice = SynthVoice(
            recipe = recipe,
            velocity = velocity.coerceIn(0.1f, 1.2f),
            pitchSemitones = pitchSemitones,
            decayMultiplier = decayMultiplier,
            cutoff = cutoff,
            chokeGroup = chokeGroup,
            sliceIndex = sliceIndex,
            loopData = preloadedLoops[loopKey] ?: preloadedLoops.values.firstOrNull()
        )
        activeVoices.add(voice)
    }

    private fun renderAudioLoop() {
        val shortBuffer = ShortArray(BUFFER_SIZE * 2)
        val floatBufL = FloatArray(BUFFER_SIZE)
        val floatBufR = FloatArray(BUFFER_SIZE)

        while (isRunning) {
            floatBufL.fill(0f)
            floatBufR.fill(0f)

            // 1. Process and mix all active voices
            val iterator = activeVoices.iterator()
            while (iterator.hasNext()) {
                val voice = iterator.next()
                voice.render(floatBufL, floatBufR, BUFFER_SIZE)
                if (voice.isFinished) {
                    activeVoices.remove(voice)
                }
            }

            // 2. Performance Roll / Tape Stop FX
            if (isTapeStopActive) {
                tapeStopRate = max(0.02f, tapeStopRate - 0.035f)
            } else {
                tapeStopRate = min(1.0f, tapeStopRate + 0.08f)
            }

            // Apply roll if active
            if (isRollActive && rollDivisionSamples > 0) {
                for (i in 0 until BUFFER_SIZE) {
                    floatBufL[i] = rollBufferLeft[rollBufferIndex]
                    floatBufR[i] = rollBufferRight[rollBufferIndex]
                    rollBufferIndex = (rollBufferIndex + 1) % rollDivisionSamples
                }
            } else {
                // Record into roll buffer
                for (i in 0 until BUFFER_SIZE) {
                    rollBufferLeft[rollBufferIndex] = floatBufL[i]
                    rollBufferRight[rollBufferIndex] = floatBufR[i]
                    rollBufferIndex = (rollBufferIndex + 1) % rollBufferLeft.size
                }
            }

            // 3. Master FX: Delay & Feedback
            if (delayWet > 0.01f) {
                for (i in 0 until BUFFER_SIZE) {
                    val readPosL = (delayWritePos - delaySamples + delayBufferLeft.size) % delayBufferLeft.size
                    val readPosR = (delayWritePos - (delaySamples * 1.3f).toInt() + delayBufferRight.size) % delayBufferRight.size

                    val dL = delayBufferLeft[readPosL]
                    val dR = delayBufferRight[readPosR]

                    delayBufferLeft[delayWritePos] = floatBufL[i] + dL * delayFeedback
                    delayBufferRight[delayWritePos] = floatBufR[i] + dR * delayFeedback

                    floatBufL[i] = floatBufL[i] * (1f - delayWet * 0.4f) + dL * delayWet
                    floatBufR[i] = floatBufR[i] * (1f - delayWet * 0.4f) + dR * delayWet

                    delayWritePos = (delayWritePos + 1) % delayBufferLeft.size
                }
            }

            // 3b. Master FX: Reverb
            if (masterReverbWet > 0.01f) {
                applyMasterReverb(floatBufL, floatBufR, BUFFER_SIZE)
            }

            // 3c. Master FX: Bitcrusher (Bit depth & Sample Rate decimation)
            if (masterBitcrusherWet > 0.01f) {
                applyMasterBitcrusher(floatBufL, floatBufR, BUFFER_SIZE)
            }

            // 4. Master Low-Pass Filter (Biquad resonant filter)
            if (filterCutoffHz < 19500f) {
                applyMasterFilter(floatBufL, floatBufR, BUFFER_SIZE)
            }

            // 5. Saturation & Master Volume + Peak Metering
            var peakL = 0f
            var peakR = 0f

            var bufIdx = 0
            for (i in 0 until BUFFER_SIZE) {
                var sL = floatBufL[i] * masterVolume
                var sR = floatBufR[i] * masterVolume

                // Soft saturation / drive
                if (driveSaturation > 0.05f) {
                    val drive = 1f + driveSaturation * 3.5f
                    sL = tanh(sL * drive)
                    sR = tanh(sR * drive)
                }

                // Hard clamp to prevent digital wrap
                sL = sL.coerceIn(-1.0f, 1.0f)
                sR = sR.coerceIn(-1.0f, 1.0f)

                peakL = max(peakL, abs(sL))
                peakR = max(peakR, abs(sR))

                val shortL = (sL * 32767f).toInt().toShort()
                val shortR = (sR * 32767f).toInt().toShort()

                shortBuffer[bufIdx++] = shortL
                shortBuffer[bufIdx++] = shortR
            }

            _vuMeterLeft.value = peakL
            _vuMeterRight.value = peakR

            // Real-Time Output Signal Waveform Sampling
            val stepSize = BUFFER_SIZE / waveformPoints
            for (p in 0 until waveformPoints) {
                val sampleIdx = (p * stepSize).coerceIn(0, BUFFER_SIZE - 1)
                val sMono = (floatBufL[sampleIdx] + floatBufR[sampleIdx]) * 0.5f * masterVolume
                waveDownsampleBuffer[p] = sMono.coerceIn(-1.0f, 1.0f)
            }
            _masterWaveform.value = waveDownsampleBuffer.clone()

            // Real-Time 16-Band Frequency Spectrum Analysis
            val bandChunkSize = BUFFER_SIZE / spectrumBandsCount
            for (b in 0 until spectrumBandsCount) {
                var sumSq = 0f
                val startIdx = b * bandChunkSize
                for (s in startIdx until (startIdx + bandChunkSize)) {
                    val sampleMono = (floatBufL[s] + floatBufR[s]) * 0.5f * masterVolume
                    sumSq += sampleMono * sampleMono
                }
                val rms = sqrt(sumSq / bandChunkSize)
                spectrumBuffer[b] = max(rms * 2.8f, spectrumBuffer[b] * 0.78f).coerceIn(0f, 1f)
            }
            _masterSpectrum.value = spectrumBuffer.clone()

            // 6. Audio Recording stream
            if (isRecording) {
                val byteBuf = ByteBuffer.allocate(shortBuffer.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                for (s in shortBuffer) {
                    byteBuf.putShort(s)
                }
                recordedPcmStream.write(byteBuf.array())
            }

            // 7. Write to Android AudioTrack
            audioTrack?.write(shortBuffer, 0, shortBuffer.size)
        }
    }

    private fun applyMasterFilter(bufL: FloatArray, bufR: FloatArray, size: Int) {
        val omega = 2f * Math.PI.toFloat() * filterCutoffHz.coerceIn(80f, 19000f) / SAMPLE_RATE
        val sinO = sin(omega)
        val cosO = cos(omega)
        val alpha = sinO / (2f * filterResonance.coerceIn(0.5f, 5.0f))

        val b0 = (1f - cosO) / 2f
        val b1 = 1f - cosO
        val b2 = (1f - cosO) / 2f
        val a0 = 1f + alpha
        val a1 = -2f * cosO
        val a2 = 1f - alpha

        val invA0 = 1f / a0
        val nb0 = b0 * invA0
        val nb1 = b1 * invA0
        val nb2 = b2 * invA0
        val na1 = a1 * invA0
        val na2 = a2 * invA0

        for (i in 0 until size) {
            val inL = bufL[i]
            val outL = nb0 * inL + nb1 * filterL_x1 + nb2 * filterL_x2 - na1 * filterL_y1 - na2 * filterL_y2
            filterL_x2 = filterL_x1
            filterL_x1 = inL
            filterL_y2 = filterL_y1
            filterL_y1 = outL
            bufL[i] = outL

            val inR = bufR[i]
            val outR = nb0 * inR + nb1 * filterR_x1 + nb2 * filterR_x2 - na1 * filterR_y1 - na2 * filterR_y2
            filterR_x2 = filterR_x1
            filterR_x1 = inR
            filterR_y2 = filterR_y1
            filterR_y1 = outR
            bufR[i] = outR
        }
    }

    private fun applyMasterReverb(bufL: FloatArray, bufR: FloatArray, size: Int) {
        val wet = masterReverbWet.coerceIn(0f, 1f)
        val dry = 1f - wet * 0.5f
        val feedback = masterReverbRoomSize.coerceIn(0.1f, 0.95f)

        for (i in 0 until size) {
            val inL = bufL[i]
            val inR = bufR[i]

            // 4 parallel combs for Left channel
            val outL1 = combL1[combIdxL1]
            combL1[combIdxL1] = inL + outL1 * feedback
            combIdxL1 = (combIdxL1 + 1) % combL1.size

            val outL2 = combL2[combIdxL2]
            combL2[combIdxL2] = inL + outL2 * feedback
            combIdxL2 = (combIdxL2 + 1) % combL2.size

            val outL3 = combL3[combIdxL3]
            combL3[combIdxL3] = inL + outL3 * feedback
            combIdxL3 = (combIdxL3 + 1) % combL3.size

            val outL4 = combL4[combIdxL4]
            combL4[combIdxL4] = inL + outL4 * feedback
            combIdxL4 = (combIdxL4 + 1) % combL4.size

            val revOutL = (outL1 + outL2 + outL3 + outL4) * 0.25f

            // 4 parallel combs for Right channel
            val outR1 = combR1[combIdxR1]
            combR1[combIdxR1] = inR + outR1 * feedback
            combIdxR1 = (combIdxR1 + 1) % combR1.size

            val outR2 = combR2[combIdxR2]
            combR2[combIdxR2] = inR + outR2 * feedback
            combIdxR2 = (combIdxR2 + 1) % combR2.size

            val outR3 = combR3[combIdxR3]
            combR3[combIdxR3] = inR + outR3 * feedback
            combIdxR3 = (combIdxR3 + 1) % combR3.size

            val outR4 = combR4[combIdxR4]
            combR4[combIdxR4] = inR + outR4 * feedback
            combIdxR4 = (combIdxR4 + 1) % combR4.size

            val revOutR = (outR1 + outR2 + outR3 + outR4) * 0.25f

            bufL[i] = inL * dry + revOutL * wet
            bufR[i] = inR * dry + revOutR * wet
        }
    }

    private fun applyMasterBitcrusher(bufL: FloatArray, bufR: FloatArray, size: Int) {
        val wet = masterBitcrusherWet.coerceIn(0f, 1f)
        val dry = 1f - wet
        val step = 1.0f / (1 shl masterBitcrusherBits.coerceIn(2, 16))
        val downsample = masterBitcrusherDownsample.coerceIn(1, 32)

        for (i in 0 until size) {
            if (crushCounter % downsample == 0) {
                crushHoldL = (kotlin.math.round(bufL[i] / step) * step).coerceIn(-1f, 1f)
                crushHoldR = (kotlin.math.round(bufR[i] / step) * step).coerceIn(-1f, 1f)
            }
            crushCounter++

            bufL[i] = bufL[i] * dry + crushHoldL * wet
            bufR[i] = bufR[i] * dry + crushHoldR * wet
        }
    }

    fun setMasterReverb(wet: Float, roomSize: Float = 0.7f) {
        masterReverbWet = wet.coerceIn(0f, 1f)
        masterReverbRoomSize = roomSize.coerceIn(0.1f, 0.95f)
    }

    fun setMasterBitcrusher(wet: Float, bits: Int = 8, downsample: Int = 2) {
        masterBitcrusherWet = wet.coerceIn(0f, 1f)
        masterBitcrusherBits = bits.coerceIn(2, 16)
        masterBitcrusherDownsample = downsample.coerceIn(1, 32)
    }

    // Recording Export
    fun startRecording() {
        recordedPcmStream.reset()
        isRecording = true
    }

    fun stopRecordingAndSave(destinationFile: File): Boolean {
        isRecording = false
        val pcmData = recordedPcmStream.toByteArray()
        if (pcmData.isEmpty()) return false

        try {
            FileOutputStream(destinationFile).use { fos ->
                writeWavHeader(fos, pcmData.size)
                fos.write(pcmData)
            }
            return true
        } catch (e: Exception) {
            Log.e("SchwungAudioEngine", "Error saving WAV file", e)
            return false
        }
    }

    private fun writeWavHeader(out: FileOutputStream, pcmSize: Int) {
        val totalDataLen = pcmSize + 36
        val byteRate = SAMPLE_RATE * 2 * 2 // 16 bit stereo = 4 bytes per sample
        val header = ByteArray(44)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        buf.put("RIFF".toByteArray())
        buf.putInt(totalDataLen)
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray())
        buf.putInt(16) // Subchunk1Size for PCM
        buf.putShort(1) // AudioFormat 1 = PCM
        buf.putShort(2) // NumChannels = 2
        buf.putInt(SAMPLE_RATE)
        buf.putInt(byteRate)
        buf.putShort(4) // BlockAlign = 2 * 16/8
        buf.putShort(16) // BitsPerSample
        buf.put("data".toByteArray())
        buf.putInt(pcmSize)

        out.write(header)
    }

    // Pre-generate Splice audio loops so the user has immediate waveforms to chop & slice
    private fun generatePresetSampleLoops() {
        val loopLengthSamples = SAMPLE_RATE * 4 // 4-second loop at ~120 BPM (2 bars)

        // 1. Lo-Fi Hip Hop Guitar & Rhodes Chill loop
        val lofiLoop = FloatArray(loopLengthSamples)
        val chords = floatArrayOf(261.63f, 329.63f, 392.00f, 493.88f) // Cmaj7, Am7, Dm7, G7
        for (i in 0 until loopLengthSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val barPos = (t % 4.0f) / 4.0f
            val chordIdx = (barPos * 4).toInt().coerceIn(0, 3)
            val baseFreq = chords[chordIdx]

            // Soft rhodes harmonic bell + warm vinyl hiss
            val v1 = sin(2f * Math.PI.toFloat() * baseFreq * t) * exp(-2.5f * ((t % 1.0f)))
            val v2 = sin(2f * Math.PI.toFloat() * baseFreq * 1.5f * t) * 0.4f * exp(-3f * ((t % 1.0f)))
            val v3 = sin(2f * Math.PI.toFloat() * baseFreq * 2.0f * t) * 0.2f
            val vinylCrackle = (Math.random().toFloat() - 0.5f) * 0.035f

            lofiLoop[i] = ((v1 + v2 + v3) * 0.5f + vinylCrackle).coerceIn(-1f, 1f)
        }
        preloadedLoops["lofi_guitar"] = lofiLoop

        // 2. Trap 808 & Drill Sub Bassline loop
        val trap808Loop = FloatArray(loopLengthSamples)
        val bassNotes = floatArrayOf(41.20f, 43.65f, 36.71f, 48.99f) // E1, F1, D1, G1
        for (i in 0 until loopLengthSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val noteIdx = ((t / 4.0f) * 4).toInt().coerceIn(0, 3)
            val f = bassNotes[noteIdx]
            val env = exp(-0.8f * (t % 1.0f))
            val sub = sin(2f * Math.PI.toFloat() * f * t)
            val dist = tanh(sub * 2.2f)
            trap808Loop[i] = (dist * env * 0.85f).coerceIn(-1f, 1f)
        }
        preloadedLoops["trap_808"] = trap808Loop

        // 3. Neon Retro Synthwave Arpeggio loop
        val synthwaveLoop = FloatArray(loopLengthSamples)
        val arpNotes = floatArrayOf(220f, 261.63f, 329.63f, 440f, 392f, 329.63f, 261.63f, 196f)
        for (i in 0 until loopLengthSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val step = ((t * 8) % 8).toInt()
            val f = arpNotes[step]
            val saw = ((2f * (f * t - floor(f * t + 0.5f)))) * exp(-4f * ((t * 8) % 1f))
            synthwaveLoop[i] = (saw * 0.6f).coerceIn(-1f, 1f)
        }
        preloadedLoops["neon_synth"] = synthwaveLoop

        // 4. Soul / Vocal Chop Melodic loop
        val vocalLoop = FloatArray(loopLengthSamples)
        for (i in 0 until loopLengthSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val beat = (t % 1.0f)
            val freq = 349.23f + sin(t * 12f) * 15f // F4 with vibrato
            val formant = sin(2f * Math.PI.toFloat() * freq * t) *
                    sin(2f * Math.PI.toFloat() * 1200f * t) * exp(-2.2f * beat)
            vocalLoop[i] = (formant * 0.7f).coerceIn(-1f, 1f)
        }
        preloadedLoops["vocal_soul"] = vocalLoop
    }

    // Dynamic Splice MCP Sound Synthesizer:
    // Called when the user types prompts like "Fat boom bap punchy kick" or "chill ambient chime"
    fun synthesizeMcpSound(prompt: String): FloatArray {
        val lower = prompt.lowercase()
        val durationSamples = (SAMPLE_RATE * 1.5f).toInt()
        val result = FloatArray(durationSamples)

        val isKick = lower.contains("kick") || lower.contains("808") || lower.contains("sub") || lower.contains("thump")
        val isSnare = lower.contains("snare") || lower.contains("clap") || lower.contains("crack") || lower.contains("snap")
        val isHiHat = lower.contains("hat") || lower.contains("cymbal") || lower.contains("shaker")
        val isVocal = lower.contains("vocal") || lower.contains("voice") || lower.contains("chop")

        for (i in 0 until durationSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val s = when {
                isKick -> {
                    val f = 160f * exp(-32f * t) + 42f
                    val click = if (t < 0.005f) (Math.random().toFloat() - 0.5f) * 0.8f else 0f
                    tanh(sin(2f * Math.PI.toFloat() * f * t) * 1.8f + click) * exp(-4.5f * t)
                }
                isSnare -> {
                    val body = sin(2f * Math.PI.toFloat() * 185f * t) * exp(-22f * t)
                    val noise = (Math.random().toFloat() - 0.5f) * exp(-12f * t)
                    (body * 0.6f + noise * 0.8f)
                }
                isHiHat -> {
                    val noise = (Math.random().toFloat() - 0.5f)
                    val metallic = sin(2f * Math.PI.toFloat() * 7800f * t)
                    (noise * 0.7f + metallic * 0.3f) * exp(-35f * t)
                }
                isVocal -> {
                    val form1 = sin(2f * Math.PI.toFloat() * 440f * t)
                    val form2 = sin(2f * Math.PI.toFloat() * 1400f * t) * 0.5f
                    (form1 + form2) * exp(-3.5f * t)
                }
                else -> {
                    // Melodic pluck / synth
                    val f = 392f // G4
                    val saw = (2f * (f * t - floor(f * t + 0.5f)))
                    val sub = sin(2f * Math.PI.toFloat() * (f / 2) * t) * 0.5f
                    (saw * 0.5f + sub) * exp(-5f * t)
                }
            }
            result[i] = s.coerceIn(-1f, 1f)
        }
        return result
    }
}

// Inner Synth Voice: handles sound synthesis for each individual pad or slice trigger
class SynthVoice(
    val recipe: String,
    val velocity: Float,
    val pitchSemitones: Float,
    val decayMultiplier: Float,
    val cutoff: Float,
    val chokeGroup: Int,
    val sliceIndex: Int = -1,
    val loopData: FloatArray? = null
) {
    var isFinished = false
    private var samplePos = 0
    private var isReleasing = false
    private var releaseStep = 0
    private var releaseTotalSamples = 1

    private val pitchRatio = 2f.pow(pitchSemitones / 12f)
    private var phase = 0.0

    // Slice range if slicing
    private var sliceStartSample = 0
    private var sliceEndSample = 0

    init {
        if (sliceIndex >= 0 && loopData != null && loopData.isNotEmpty()) {
            val total = loopData.size
            val sliceLen = total / 16
            sliceStartSample = (sliceIndex * sliceLen).coerceIn(0, total - 1)
            sliceEndSample = min(total, sliceStartSample + sliceLen)
            samplePos = sliceStartSample
        }
    }

    fun startRelease(seconds: Float) {
        if (!isReleasing) {
            isReleasing = true
            releaseStep = 0
            releaseTotalSamples = max(1, (SchwungAudioEngine.SAMPLE_RATE * seconds).toInt())
        }
    }

    fun render(outL: FloatArray, outR: FloatArray, bufferSize: Int) {
        if (isFinished) return

        val sr = SchwungAudioEngine.SAMPLE_RATE.toFloat()

        for (i in 0 until bufferSize) {
            val t = samplePos / sr
            var sample = 0f

            when {
                // Slice trigger
                sliceIndex >= 0 && loopData != null -> {
                    if (samplePos >= sliceEndSample || samplePos >= loopData.size) {
                        isFinished = true
                        break
                    }
                    sample = loopData[samplePos] * velocity
                    samplePos++
                }

                recipe.startsWith("kick") -> {
                    // Kick drum sine drop + transient click
                    val f = (180f * exp(-38f * t) + 45f) * pitchRatio
                    phase += (2.0 * Math.PI * f) / sr
                    val click = if (t < 0.005f) (Math.random().toFloat() - 0.5f) * 0.7f else 0f
                    val env = exp(-7f * (1f / decayMultiplier) * t)
                    val s = sin(phase).toFloat() * env + click
                    sample = tanh(s * 1.5f) * velocity
                    samplePos++
                    if (env < 0.002f || t > 1.2f) isFinished = true
                }

                recipe.startsWith("snare") -> {
                    // Snare: tonal sine body + white noise burst
                    val f = 195f * pitchRatio
                    phase += (2.0 * Math.PI * f) / sr
                    val body = sin(phase).toFloat() * exp(-24f * t)
                    val noise = (Math.random().toFloat() - 0.5f) * exp(-14f * (1f / decayMultiplier) * t)
                    val env = exp(-11f * (1f / decayMultiplier) * t)
                    sample = (body * 0.5f + noise * 0.85f) * env * velocity
                    samplePos++
                    if (env < 0.002f || t > 1.0f) isFinished = true
                }

                recipe.startsWith("clap") -> {
                    // Clap: 3 micro-transient claps + tail
                    val env = exp(-12f * (1f / decayMultiplier) * t)
                    var c = (Math.random().toFloat() - 0.5f)
                    if (t < 0.012f || (t in 0.022f..0.034f) || (t in 0.044f..0.056f)) {
                        c *= 1.4f
                    }
                    sample = c * env * velocity
                    samplePos++
                    if (env < 0.002f || t > 0.8f) isFinished = true
                }

                recipe.startsWith("hat_closed") -> {
                    // Crisp closed hi-hat
                    val noise = (Math.random().toFloat() - 0.5f)
                    val metallic = sin(samplePos * 1.2).toFloat() * 0.3f
                    val env = exp(-48f * (1f / decayMultiplier) * t)
                    sample = (noise + metallic) * env * velocity * 0.7f
                    samplePos++
                    if (env < 0.002f || t > 0.3f) isFinished = true
                }

                recipe.startsWith("hat_open") -> {
                    // Open hi-hat with sizzle
                    val noise = (Math.random().toFloat() - 0.5f)
                    val metallic = sin(samplePos * 1.3).toFloat() * 0.35f
                    val env = exp(-9f * (1f / decayMultiplier) * t)
                    sample = (noise + metallic) * env * velocity * 0.8f
                    samplePos++
                    if (env < 0.002f || t > 1.4f) isFinished = true
                }

                recipe.startsWith("808_bass") || recipe.startsWith("sub_bass") -> {
                    // Deep 808 sub bass with warm harmonic saturation
                    val f = 43.65f * pitchRatio // F1
                    phase += (2.0 * Math.PI * f) / sr
                    val sub = sin(phase).toFloat()
                    val harm = sin(phase * 2.0).toFloat() * 0.25f
                    val env = exp(-2.2f * (1f / decayMultiplier) * t)
                    val sat = tanh((sub + harm) * 2.0f)
                    sample = sat * env * velocity * 0.9f
                    samplePos++
                    if (env < 0.002f || t > 2.5f) isFinished = true
                }

                recipe.startsWith("acid_303") -> {
                    // Resonant Acid Sawtooth bass
                    val f = 65.41f * pitchRatio // C2
                    phase += (2.0 * Math.PI * f) / sr
                    val saw = (2.0 * (phase / (2 * Math.PI) - floor(phase / (2 * Math.PI) + 0.5))).toFloat()
                    val filterEnv = exp(-10f * t)
                    val modSaw = saw * (0.4f + filterEnv * 0.6f)
                    val env = exp(-4f * (1f / decayMultiplier) * t)
                    sample = tanh(modSaw * 2.2f) * env * velocity
                    samplePos++
                    if (env < 0.002f || t > 1.8f) isFinished = true
                }

                recipe.startsWith("lofi_keys") -> {
                    // Mellow vintage Rhodes electric piano
                    val f = 261.63f * pitchRatio // C4
                    phase += (2.0 * Math.PI * f) / sr
                    val s1 = sin(phase).toFloat()
                    val s2 = sin(phase * 2.0).toFloat() * 0.35f
                    val s3 = sin(phase * 3.0).toFloat() * 0.15f
                    val tremolo = 1.0f + 0.15f * sin(t * 30f)
                    val env = exp(-3.2f * (1f / decayMultiplier) * t)
                    sample = (s1 + s2 + s3) * tremolo * env * velocity * 0.75f
                    samplePos++
                    if (env < 0.002f || t > 2.2f) isFinished = true
                }

                recipe.startsWith("synth_lead") -> {
                    // Dual detuned Sawtooth synth lead
                    val f = 329.63f * pitchRatio // E4
                    phase += (2.0 * Math.PI * f) / sr
                    val saw1 = (2.0 * (phase / (2 * Math.PI) - floor(phase / (2 * Math.PI) + 0.5))).toFloat()
                    val saw2 = (2.0 * ((phase * 1.008) / (2 * Math.PI) - floor((phase * 1.008) / (2 * Math.PI) + 0.5))).toFloat()
                    val env = exp(-3.8f * (1f / decayMultiplier) * t)
                    sample = ((saw1 + saw2) * 0.5f) * env * velocity * 0.75f
                    samplePos++
                    if (env < 0.002f || t > 2.0f) isFinished = true
                }

                recipe.startsWith("vocal_chop") -> {
                    // Vocal formant chop
                    val f = 293.66f * pitchRatio // D4
                    phase += (2.0 * Math.PI * f) / sr
                    val formant = sin(phase).toFloat() * sin(phase * 4.2).toFloat()
                    val env = exp(-4.2f * (1f / decayMultiplier) * t)
                    sample = formant * env * velocity * 0.85f
                    samplePos++
                    if (env < 0.002f || t > 1.8f) isFinished = true
                }

                else -> {
                    // Default percussion/click
                    val f = 440f * pitchRatio
                    phase += (2.0 * Math.PI * f) / sr
                    val env = exp(-18f * (1f / decayMultiplier) * t)
                    sample = sin(phase).toFloat() * env * velocity
                    samplePos++
                    if (env < 0.002f || t > 0.8f) isFinished = true
                }
            }

            // Apply rapid fade-out if choked
            if (isReleasing) {
                val relFactor = 1f - (releaseStep.toFloat() / releaseTotalSamples)
                sample *= relFactor.coerceIn(0f, 1f)
                releaseStep++
                if (releaseStep >= releaseTotalSamples) {
                    isFinished = true
                    break
                }
            }

            outL[i] += sample
            outR[i] += sample
        }
    }
}
