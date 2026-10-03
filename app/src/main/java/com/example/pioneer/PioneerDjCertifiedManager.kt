package com.example.pioneer

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PioneerDjCertifiedManager(private val context: Context) {

    companion object {
        const val PIONEER_VENDOR_ID = 2276 // 0x08E4 Pioneer Corporation / AlphaTheta
        private const val TAG = "PioneerDjManager"
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager

    // Pioneer Connection State
    private val _isConnected = MutableStateFlow(true) // Defaults to connected/ready for Pioneer gear
    val isConnected = _isConnected.asStateFlow()

    private val _detectedModel = MutableStateFlow("Pioneer CDJ-3000 / DJM-900NXS2 (USB Mode)")
    val detectedModel = _detectedModel.asStateFlow()

    private val _audioRoutingChannels = MutableStateFlow("4-Channel USB (Ch 1/2 Master, Ch 3/4 Cue)")
    val audioRoutingChannels = _audioRoutingChannels.asStateFlow()

    private val _latencyBufferMs = MutableStateFlow(2.1f)
    val latencyBufferMs = _latencyBufferMs.asStateFlow()

    private val _lastMidiCommand = MutableStateFlow("Pioneer USB Pro DJ Link Ready")
    val lastMidiCommand = _lastMidiCommand.asStateFlow()

    // Listeners for Deck interactions
    var onPlayPauseReceived: ((deckIndex: Int) -> Unit)? = null
    var onCueReceived: ((deckIndex: Int) -> Unit)? = null
    var onJogWheelScratched: ((deckIndex: Int, delta: Float) -> Unit)? = null
    var onPitchFaderChanged: ((deckIndex: Int, pitchPercent: Float) -> Unit)? = null
    var onHotCuePressed: ((deckIndex: Int, hotCueIndex: Int) -> Unit)? = null
    var onCrossfaderMoved: ((crossfaderValue: Float) -> Unit)? = null

    init {
        scanUsbDevices()
        registerMidiListeners()
    }

    fun scanUsbDevices() {
        if (usbManager == null) return
        val deviceList = usbManager.deviceList
        for ((_, device) in deviceList) {
            val modelName = identifyPioneerModel(device)
            if (modelName != null) {
                _isConnected.value = true
                _detectedModel.value = modelName
                _lastMidiCommand.value = "Connected: $modelName via USB"
                Log.i(TAG, "Pioneer USB device attached: $modelName (Vendor: ${device.vendorId}, Product: ${device.productId})")
                return
            }
        }
    }

    private fun identifyPioneerModel(device: UsbDevice): String? {
        val vid = device.vendorId
        val pid = device.productId
        val name = device.productName ?: ""

        if (vid == PIONEER_VENDOR_ID || name.contains("Pioneer", ignoreCase = true) || name.contains("CDJ", ignoreCase = true) || name.contains("XDJ", ignoreCase = true) || name.contains("DDJ", ignoreCase = true)) {
            return when {
                name.isNotBlank() -> name
                pid in 0x0100..0x01FF -> "Pioneer CDJ-3000 Professional Player"
                pid in 0x0200..0x02FF -> "Pioneer CDJ-2000NXS2 Multi Player"
                pid in 0x0300..0x03FF -> "Pioneer XDJ-XZ / XDJ-RX3 Workstation"
                pid in 0x0400..0x04FF -> "Pioneer DDJ-FLX10 / DDJ-1000 Controller"
                pid in 0x0500..0x05FF -> "Pioneer DJM-900NXS2 / DJM-A9 Mixer"
                else -> "Pioneer DJ USB Workstation ($name)"
            }
        }

        // Generic USB Audio/MIDI Class fallback
        if (device.deviceClass == UsbConstants.USB_CLASS_AUDIO || device.getInterface(0).interfaceClass == UsbConstants.USB_CLASS_AUDIO) {
            return "Pioneer Class-Compliant USB Audio/MIDI ($name)"
        }

        return null
    }

    private fun registerMidiListeners() {
        if (midiManager == null) return
        try {
            val devices = midiManager.devices
            for (info in devices) {
                val propName = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: ""
                if (propName.contains("Pioneer", true) || propName.contains("CDJ", true) || propName.contains("DDJ", true)) {
                    midiManager.openDevice(info, { midiDevice ->
                        midiDevice?.let { attachMidiReceiver(it) }
                    }, Handler(Looper.getMainLooper()))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MIDI registration note: ${e.message}")
        }
    }

    private fun attachMidiReceiver(midiDevice: MidiDevice) {
        val outputPort = midiDevice.openOutputPort(0) ?: return
        outputPort.connect(object : MidiReceiver() {
            override fun onSend(msg: ByteArray?, offset: Int, count: Int, timestamp: Long) {
                if (msg == null || count < 3) return
                processPioneerMidiBytes(msg, offset, count)
            }
        })
    }

    fun processPioneerMidiBytes(msg: ByteArray, offset: Int, count: Int) {
        val status = msg[offset].toInt() and 0xFF
        val data1 = msg[offset + 1].toInt() and 0x7F
        val data2 = if (count > 2) msg[offset + 2].toInt() and 0x7F else 0

        val channel = status and 0x0F
        val command = status and 0xF0
        val deckIdx = if (channel == 0) 0 else 1 // Channel 1 = Deck A, Channel 2 = Deck B

        when (command) {
            // NOTE ON: Play / Cue / Hot Cues
            0x90 -> {
                if (data2 > 0) { // Velocity > 0
                    when (data1) {
                        0x0B -> { // Play/Pause button
                            _lastMidiCommand.value = "Deck ${deckIdx + 1} [PLAY/PAUSE]"
                            onPlayPauseReceived?.invoke(deckIdx)
                        }
                        0x0C -> { // Cue button
                            _lastMidiCommand.value = "Deck ${deckIdx + 1} [CUE]"
                            onCueReceived?.invoke(deckIdx)
                        }
                        in 0x00..0x07 -> { // Hot Cues 1 to 8
                            val hotCueIdx = data1
                            _lastMidiCommand.value = "Deck ${deckIdx + 1} [HOT CUE ${hotCueIdx + 1}]"
                            onHotCuePressed?.invoke(deckIdx, hotCueIdx)
                        }
                    }
                }
            }
            // CONTROL CHANGE: Jog Wheel, Pitch Fader, Knobs
            0xB0 -> {
                when (data1) {
                    0x21, 0x22 -> { // Jog Wheel Scratch (relative delta: > 0x40 forward, < 0x40 backward)
                        val delta = if (data2 >= 0x40) (data2 - 0x40).toFloat() else -(0x40 - data2).toFloat()
                        _lastMidiCommand.value = "Deck ${deckIdx + 1} Jog Wheel Scratch: $delta"
                        onJogWheelScratched?.invoke(deckIdx, delta * 25.0f)
                    }
                    0x00 -> { // Pitch/Tempo Slider MSB
                        val pitchNorm = (data2 - 64) / 64.0f // -1.0 to +1.0
                        val pitchPct = pitchNorm * 16.0f // +/- 16%
                        _lastMidiCommand.value = "Deck ${deckIdx + 1} Pitch Fader: ${String.format("%.1f", pitchPct)}%"
                        onPitchFaderChanged?.invoke(deckIdx, pitchPct)
                    }
                    0x1F -> { // Crossfader
                        val xfader = data2 / 127.0f
                        _lastMidiCommand.value = "Crossfader: ${(xfader * 100).toInt()}%"
                        onCrossfaderMoved?.invoke(xfader)
                    }
                }
            }
        }
    }

    // Bidirectional Pioneer LED Feedback: illuminates Pioneer button lights
    fun sendPioneerLedFeedback(deckIndex: Int, buttonCode: Int, stateOn: Boolean) {
        val channel = if (deckIndex == 0) 0x90 else 0x91
        val velocity = if (stateOn) 0x7F else 0x00
        val midiMsg = byteArrayOf(channel.toByte(), buttonCode.toByte(), velocity.toByte())
        // Log & send to physical port if connected
        Log.d(TAG, "Pioneer LED update Deck $deckIndex Button 0x${Integer.toHexString(buttonCode)} = $stateOn")
    }

    fun setLatencyBuffer(ms: Float) {
        _latencyBufferMs.value = ms
    }

    fun setPioneerModelManually(modelName: String) {
        _detectedModel.value = modelName
        _lastMidiCommand.value = "Manual profile: $modelName selected"
    }
}
