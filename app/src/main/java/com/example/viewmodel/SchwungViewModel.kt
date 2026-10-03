package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SchwungAudioEngine
import com.example.data.AppDatabase
import com.example.data.Presets
import com.example.data.ProjectEntity
import com.example.midi.MidiClockSyncService
import com.example.model.*
import com.example.pioneer.PioneerDjCertifiedManager
import com.example.spotify.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

class SchwungViewModel(application: Application) : AndroidViewModel(application) {

    val audioEngine = SchwungAudioEngine()
    private val database = AppDatabase.getInstance(application)
    private val projectDao = database.projectDao()

    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    // App Navigation Mode
    private val _currentMode = MutableStateFlow(AppMode.MOVE_CONSOLE)
    val currentMode = _currentMode.asStateFlow()

    // Transport & Tempo
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _bpm = MutableStateFlow(120)
    val bpm = _bpm.asStateFlow()

    // Schwung Groove & Swing (0 to 75%)
    private val _swingPercent = MutableStateFlow(32)
    val swingPercent = _swingPercent.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep = _currentStep.asStateFlow()

    private val _metronomeEnabled = MutableStateFlow(false)
    val metronomeEnabled = _metronomeEnabled.asStateFlow()

    // PadGrid Bank & Pads
    private val _currentBank = MutableStateFlow(PadBank.DRUMS)
    val currentBank = _currentBank.asStateFlow()

    private val _drumsPads = MutableStateFlow(Presets.getDefaultDrumsPads())
    val drumsPads = _drumsPads.asStateFlow()

    private val _bassPads = MutableStateFlow(Presets.getDefaultBassPads())
    val bassPads = _bassPads.asStateFlow()

    private val _synthPads = MutableStateFlow(Presets.getDefaultSynthPads())
    val synthPads = _synthPads.asStateFlow()

    private val _splicePads = MutableStateFlow(Presets.getDefaultSpliceSlicesPads())
    val splicePads = _splicePads.asStateFlow()

    // Currently selected pad for tweaking
    private val _selectedPad = MutableStateFlow<PadData?>(null)
    val selectedPad = _selectedPad.asStateFlow()

    // Pad Effects Rack Overlay Dialog State
    private val _showPadFxOverlay = MutableStateFlow(false)
    val showPadFxOverlay = _showPadFxOverlay.asStateFlow()

    fun openPadFxOverlay(pad: PadData? = null) {
        if (pad != null) {
            _selectedPad.value = pad
        }
        _showPadFxOverlay.value = true
    }

    fun closePadFxOverlay() {
        _showPadFxOverlay.value = false
    }

    // Active pad lighting state (set of pad IDs being touched or triggered)
    private val _activePadTriggers = MutableStateFlow<Set<Int>>(emptySet())
    val activePadTriggers = _activePadTriggers.asStateFlow()

    // Pad Repeat / Roll
    private val _rollRate = MutableStateFlow(RollRate.OFF)
    val rollRate = _rollRate.asStateFlow()

    private var rollJob: Job? = null

    // Patterns & Sequencer
    private val _patterns = MutableStateFlow(Presets.getDefaultInitialPatterns())
    val patterns = _patterns.asStateFlow()

    private val _activePatternIndex = MutableStateFlow(0)
    val activePatternIndex = _activePatternIndex.asStateFlow()

    // Splice MCP Sound Cloud
    private val _splicePacks = MutableStateFlow(Presets.getSplicePacks())
    val splicePacks = _splicePacks.asStateFlow()

    private val _selectedPack = MutableStateFlow(Presets.getSplicePacks().first())
    val selectedPack = _selectedPack.asStateFlow()

    private val _selectedSample = MutableStateFlow(Presets.getSplicePacks().first().samples.first())
    val selectedSample = _selectedSample.asStateFlow()

    private val _searchFilter = MutableStateFlow("ALL")
    val searchFilter = _searchFilter.asStateFlow()

    private val _mcpPromptText = MutableStateFlow("")
    val mcpPromptText = _mcpPromptText.asStateFlow()

    private val _isMcpProcessing = MutableStateFlow(false)
    val isMcpProcessing = _isMcpProcessing.asStateFlow()

    private val _mcpNotification = MutableStateFlow<String?>(null)
    val mcpNotification = _mcpNotification.asStateFlow()

    // Slicer Studio
    private val _sliceMarkers = MutableStateFlow(
        (0..15).map { idx ->
            SliceMarker(
                sliceIndex = idx,
                startRatio = idx / 16f,
                endRatio = (idx + 1) / 16f,
                name = "Slice ${idx + 1}"
            )
        }
    )
    val sliceMarkers = _sliceMarkers.asStateFlow()

    private val _slicerPitchSemitones = MutableStateFlow(0f)
    val slicerPitchSemitones = _slicerPitchSemitones.asStateFlow()

    private val _slicerIsReverse = MutableStateFlow(false)
    val slicerIsReverse = _slicerIsReverse.asStateFlow()

    // Master FX
    private val _filterCutoff = MutableStateFlow(20000f)
    val filterCutoff = _filterCutoff.asStateFlow()

    private val _filterResonance = MutableStateFlow(1.0f)
    val filterResonance = _filterResonance.asStateFlow()

    private val _delayWet = MutableStateFlow(0.0f)
    val delayWet = _delayWet.asStateFlow()

    private val _delayFeedback = MutableStateFlow(0.35f)
    val delayFeedback = _delayFeedback.asStateFlow()

    private val _driveSaturation = MutableStateFlow(0.1f)
    val driveSaturation = _driveSaturation.asStateFlow()

    private val _isTapeStop = MutableStateFlow(false)
    val isTapeStop = _isTapeStop.asStateFlow()

    private val _isGlitchRoll = MutableStateFlow(false)
    val isGlitchRoll = _isGlitchRoll.asStateFlow()

    // Pioneer DJ Hardware & USB Manager
    val pioneerManager = PioneerDjCertifiedManager(application)

    // Spotify Repository & Dual-Deck Stream Player
    val spotifyRepository = SpotifyRepository(application)
    val spotifyPlayer = SpotifyStreamPlayer(application, pioneerManager)

    private val _spotifyTracks = MutableStateFlow(spotifyRepository.getAllTracks())
    val spotifyTracks = _spotifyTracks.asStateFlow()

    private val _spotifySearchQuery = MutableStateFlow("")
    val spotifySearchQuery = _spotifySearchQuery.asStateFlow()

    // MIDI Clock Synchronization Service (Pioneer Hardware Sync)
    val midiClockService = MidiClockSyncService(application)

    // Last activated pad for real-time waveform visualizer
    private val _lastActivatedPad = MutableStateFlow<PadData?>(null)
    val lastActivatedPad = _lastActivatedPad.asStateFlow()

    private val padWaveformCache = mutableMapOf<String, FloatArray>()

    fun getPadWaveformPeaks(pad: PadData): FloatArray {
        val cacheKey = "${pad.soundRecipe}_${pad.sliceIndex}_${pad.pitch}_${pad.decay}"
        return padWaveformCache.getOrPut(cacheKey) {
            val raw = when {
                pad.soundRecipe == "splice_slice" -> {
                    val loop = audioEngine.preloadedLoops["lofi_guitar"]
                    if (loop != null && loop.isNotEmpty()) {
                        val sliceLen = loop.size / 16
                        val start = (pad.sliceIndex * sliceLen).coerceIn(0, loop.size - 1)
                        val end = (start + sliceLen).coerceAtMost(loop.size)
                        loop.copyOfRange(start, end)
                    } else null
                }
                else -> audioEngine.synthesizeMcpSound(pad.soundRecipe)
            }

            if (raw != null && raw.isNotEmpty()) {
                val numPeaks = 32
                val chunkSize = (raw.size / numPeaks).coerceAtLeast(1)
                FloatArray(numPeaks) { i ->
                    val start = i * chunkSize
                    val end = (start + chunkSize).coerceAtMost(raw.size)
                    var peak = 0.05f
                    for (j in start until end) {
                        val absVal = kotlin.math.abs(raw[j])
                        if (absVal > peak) peak = absVal
                    }
                    peak.coerceIn(0.05f, 1.0f)
                }
            } else {
                FloatArray(32) { i ->
                    val t = i / 32f
                    (kotlin.math.sin(t * Math.PI) * kotlin.math.exp(-2.5 * t)).toFloat().coerceIn(0.05f, 1f)
                }
            }
        }
    }

    // Projects list from Room
    val savedProjects: StateFlow<List<ProjectEntity>> = projectDao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Performance Grid Snapshots (Save & Recall 4 Performance States)
    private val _snapshots = MutableStateFlow<Map<Int, GridSnapshot>>(emptyMap())
    val snapshots = _snapshots.asStateFlow()

    private val _activeSnapshotSlot = MutableStateFlow<Int?>(0)
    val activeSnapshotSlot = _activeSnapshotSlot.asStateFlow()

    private val _snapshotNotification = MutableStateFlow<String?>(null)
    val snapshotNotification = _snapshotNotification.asStateFlow()

    // Sequencer Clock Job
    private var sequencerJob: Job? = null

    init {
        audioEngine.start()
        _selectedPad.value = _drumsPads.value.firstOrNull()

        // Initialize 4 pre-configured performance snapshots
        val initialPat = _patterns.value.first()
        val defaultSnaps = mapOf(
            0 to GridSnapshot(0, "MAIN GROOVE", PadBank.DRUMS, _drumsPads.value, _bassPads.value, _synthPads.value, _splicePads.value, initialPat, 120, 32, 20000f, 1.0f, 0.2f, 0.15f),
            1 to GridSnapshot(1, "808 TRAP DROP", PadBank.BASS, _drumsPads.value, _bassPads.value, _synthPads.value, _splicePads.value, initialPat, 140, 16, 12000f, 1.8f, 0.35f, 0.25f),
            2 to GridSnapshot(2, "LO-FI CHILL", PadBank.SYNTH, _drumsPads.value, _bassPads.value, _synthPads.value, _splicePads.value, initialPat, 85, 54, 4500f, 1.2f, 0.45f, 0.35f),
            3 to GridSnapshot(3, "DUB SPACE", PadBank.SPLICE, _drumsPads.value, _bassPads.value, _synthPads.value, _splicePads.value, initialPat, 126, 28, 8000f, 2.4f, 0.65f, 0.5f)
        )
        _snapshots.value = defaultSnaps

        // Wire Pioneer MIDI Clock Sync callbacks
        midiClockService.onStepTriggeredFromMidiClock = { step ->
            _currentStep.value = step
            playStep(step)
        }
        midiClockService.onBpmCalculated = { newBpm ->
            _bpm.value = newBpm.toInt()
        }
        midiClockService.onMidiStartReceived = {
            if (!_isPlaying.value) {
                startPlayback()
            }
        }
        midiClockService.onMidiStopReceived = {
            if (_isPlaying.value) {
                stopPlayback()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        audioEngine.stop()
        spotifyPlayer.release()
        midiClockService.stopClockSender()
    }

    fun setAppMode(mode: AppMode) {
        _currentMode.value = mode
    }

    fun setBank(bank: PadBank) {
        _currentBank.value = bank
        val pads = getPadsForBank(bank)
        _selectedPad.value = pads.firstOrNull()
    }

    fun getPadsForBank(bank: PadBank): List<PadData> {
        return when (bank) {
            PadBank.DRUMS -> _drumsPads.value
            PadBank.BASS -> _bassPads.value
            PadBank.SYNTH -> _synthPads.value
            PadBank.SPLICE -> _splicePads.value
        }
    }

    fun selectPad(pad: PadData) {
        _selectedPad.value = pad
    }

    fun updatePad(updatedPad: PadData) {
        val targetFlow = when (updatedPad.bank) {
            PadBank.DRUMS -> _drumsPads
            PadBank.BASS -> _bassPads
            PadBank.SYNTH -> _synthPads
            PadBank.SPLICE -> _splicePads
        }
        val updatedPads = targetFlow.value.map { pad ->
            if (pad.id == updatedPad.id) updatedPad else pad
        }
        targetFlow.value = updatedPads
        _selectedPad.value = updatedPad
    }

    fun updatePadFx(
        padId: Int,
        bank: PadBank,
        filterCutoff: Float? = null,
        filterResonance: Float? = null,
        bitcrushBits: Int? = null,
        reverbSend: Float? = null,
        delaySend: Float? = null,
        isFxChainBypassed: Boolean? = null
    ) {
        val targetFlow = when (bank) {
            PadBank.DRUMS -> _drumsPads
            PadBank.BASS -> _bassPads
            PadBank.SYNTH -> _synthPads
            PadBank.SPLICE -> _splicePads
        }

        val updatedPads = targetFlow.value.map { pad ->
            if (pad.id == padId) {
                pad.copy(
                    filterCutoff = filterCutoff ?: pad.filterCutoff,
                    filterResonance = filterResonance ?: pad.filterResonance,
                    bitcrushBits = bitcrushBits ?: pad.bitcrushBits,
                    reverbSend = reverbSend ?: pad.reverbSend,
                    delaySend = delaySend ?: pad.delaySend,
                    isFxChainBypassed = isFxChainBypassed ?: pad.isFxChainBypassed
                )
            } else pad
        }
        targetFlow.value = updatedPads

        if (_selectedPad.value?.id == padId && _selectedPad.value?.bank == bank) {
            _selectedPad.value = updatedPads.firstOrNull { it.id == padId }
        }
    }

    fun updatePadSampleEditor(
        padId: Int,
        bank: PadBank,
        sampleStartRatio: Float? = null,
        sampleEndRatio: Float? = null,
        loopStartRatio: Float? = null,
        loopEndRatio: Float? = null,
        isLoopEnabled: Boolean? = null
    ) {
        val targetFlow = when (bank) {
            PadBank.DRUMS -> _drumsPads
            PadBank.BASS -> _bassPads
            PadBank.SYNTH -> _synthPads
            PadBank.SPLICE -> _splicePads
        }

        val updatedPads = targetFlow.value.map { pad ->
            if (pad.id == padId) {
                val newStart = sampleStartRatio?.coerceIn(0f, 0.95f) ?: pad.sampleStartRatio
                val newEnd = sampleEndRatio?.coerceIn(newStart + 0.02f, 1.0f) ?: pad.sampleEndRatio
                val newLoopStart = loopStartRatio?.coerceIn(newStart, newEnd - 0.02f) ?: pad.loopStartRatio
                val newLoopEnd = loopEndRatio?.coerceIn(newLoopStart + 0.02f, newEnd) ?: pad.loopEndRatio
                pad.copy(
                    sampleStartRatio = newStart,
                    sampleEndRatio = newEnd,
                    loopStartRatio = newLoopStart,
                    loopEndRatio = newLoopEnd,
                    isLoopEnabled = isLoopEnabled ?: pad.isLoopEnabled
                )
            } else pad
        }
        targetFlow.value = updatedPads

        if (_selectedPad.value?.id == padId && _selectedPad.value?.bank == bank) {
            _selectedPad.value = updatedPads.firstOrNull { it.id == padId }
        }
    }

    // Pad Trigger with Expressive Velocity
    fun onPadDown(pad: PadData, velocity: Float) {
        triggerHaptic()
        _lastActivatedPad.value = pad
        val currentPads = _activePadTriggers.value.toMutableSet()
        currentPads.add(pad.id)
        _activePadTriggers.value = currentPads

        firePadSound(pad, velocity)

        // If recording is active, capture into current pattern step!
        if (_isRecording.value && _isPlaying.value) {
            recordPadHitToPattern(pad, velocity)
        }

        // Check if Roll/Repeat is active
        if (_rollRate.value != RollRate.OFF) {
            startPadRoll(pad, velocity)
        }
    }

    fun onPadUp(pad: PadData) {
        val currentPads = _activePadTriggers.value.toMutableSet()
        currentPads.remove(pad.id)
        _activePadTriggers.value = currentPads

        rollJob?.cancel()
        rollJob = null
    }

    private fun firePadSound(pad: PadData, velocity: Float) {
        val loopKey = when (pad.bank) {
            PadBank.SPLICE -> _selectedSample.value.soundRecipe
            else -> "lofi_guitar"
        }

        audioEngine.triggerSound(
            recipe = pad.soundRecipe,
            velocity = velocity * pad.volume,
            pitchSemitones = pad.pitch,
            decayMultiplier = pad.decay,
            cutoff = if (pad.isFxChainBypassed) 20000f else pad.filterCutoff,
            chokeGroup = pad.chokeGroup,
            sliceIndex = pad.sliceIndex,
            loopKey = loopKey
        )
    }

    private fun startPadRoll(pad: PadData, velocity: Float) {
        rollJob?.cancel()
        rollJob = viewModelScope.launch {
            val stepDivision = _rollRate.value.division
            val baseSixteenthMs = (60_000.0 / _bpm.value) / 4.0
            val intervalMs = (baseSixteenthMs * (stepDivision * 4.0)).toLong().coerceAtLeast(20)

            while (isActive) {
                delay(intervalMs)
                firePadSound(pad, velocity)
            }
        }
    }

    fun setRollRate(rate: RollRate) {
        _rollRate.value = rate
    }

    // Transport Controls
    fun togglePlayPause() {
        if (_isPlaying.value) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        _isPlaying.value = true
        _currentStep.value = 0

        sequencerJob?.cancel()
        sequencerJob = viewModelScope.launch(Dispatchers.Default) {
            val totalSteps = 16
            while (isActive && _isPlaying.value) {
                val step = _currentStep.value
                val bpmVal = _bpm.value
                val swingVal = _swingPercent.value

                // 1. Trigger sounds for this step
                playStep(step)

                // 2. Metronome click on beat 0, 4, 8, 12
                if (_metronomeEnabled.value && step % 4 == 0) {
                    audioEngine.triggerSound(
                        recipe = "perc_click",
                        velocity = if (step == 0) 0.9f else 0.5f,
                        pitchSemitones = if (step == 0) 12f else 7f,
                        decayMultiplier = 0.1f
                    )
                }

                // 3. Schwung Groove Microtiming Delay
                // In 16th notes: even steps are straight, odd steps get delayed by swing factor!
                val baseStepDurationMs = (60_000.0 / bpmVal) / 4.0
                val isSwungStep = step % 2 == 1
                val swingFactor = (swingVal / 100.0) * 0.5 // up to ~35% offset
                val stepDelayMs = if (isSwungStep) {
                    baseStepDurationMs * (1.0 + swingFactor)
                } else {
                    baseStepDurationMs * (1.0 - swingFactor)
                }.toLong().coerceAtLeast(15)

                delay(stepDelayMs)

                // Advance step
                _currentStep.value = (step + 1) % totalSteps
            }
        }
    }

    private fun stopPlayback() {
        _isPlaying.value = false
        sequencerJob?.cancel()
        sequencerJob = null
        _currentStep.value = 0
    }

    fun toggleRecording() {
        _isRecording.value = !_isRecording.value
        if (_isRecording.value && !_isPlaying.value) {
            startPlayback()
        }
    }

    fun setBpm(newBpm: Int) {
        _bpm.value = newBpm.coerceIn(40, 240)
    }

    fun setSwingPercent(swing: Int) {
        _swingPercent.value = swing.coerceIn(0, 75)
    }

    fun toggleMetronome() {
        _metronomeEnabled.value = !_metronomeEnabled.value
    }

    // Step Sequencer Logic
    private fun playStep(stepIndex: Int) {
        val currentPattern = _patterns.value.getOrNull(_activePatternIndex.value) ?: return

        for (track in currentPattern.tracks) {
            if (track.isMuted) continue
            val step = track.steps.getOrNull(stepIndex) ?: continue

            if (step.active) {
                val vel = (step.velocity * (if (step.isAccent) 1.25f else 1.0f) * track.volume).coerceIn(0.1f, 1.2f)
                val pitch = step.pitchOffset.toFloat()

                audioEngine.triggerSound(
                    recipe = track.soundRecipe,
                    velocity = vel,
                    pitchSemitones = pitch,
                    decayMultiplier = 1.0f,
                    cutoff = 20000f,
                    chokeGroup = if (track.trackId == 1) 1 else 0,
                    sliceIndex = if (track.soundRecipe == "splice_slice") stepIndex else -1,
                    loopKey = _selectedSample.value.soundRecipe
                )
            }
        }
    }

    fun toggleStep(trackId: Int, stepIndex: Int) {
        val currentPatIdx = _activePatternIndex.value
        val pats = _patterns.value.toMutableList()
        val currentPat = pats[currentPatIdx]

        val updatedTracks = currentPat.tracks.map { track ->
            if (track.trackId == trackId) {
                val updatedSteps = track.steps.map { s ->
                    if (s.stepIndex == stepIndex) {
                        s.copy(active = !s.active)
                    } else s
                }
                track.copy(steps = updatedSteps)
            } else track
        }

        pats[currentPatIdx] = currentPat.copy(tracks = updatedTracks)
        _patterns.value = pats
    }

    fun setStepVelocity(trackId: Int, stepIndex: Int, velocity: Float) {
        val currentPatIdx = _activePatternIndex.value
        val pats = _patterns.value.toMutableList()
        val currentPat = pats[currentPatIdx]

        val updatedTracks = currentPat.tracks.map { track ->
            if (track.trackId == trackId) {
                val updatedSteps = track.steps.map { s ->
                    if (s.stepIndex == stepIndex) {
                        s.copy(velocity = velocity.coerceIn(0.1f, 1.0f))
                    } else s
                }
                track.copy(steps = updatedSteps)
            } else track
        }

        pats[currentPatIdx] = currentPat.copy(tracks = updatedTracks)
        _patterns.value = pats
    }

    fun toggleTrackMute(trackId: Int) {
        val currentPatIdx = _activePatternIndex.value
        val pats = _patterns.value.toMutableList()
        val currentPat = pats[currentPatIdx]

        val updatedTracks = currentPat.tracks.map { track ->
            if (track.trackId == trackId) {
                track.copy(isMuted = !track.isMuted)
            } else track
        }

        pats[currentPatIdx] = currentPat.copy(tracks = updatedTracks)
        _patterns.value = pats
    }

    fun setActivePattern(index: Int) {
        if (index in 0 until _patterns.value.size) {
            _activePatternIndex.value = index
        }
    }

    fun clearActivePattern() {
        val currentPatIdx = _activePatternIndex.value
        val pats = _patterns.value.toMutableList()
        val currentPat = pats[currentPatIdx]

        val clearedTracks = currentPat.tracks.map { track ->
            track.copy(steps = (0..15).map { StepData(stepIndex = it, active = false) })
        }
        pats[currentPatIdx] = currentPat.copy(tracks = clearedTracks)
        _patterns.value = pats
    }

    // Grid Snapshot Save & Recall
    fun saveSnapshot(slotIndex: Int, customName: String? = null) {
        val slotName = customName ?: when (slotIndex) {
            0 -> "SET A (MAIN)"
            1 -> "SET B (DROP)"
            2 -> "SET C (BRIDGE)"
            else -> "SET D (OUTRO)"
        }
        val currentPat = _patterns.value.getOrNull(_activePatternIndex.value) ?: Presets.getDefaultInitialPatterns().first()
        val snapshot = GridSnapshot(
            slotIndex = slotIndex,
            name = slotName,
            activeBank = _currentBank.value,
            drumsPads = _drumsPads.value,
            bassPads = _bassPads.value,
            synthPads = _synthPads.value,
            splicePads = _splicePads.value,
            pattern = currentPat,
            bpm = _bpm.value,
            swingPercent = _swingPercent.value,
            filterCutoff = _filterCutoff.value,
            filterResonance = _filterResonance.value,
            delayWet = _delayWet.value,
            reverbWet = _masterReverbWet.value
        )
        val currentMap = _snapshots.value.toMutableMap()
        currentMap[slotIndex] = snapshot
        _snapshots.value = currentMap
        _activeSnapshotSlot.value = slotIndex
        _snapshotNotification.value = "Saved Snapshot into Slot ${slotIndex + 1}: $slotName"
        triggerHaptic()
    }

    fun recallSnapshot(slotIndex: Int) {
        val snapshot = _snapshots.value[slotIndex] ?: return
        _currentBank.value = snapshot.activeBank
        _drumsPads.value = snapshot.drumsPads
        _bassPads.value = snapshot.bassPads
        _synthPads.value = snapshot.synthPads
        _splicePads.value = snapshot.splicePads
        _bpm.value = snapshot.bpm
        _swingPercent.value = snapshot.swingPercent
        setMasterFilterCutoff(snapshot.filterCutoff)
        setMasterFilterResonance(snapshot.filterResonance)
        setDelayWet(snapshot.delayWet)
        setMasterReverbWet(snapshot.reverbWet)

        val patList = _patterns.value.toMutableList()
        val patIdx = _activePatternIndex.value
        if (patIdx in patList.indices) {
            patList[patIdx] = snapshot.pattern
            _patterns.value = patList
        }

        _activeSnapshotSlot.value = slotIndex
        _snapshotNotification.value = "Recalled Snapshot Slot ${slotIndex + 1}: ${snapshot.name}"
        triggerHaptic()
    }

    fun clearSnapshotNotification() {
        _snapshotNotification.value = null
    }

    private fun recordPadHitToPattern(pad: PadData, velocity: Float) {
        val currentStepIdx = _currentStep.value
        val trackId = when (pad.bank) {
            PadBank.DRUMS -> if (pad.id in listOf(6, 7, 10)) 1 else 0
            PadBank.BASS -> 2
            PadBank.SYNTH -> 3
            PadBank.SPLICE -> 3
        }

        val pats = _patterns.value.toMutableList()
        val currentPat = pats[_activePatternIndex.value]

        val updatedTracks = currentPat.tracks.map { track ->
            if (track.trackId == trackId) {
                val updatedSteps = track.steps.map { s ->
                    if (s.stepIndex == currentStepIdx) {
                        s.copy(active = true, velocity = velocity, pitchOffset = pad.pitch.toInt())
                    } else s
                }
                track.copy(steps = updatedSteps)
            } else track
        }

        pats[_activePatternIndex.value] = currentPat.copy(tracks = updatedTracks)
        _patterns.value = pats
    }

    // Splice MCP Sound Cloud Methods
    fun selectSplicePack(pack: SplicePack) {
        _selectedPack.value = pack
        _selectedSample.value = pack.samples.firstOrNull() ?: _selectedSample.value
    }

    fun selectSpliceSample(sample: SpliceSample) {
        _selectedSample.value = sample
    }

    fun setSearchFilter(category: String) {
        _searchFilter.value = category
    }

    fun setMcpPrompt(prompt: String) {
        _mcpPromptText.value = prompt
    }

    fun sliceSampleToGrid(sample: SpliceSample) {
        _selectedSample.value = sample

        // Generate 16 chops on Splice Bank
        val slicedPads = (0..15).map { idx ->
            PadData(
                id = idx,
                name = "${sample.title.take(8)} #$idx",
                bank = PadBank.SPLICE,
                colorHex = 0xFFFFB300,
                soundRecipe = "splice_slice",
                sliceIndex = idx,
                chokeGroup = 2
            )
        }
        _splicePads.value = slicedPads
        _currentBank.value = PadBank.SPLICE
        _mcpNotification.value = "Chapped 16 slices from '${sample.title}' into PadGrid Bank D!"
    }

    fun loadSampleToSelectedPad(sample: SpliceSample) {
        val targetPad = _selectedPad.value ?: getPadsForBank(_currentBank.value).firstOrNull() ?: return
        val bank = targetPad.bank
        val targetFlow = when (bank) {
            PadBank.DRUMS -> _drumsPads
            PadBank.BASS -> _bassPads
            PadBank.SYNTH -> _synthPads
            PadBank.SPLICE -> _splicePads
        }

        val updated = targetFlow.value.map { pad ->
            if (pad.id == targetPad.id) {
                pad.copy(
                    name = sample.title.take(12),
                    soundRecipe = sample.soundRecipe
                )
            } else pad
        }
        targetFlow.value = updated
        _selectedPad.value = updated.firstOrNull { it.id == targetPad.id }
        _mcpNotification.value = "Loaded '${sample.title}' onto ${bank.label} Pad ${targetPad.id + 1}!"
        triggerHaptic()
    }

    fun generateWithSpliceMcp(prompt: String) {
        if (prompt.isBlank()) return
        _isMcpProcessing.value = true
        _mcpNotification.value = null

        viewModelScope.launch {
            delay(1200) // Realistic MCP processing time
            val generatedWave = audioEngine.synthesizeMcpSound(prompt)
            val newId = "mcp_gen_${System.currentTimeMillis()}"
            val newSample = SpliceSample(
                id = newId,
                title = prompt.take(24).trim().replaceFirstChar { it.uppercase() },
                packName = "Splice MCP Vault",
                category = when {
                    prompt.contains("kick", true) || prompt.contains("snare", true) -> "DRUM LOOP"
                    prompt.contains("808", true) || prompt.contains("bass", true) -> "BASS 808"
                    prompt.contains("vocal", true) -> "VOCAL CHOP"
                    else -> "SYNTH LOOP"
                },
                bpm = _bpm.value,
                key = "C Min",
                durationSec = 1.5f,
                wavePeaks = (0..15).map { (Math.random().toFloat() * 0.7f + 0.3f) },
                soundRecipe = when {
                    prompt.contains("kick", true) -> "kick_sub"
                    prompt.contains("snare", true) -> "snare_acoustic"
                    prompt.contains("808", true) -> "808_bass"
                    prompt.contains("vocal", true) -> "vocal_chop"
                    else -> "synth_lead"
                }
            )

            // Register into audio engine preloaded loops
            audioEngine.preloadedLoops[newId] = generatedWave

            // Update sample list
            val updatedPack = _selectedPack.value.copy(
                samples = listOf(newSample) + _selectedPack.value.samples
            )
            _selectedPack.value = updatedPack
            _selectedSample.value = newSample

            _isMcpProcessing.value = false
            _mcpNotification.value = "Splice MCP generated: '${newSample.title}' ready on pads!"
        }
    }

    fun clearMcpNotification() {
        _mcpNotification.value = null
    }

    // Master FX Parameters
    fun setMasterFilterCutoff(cutoff: Float) {
        val c = cutoff.coerceIn(150f, 20000f)
        _filterCutoff.value = c
        audioEngine.filterCutoffHz = c
    }

    fun setMasterFilterResonance(q: Float) {
        val res = q.coerceIn(0.5f, 4.5f)
        _filterResonance.value = res
        audioEngine.filterResonance = res
    }

    fun setDelayWet(wet: Float) {
        val w = wet.coerceIn(0f, 1f)
        _delayWet.value = w
        audioEngine.delayWet = w
    }

    fun setDelayFeedback(fb: Float) {
        val f = fb.coerceIn(0f, 0.85f)
        _delayFeedback.value = f
        audioEngine.delayFeedback = f
    }

    // Master FX Chain: Reverb, Delay, Bitcrusher
    private val _masterReverbWet = MutableStateFlow(0.15f)
    val masterReverbWet = _masterReverbWet.asStateFlow()

    private val _masterReverbRoom = MutableStateFlow(0.7f)
    val masterReverbRoom = _masterReverbRoom.asStateFlow()

    private val _masterBitcrusherWet = MutableStateFlow(0.0f)
    val masterBitcrusherWet = _masterBitcrusherWet.asStateFlow()

    private val _masterBitcrusherBits = MutableStateFlow(8)
    val masterBitcrusherBits = _masterBitcrusherBits.asStateFlow()

    private val _masterBitcrusherDownsample = MutableStateFlow(1)
    val masterBitcrusherDownsample = _masterBitcrusherDownsample.asStateFlow()

    fun setMasterReverbWet(wet: Float) {
        val w = wet.coerceIn(0f, 1f)
        _masterReverbWet.value = w
        audioEngine.setMasterReverb(w, _masterReverbRoom.value)
    }

    fun setMasterReverbRoom(room: Float) {
        val r = room.coerceIn(0.1f, 0.95f)
        _masterReverbRoom.value = r
        audioEngine.setMasterReverb(_masterReverbWet.value, r)
    }

    fun setMasterBitcrusherWet(wet: Float) {
        val w = wet.coerceIn(0f, 1f)
        _masterBitcrusherWet.value = w
        audioEngine.setMasterBitcrusher(w, _masterBitcrusherBits.value, _masterBitcrusherDownsample.value)
    }

    fun setMasterBitcrusherBits(bits: Int) {
        val b = bits.coerceIn(2, 16)
        _masterBitcrusherBits.value = b
        audioEngine.setMasterBitcrusher(_masterBitcrusherWet.value, b, _masterBitcrusherDownsample.value)
    }

    fun setMasterBitcrusherDownsample(downsample: Int) {
        val d = downsample.coerceIn(1, 32)
        _masterBitcrusherDownsample.value = d
        audioEngine.setMasterBitcrusher(_masterBitcrusherWet.value, _masterBitcrusherBits.value, d)
    }

    fun setDriveSaturation(drive: Float) {
        val d = drive.coerceIn(0f, 1f)
        _driveSaturation.value = d
        audioEngine.driveSaturation = d
    }

    fun setTapeStop(active: Boolean) {
        _isTapeStop.value = active
        audioEngine.isTapeStopActive = active
    }

    fun setGlitchRoll(active: Boolean, division: Float = 0.125f) {
        _isGlitchRoll.value = active
        audioEngine.isRollActive = active
        audioEngine.rollDivisionSamples = (SchwungAudioEngine.SAMPLE_RATE * division).toInt()
    }

    // Tweak Selected Pad
    fun updateSelectedPadPitch(pitch: Float) {
        val pad = _selectedPad.value ?: return
        val updated = pad.copy(pitch = pitch)
        _selectedPad.value = updated
        updatePadInBank(updated)
    }

    fun updateSelectedPadDecay(decay: Float) {
        val pad = _selectedPad.value ?: return
        val updated = pad.copy(decay = decay)
        _selectedPad.value = updated
        updatePadInBank(updated)
    }

    fun updateSelectedPadVolume(volume: Float) {
        val pad = _selectedPad.value ?: return
        val updated = pad.copy(volume = volume)
        _selectedPad.value = updated
        updatePadInBank(updated)
    }

    private fun updatePadInBank(pad: PadData) {
        when (pad.bank) {
            PadBank.DRUMS -> _drumsPads.value = _drumsPads.value.map { if (it.id == pad.id) pad else it }
            PadBank.BASS -> _bassPads.value = _bassPads.value.map { if (it.id == pad.id) pad else it }
            PadBank.SYNTH -> _synthPads.value = _synthPads.value.map { if (it.id == pad.id) pad else it }
            PadBank.SPLICE -> _splicePads.value = _splicePads.value.map { if (it.id == pad.id) pad else it }
        }
    }

    // Project Save & Load
    fun saveProject(title: String) {
        viewModelScope.launch {
            val project = ProjectEntity(
                id = "proj_${System.currentTimeMillis()}",
                title = title.ifBlank { "Schwung Beat ${System.currentTimeMillis() % 1000}" },
                bpm = _bpm.value,
                swingPercent = _swingPercent.value,
                projectJson = "{}",
                lastModified = System.currentTimeMillis()
            )
            projectDao.saveProject(project)
            _mcpNotification.value = "Project saved: '${project.title}'"
        }
    }

    fun loadDemoProject(demo: BeatProject) {
        _bpm.value = demo.bpm
        _swingPercent.value = demo.swingPercent
        _patterns.value = demo.patterns
        _activePatternIndex.value = demo.activePatternIndex
        _mcpNotification.value = "Loaded demo: '${demo.title}'"
    }

    // Audio Recording Export
    fun startExportRecording() {
        audioEngine.startRecording()
        _mcpNotification.value = "Master WAV recording started..."
    }

    fun stopExportRecording(context: Context, onSaved: (File?) -> Unit) {
        val exportDir = File(context.cacheDir, "exports")
        exportDir.mkdirs()
        val wavFile = File(exportDir, "SchwungLive_${System.currentTimeMillis()}.wav")

        val success = audioEngine.stopRecordingAndSave(wavFile)
        if (success) {
            _mcpNotification.value = "Exported WAV to ${wavFile.name} (${wavFile.length() / 1024} KB)"
            onSaved(wavFile)
        } else {
            _mcpNotification.value = "Export failed or no audio recorded."
            onSaved(null)
        }
    }

    fun searchSpotifyTracks(query: String) {
        _spotifySearchQuery.value = query
        viewModelScope.launch {
            _spotifyTracks.value = spotifyRepository.searchTracks(query)
        }
    }

    fun loadSpotifyTrackToDeck(deckId: DjDeckId, track: SpotifyTrack) {
        spotifyPlayer.loadTrack(deckId, track)
        _mcpNotification.value = "Loaded '${track.name}' into Pioneer $deckId"
    }

    fun sliceSpotifyTrackToPads(track: SpotifyTrack) {
        val slicedPads = (0..15).map { idx ->
            PadData(
                id = idx,
                name = "${track.name.take(7)} #$idx",
                bank = PadBank.SPLICE,
                colorHex = 0xFF1DB954, // Spotify Green
                soundRecipe = "splice_slice",
                sliceIndex = idx,
                chokeGroup = 2
            )
        }
        _splicePads.value = slicedPads
        _currentBank.value = PadBank.SPLICE
        _mcpNotification.value = "Auto-chopped '${track.name}' into 16 slices on PadGrid Bank D!"
    }

    private fun triggerHaptic() {
        try {
            vibrator?.let { v ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(18)
                }
            }
        } catch (_: Exception) {}
    }
}
