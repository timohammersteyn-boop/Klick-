package com.example.spotify

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import com.example.pioneer.PioneerDjCertifiedManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

class SpotifyStreamPlayer(
    private val context: Context,
    private val pioneerManager: PioneerDjCertifiedManager
) {
    companion object {
        private const val TAG = "SpotifyStreamPlayer"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Dual Decks
    private val _deckAState = MutableStateFlow(DjDeckState(deckId = DjDeckId.DECK_A))
    val deckAState = _deckAState.asStateFlow()

    private val _deckBState = MutableStateFlow(DjDeckState(deckId = DjDeckId.DECK_B))
    val deckBState = _deckBState.asStateFlow()

    private val _crossfader = MutableStateFlow(0.5f) // 0.0 (Deck A) to 1.0 (Deck B)
    val crossfader = _crossfader.asStateFlow()

    private var mediaPlayerA: MediaPlayer? = null
    private var mediaPlayerB: MediaPlayer? = null

    private var updateJob: Job? = null

    init {
        // Wire Pioneer hardware controller callbacks
        pioneerManager.onPlayPauseReceived = { deckIdx ->
            if (deckIdx == 0) togglePlayPause(DjDeckId.DECK_A) else togglePlayPause(DjDeckId.DECK_B)
        }

        pioneerManager.onCueReceived = { deckIdx ->
            if (deckIdx == 0) triggerCue(DjDeckId.DECK_A) else triggerCue(DjDeckId.DECK_B)
        }

        pioneerManager.onJogWheelScratched = { deckIdx, delta ->
            jogWheelScratch(if (deckIdx == 0) DjDeckId.DECK_A else DjDeckId.DECK_B, delta)
        }

        pioneerManager.onPitchFaderChanged = { deckIdx, pitchPct ->
            setPitchPercent(if (deckIdx == 0) DjDeckId.DECK_A else DjDeckId.DECK_B, pitchPct)
        }

        pioneerManager.onHotCuePressed = { deckIdx, hotCueIdx ->
            triggerHotCue(if (deckIdx == 0) DjDeckId.DECK_A else DjDeckId.DECK_B, hotCueIdx)
        }

        pioneerManager.onCrossfaderMoved = { xfader ->
            setCrossfader(xfader)
        }

        startPositionUpdater()
    }

    fun loadTrack(deckId: DjDeckId, track: SpotifyTrack) {
        val targetState = if (deckId == DjDeckId.DECK_A) _deckAState else _deckBState
        targetState.value = targetState.value.copy(
            loadedTrack = track,
            isPlaying = false,
            currentPositionMs = 0L,
            durationMs = track.durationMs,
            effectiveBpm = track.bpm,
            cuePositionMs = 0L,
            hotCues = listOf(0L, (track.durationMs * 0.15f).toLong(), (track.durationMs * 0.35f).toLong(), (track.durationMs * 0.5f).toLong(), null, null, null, null)
        )

        // Initialize / re-prepare MediaPlayer
        setupMediaPlayer(deckId, track)
    }

    private fun setupMediaPlayer(deckId: DjDeckId, track: SpotifyTrack) {
        try {
            val player = if (deckId == DjDeckId.DECK_A) {
                mediaPlayerA?.release()
                MediaPlayer().also { mediaPlayerA = it }
            } else {
                mediaPlayerB?.release()
                MediaPlayer().also { mediaPlayerB = it }
            }

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            // If preview URL is available, set it
            val url = track.previewUrl
            if (!url.isNullOrBlank()) {
                player.setDataSource(url)
                player.prepareAsync()
                player.setOnPreparedListener {
                    Log.i(TAG, "Spotify preview prepared for $deckId: ${track.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio player setup note: ${e.message}")
        }
    }

    fun togglePlayPause(deckId: DjDeckId) {
        val targetState = if (deckId == DjDeckId.DECK_A) _deckAState else _deckBState
        val current = targetState.value
        val newPlayState = !current.isPlaying

        targetState.value = current.copy(isPlaying = newPlayState)

        val player = if (deckId == DjDeckId.DECK_A) mediaPlayerA else mediaPlayerB
        try {
            if (newPlayState) {
                player?.start()
                applyPitchParams(deckId)
            } else {
                player?.pause()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Play/pause error: ${e.message}")
        }

        // Send feedback to Pioneer hardware (Play button light = 0x0B)
        pioneerManager.sendPioneerLedFeedback(
            deckIndex = if (deckId == DjDeckId.DECK_A) 0 else 1,
            buttonCode = 0x0B,
            stateOn = newPlayState
        )
    }

    fun triggerCue(deckId: DjDeckId) {
        val targetState = if (deckId == DjDeckId.DECK_A) _deckAState else _deckBState
        val current = targetState.value

        if (current.isPlaying) {
            // Stop and jump back to cue point
            targetState.value = current.copy(
                isPlaying = false,
                currentPositionMs = current.cuePositionMs
            )
            val player = if (deckId == DjDeckId.DECK_A) mediaPlayerA else mediaPlayerB
            player?.pause()
            player?.seekTo(current.cuePositionMs.toInt())
        } else {
            // Set new cue point at current position
            targetState.value = current.copy(cuePositionMs = current.currentPositionMs)
        }

        // Send feedback to Pioneer hardware (Cue button light = 0x0C)
        pioneerManager.sendPioneerLedFeedback(
            deckIndex = if (deckId == DjDeckId.DECK_A) 0 else 1,
            buttonCode = 0x0C,
            stateOn = true
        )
    }

    fun triggerHotCue(deckId: DjDeckId, hotCueIndex: Int) {
        val targetState = if (deckId == DjDeckId.DECK_A) _deckAState else _deckBState
        val current = targetState.value
        val hotCues = current.hotCues.toMutableList()

        val cuePos = hotCues.getOrNull(hotCueIndex)
        if (cuePos != null) {
            // Jump to cue position
            seekTo(deckId, cuePos)
        } else {
            // Set current position as hot cue
            hotCues[hotCueIndex] = current.currentPositionMs
            targetState.value = current.copy(hotCues = hotCues)
        }
    }

    fun setPitchPercent(deckId: DjDeckId, pitchPercent: Float) {
        val targetState = if (deckId == DjDeckId.DECK_A) _deckAState else _deckBState
        val current = targetState.value
        val clampedPitch = pitchPercent.coerceIn(-16.0f, 16.0f)
        val baseBpm = current.loadedTrack?.bpm ?: 124.0f
        val effectiveBpm = baseBpm * (1.0f + (clampedPitch / 100.0f))

        targetState.value = current.copy(
            pitchPercent = clampedPitch,
            effectiveBpm = effectiveBpm
        )

        applyPitchParams(deckId)
    }

    private fun applyPitchParams(deckId: DjDeckId) {
        val targetState = if (deckId == DjDeckId.DECK_A) _deckAState else _deckBState
        val pitchFactor = 1.0f + (targetState.value.pitchPercent / 100.0f)
        val player = if (deckId == DjDeckId.DECK_A) mediaPlayerA else mediaPlayerB

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && player != null) {
            try {
                if (player.isPlaying) {
                    val params = PlaybackParams()
                    params.speed = pitchFactor.coerceIn(0.5f, 2.0f)
                    params.pitch = pitchFactor.coerceIn(0.5f, 2.0f)
                    player.playbackParams = params
                }
            } catch (e: Exception) {
                Log.w(TAG, "PlaybackParams error: ${e.message}")
            }
        }
    }

    fun jogWheelScratch(deckId: DjDeckId, deltaMs: Float) {
        val targetState = if (deckId == DjDeckId.DECK_A) _deckAState else _deckBState
        val current = targetState.value
        val newPos = (current.currentPositionMs + deltaMs.toLong()).coerceIn(0L, current.durationMs)

        targetState.value = current.copy(
            currentPositionMs = newPos,
            jogTouchActive = true
        )

        val player = if (deckId == DjDeckId.DECK_A) mediaPlayerA else mediaPlayerB
        try {
            player?.seekTo(newPos.toInt())
        } catch (_: Exception) {}
    }

    fun toggleAutoLoop(deckId: DjDeckId, beats: Float = 4.0f) {
        val targetState = if (deckId == DjDeckId.DECK_A) _deckAState else _deckBState
        val current = targetState.value
        val isNewLoop = !current.isLoopActive

        targetState.value = current.copy(
            isLoopActive = isNewLoop,
            loopBeats = beats,
            loopStartPositionMs = if (isNewLoop) current.currentPositionMs else 0L
        )
    }

    fun setEqHigh(deckId: DjDeckId, value: Float) {
        val targetState = if (deckId == DjDeckId.DECK_A) _deckAState else _deckBState
        targetState.value = targetState.value.copy(eqHigh = value.coerceIn(0f, 2f))
    }

    fun setEqMid(deckId: DjDeckId, value: Float) {
        val targetState = if (deckId == DjDeckId.DECK_A) _deckAState else _deckBState
        targetState.value = targetState.value.copy(eqMid = value.coerceIn(0f, 2f))
    }

    fun setEqLow(deckId: DjDeckId, value: Float) {
        val targetState = if (deckId == DjDeckId.DECK_A) _deckAState else _deckBState
        targetState.value = targetState.value.copy(eqLow = value.coerceIn(0f, 2f))
    }

    fun setFilterKnob(deckId: DjDeckId, value: Float) {
        val targetState = if (deckId == DjDeckId.DECK_A) _deckAState else _deckBState
        targetState.value = targetState.value.copy(filterKnob = value.coerceIn(0f, 1f))
    }

    fun setCrossfader(xfader: Float) {
        _crossfader.value = xfader.coerceIn(0f, 1f)
    }

    fun seekTo(deckId: DjDeckId, positionMs: Long) {
        val targetState = if (deckId == DjDeckId.DECK_A) _deckAState else _deckBState
        val clamped = positionMs.coerceIn(0L, targetState.value.durationMs)
        targetState.value = targetState.value.copy(currentPositionMs = clamped)

        val player = if (deckId == DjDeckId.DECK_A) mediaPlayerA else mediaPlayerB
        try {
            player?.seekTo(clamped.toInt())
        } catch (_: Exception) {}
    }

    private fun startPositionUpdater() {
        updateJob?.cancel()
        updateJob = coroutineScope.launch {
            while (isActive) {
                delay(33) // ~30 fps update rate for smooth Pioneer jog & waveform

                // Update Deck A
                val stA = _deckAState.value
                if (stA.isPlaying) {
                    val bpm = stA.effectiveBpm
                    val beatDurationMs = (60000.0f / bpm)
                    val advance = 33L

                    var newPos = stA.currentPositionMs + advance
                    // Handle loop
                    if (stA.isLoopActive) {
                        val loopLengthMs = (beatDurationMs * stA.loopBeats).toLong()
                        if (newPos >= stA.loopStartPositionMs + loopLengthMs) {
                            newPos = stA.loopStartPositionMs
                            mediaPlayerA?.seekTo(newPos.toInt())
                        }
                    } else if (newPos >= stA.durationMs) {
                        newPos = 0L
                    }

                    _deckAState.value = stA.copy(currentPositionMs = newPos)
                }

                // Update Deck B
                val stB = _deckBState.value
                if (stB.isPlaying) {
                    val bpm = stB.effectiveBpm
                    val beatDurationMs = (60000.0f / bpm)
                    val advance = 33L

                    var newPos = stB.currentPositionMs + advance
                    // Handle loop
                    if (stB.isLoopActive) {
                        val loopLengthMs = (beatDurationMs * stB.loopBeats).toLong()
                        if (newPos >= stB.loopStartPositionMs + loopLengthMs) {
                            newPos = stB.loopStartPositionMs
                            mediaPlayerB?.seekTo(newPos.toInt())
                        }
                    } else if (newPos >= stB.durationMs) {
                        newPos = 0L
                    }

                    _deckBState.value = stB.copy(currentPositionMs = newPos)
                }
            }
        }
    }

    fun release() {
        updateJob?.cancel()
        mediaPlayerA?.release()
        mediaPlayerB?.release()
        mediaPlayerA = null
        mediaPlayerB = null
    }
}
