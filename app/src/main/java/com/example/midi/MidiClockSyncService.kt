package com.example.midi

import android.content.Context
import android.media.midi.MidiInputPort
import android.media.midi.MidiReceiver
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MidiClockSyncService(private val context: Context) {

    companion object {
        private const val TAG = "MidiClockSyncService"
        const val MIDI_CLOCK: Byte = 0xF8.toByte()
        const val MIDI_START: Byte = 0xFA.toByte()
        const val MIDI_CONTINUE: Byte = 0xFB.toByte()
        const val MIDI_STOP: Byte = 0xFC.toByte()
        const val PPQN = 24 // 24 pulses per quarter note (standard MIDI clock)
        const val TICKS_PER_SIXTEENTH = 6 // 24 / 4 = 6 ticks per 16th note step
    }

    enum class SyncMode {
        PIONEER_IS_MASTER, // App slaves to Pioneer CDJ/DJM USB MIDI clock
        APP_IS_MASTER       // App sends MIDI clock to Pioneer hardware
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var clockSenderJob: Job? = null

    // State
    private val _syncMode = MutableStateFlow(SyncMode.PIONEER_IS_MASTER)
    val syncMode = _syncMode.asStateFlow()

    private val _isClockLocked = MutableStateFlow(true)
    val isClockLocked = _isClockLocked.asStateFlow()

    private val _detectedPioneerBpm = MutableStateFlow(126.0f)
    val detectedPioneerBpm = _detectedPioneerBpm.asStateFlow()

    private val _clockTickCount = MutableStateFlow(0L)
    val clockTickCount = _clockTickCount.asStateFlow()

    // Callbacks to SchwungAudioEngine / Sequencer
    var onStepTriggeredFromMidiClock: ((stepIndex: Int) -> Unit)? = null
    var onMidiStartReceived: (() -> Unit)? = null
    var onMidiStopReceived: (() -> Unit)? = null
    var onBpmCalculated: ((bpm: Float) -> Unit)? = null

    // Tracking variables for incoming clock
    private var lastTickTimestampNs: Long = 0L
    private val tickIntervalsNs = LongArray(24)
    private var intervalIndex = 0
    private var incomingTickCounter = 0
    private var currentStep = 0

    // Hardware MIDI Output Port for sending clock
    var hardwareMidiOutputPort: MidiInputPort? = null

    // MIDI Receiver that can be attached to Android MidiManager or Pioneer USB input port
    val midiClockReceiver = object : MidiReceiver() {
        override fun onSend(msg: ByteArray?, offset: Int, count: Int, timestamp: Long) {
            if (msg == null) return
            for (i in offset until (offset + count)) {
                processSingleMidiByte(msg[i])
            }
        }
    }

    fun processSingleMidiByte(byte: Byte) {
        val nowNs = System.nanoTime()
        when (byte) {
            MIDI_CLOCK -> {
                _clockTickCount.value++
                if (_syncMode.value == SyncMode.PIONEER_IS_MASTER) {
                    handleIncomingMidiClockTick(nowNs)
                }
            }
            MIDI_START -> {
                Log.i(TAG, "Pioneer MIDI START received (0xFA)")
                incomingTickCounter = 0
                currentStep = 0
                _isClockLocked.value = true
                onMidiStartReceived?.invoke()
            }
            MIDI_CONTINUE -> {
                Log.i(TAG, "Pioneer MIDI CONTINUE received (0xFB)")
                _isClockLocked.value = true
                onMidiStartReceived?.invoke()
            }
            MIDI_STOP -> {
                Log.i(TAG, "Pioneer MIDI STOP received (0xFC)")
                _isClockLocked.value = false
                onMidiStopReceived?.invoke()
            }
        }
    }

    private fun handleIncomingMidiClockTick(nowNs: Long) {
        if (lastTickTimestampNs > 0) {
            val deltaNs = nowNs - lastTickTimestampNs
            if (deltaNs in 1_000_000..500_000_000) { // filter outliers (between 1ms and 500ms)
                tickIntervalsNs[intervalIndex] = deltaNs
                intervalIndex = (intervalIndex + 1) % tickIntervalsNs.size

                // Calculate average tick interval over a full quarter note (24 ticks)
                var sumNs = 0L
                var validCount = 0
                for (interval in tickIntervalsNs) {
                    if (interval > 0) {
                        sumNs += interval
                        validCount++
                    }
                }

                if (validCount >= 12) {
                    val avgTickNs = sumNs.toDouble() / validCount
                    // BPM = (60 * 1,000,000,000) / (avgTickNs * 24)
                    val calculatedBpm = (60.0 * 1_000_000_000.0) / (avgTickNs * PPQN.toDouble())
                    val roundedBpm = (calculatedBpm * 10).toInt() / 10.0f
                    if (roundedBpm in 40.0f..250.0f && kotlin.math.abs(_detectedPioneerBpm.value - roundedBpm) > 0.1f) {
                        _detectedPioneerBpm.value = roundedBpm
                        onBpmCalculated?.invoke(roundedBpm)
                    }
                }
            }
        }
        lastTickTimestampNs = nowNs

        // Advance sequencer step every 6 ticks (1/16th note step)
        incomingTickCounter++
        if (incomingTickCounter >= TICKS_PER_SIXTEENTH) {
            incomingTickCounter = 0
            val stepToFire = currentStep
            currentStep = (currentStep + 1) % 16
            onStepTriggeredFromMidiClock?.invoke(stepToFire)
        }
    }

    // Generator for when SchwungLive is Master
    fun startClockSender(bpm: Float) {
        stopClockSender()
        _syncMode.value = SyncMode.APP_IS_MASTER
        sendMidiByte(MIDI_START)

        clockSenderJob = coroutineScope.launch {
            while (isActive) {
                val tickIntervalMs = (60000.0f / bpm / PPQN.toFloat()).toLong()
                sendMidiByte(MIDI_CLOCK)
                delay(tickIntervalMs.coerceAtLeast(1L))
            }
        }
    }

    fun stopClockSender() {
        clockSenderJob?.cancel()
        clockSenderJob = null
        sendMidiByte(MIDI_STOP)
    }

    private fun sendMidiByte(b: Byte) {
        try {
            hardwareMidiOutputPort?.send(byteArrayOf(b), 0, 1)
        } catch (_: Exception) {}
    }

    fun setSyncMode(mode: SyncMode) {
        _syncMode.value = mode
        if (mode == SyncMode.PIONEER_IS_MASTER) {
            stopClockSender()
        }
    }
}
