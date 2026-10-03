package com.example.spotify

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val albumArtUrl: String = "",
    val durationMs: Long = 180000L,
    val bpm: Float = 124.0f,
    val musicalKey: String = "8A (A min)",
    val energy: Float = 0.82f,
    val danceability: Float = 0.78f,
    val previewUrl: String? = null,
    val uri: String = "spotify:track:$id",
    val waveformData: List<Float> = (0..31).map { (Math.random().toFloat() * 0.7f + 0.3f) }
)

data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val description: String,
    val coverUrl: String = "",
    val tracks: List<SpotifyTrack>
)

enum class DjDeckId {
    DECK_A, DECK_B
}

data class DjDeckState(
    val deckId: DjDeckId,
    val loadedTrack: SpotifyTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 180000L,
    val pitchPercent: Float = 0.0f, // -16% to +16%
    val effectiveBpm: Float = 124.0f,
    val isLoopActive: Boolean = false,
    val loopBeats: Float = 4.0f,
    val loopStartPositionMs: Long = 0L,
    val cuePositionMs: Long = 0L,
    val hotCues: List<Long?> = listOf(null, null, null, null, null, null, null, null),
    val eqHigh: Float = 1.0f, // 0.0 to 2.0
    val eqMid: Float = 1.0f,
    val eqLow: Float = 1.0f,
    val filterKnob: Float = 0.5f, // 0.0 (LPF) to 0.5 (Neutral) to 1.0 (HPF)
    val volume: Float = 0.85f,
    val isCueMonitored: Boolean = false,
    val isSyncEnabled: Boolean = false,
    val jogTouchActive: Boolean = false
)

data class SpotifyAuthState(
    val isConnected: Boolean = false,
    val clientKey: String = "",
    val userDisplayName: String = "Pioneer DJ Pro",
    val userTier: String = "Premium (Streaming Ready)"
)
