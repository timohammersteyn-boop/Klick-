package com.example.model

enum class PadBank(val label: String, val colorHex: Long) {
    DRUMS("DRUMS", 0xFFFF5252),
    BASS("BASS 808", 0xFFA855F7),
    SYNTH("KEYS / SYNTH", 0xFF00E5FF),
    SPLICE("SPLICE SLICES", 0xFFFFB300)
}

enum class AppMode(val label: String) {
    MOVE_CONSOLE("ABLETON MOVE"),
    PAD_GRID("PAD GRID"),
    PIONEER_SPOTIFY_DJ("PIONEER DJ"),
    STEP_SEQUENCER("STEP SEQ"),
    SPLICE_MCP("SPLICE MCP"),
    SLICER_STUDIO("SLICER"),
    FX_RACK("FX RACK"),
    PROJECTS("PROJECTS")
}

enum class RollRate(val label: String, val division: Float) {
    OFF("OFF", 0f),
    EIGHTH("1/8", 0.5f),
    SIXTEENTH("1/16", 0.25f),
    THIRTY_SECOND("1/32", 0.125f),
    TRIPLET("1/16T", 0.1666f)
}

data class PadData(
    val id: Int,
    val name: String,
    val bank: PadBank,
    val colorHex: Long,
    val soundRecipe: String,
    val pitch: Float = 0f,            // -12.0 to +12.0 semitones
    val decay: Float = 0.5f,          // 0.05 to 2.5s
    val filterCutoff: Float = 20000f, // 200Hz to 20000Hz
    val filterResonance: Float = 1.0f,// 0.5 to 4.5 Q
    val bitcrushBits: Int = 16,       // 1 to 16 bits
    val reverbSend: Float = 0.0f,     // 0.0 to 1.0
    val delaySend: Float = 0.0f,      // 0.0 to 1.0
    val isFxChainBypassed: Boolean = false,
    val sampleStartRatio: Float = 0.0f, // 0.0 to 1.0 (Start point)
    val sampleEndRatio: Float = 1.0f,   // 0.0 to 1.0 (End point)
    val loopStartRatio: Float = 0.0f,   // 0.0 to 1.0 (Loop start)
    val loopEndRatio: Float = 1.0f,     // 0.0 to 1.0 (Loop end)
    val isLoopEnabled: Boolean = false,
    val chokeGroup: Int = 0,          // 0: no choke, 1: hi-hats, 2: splice chops
    val volume: Float = 0.9f,
    val pan: Float = 0f,
    val sliceIndex: Int = -1,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false
)

data class StepData(
    val stepIndex: Int,
    val active: Boolean = false,
    val velocity: Float = 0.8f,
    val pitchOffset: Int = 0,
    val isAccent: Boolean = false
)

data class TrackData(
    val trackId: Int,
    val name: String,
    val soundRecipe: String,
    val colorHex: Long,
    val steps: List<StepData>,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val volume: Float = 0.85f
)

data class Pattern(
    val id: String,
    val name: String,
    val tracks: List<TrackData>
)

data class SpliceSample(
    val id: String,
    val title: String,
    val packName: String,
    val category: String,
    val bpm: Int,
    val key: String,
    val durationSec: Float,
    val wavePeaks: List<Float>,
    val soundRecipe: String,
    val genre: String = "House",
    val instrument: String = "Drums",
    val mood: String = "Punchy",
    val tags: List<String> = listOf("House", "Drums", "Punchy")
)

data class SplicePack(
    val id: String,
    val title: String,
    val author: String,
    val artworkDrawable: String,
    val description: String,
    val tags: List<String>,
    val samples: List<SpliceSample>
)

data class GridSnapshot(
    val slotIndex: Int,
    val name: String,
    val activeBank: PadBank,
    val drumsPads: List<PadData>,
    val bassPads: List<PadData>,
    val synthPads: List<PadData>,
    val splicePads: List<PadData>,
    val pattern: Pattern,
    val bpm: Int,
    val swingPercent: Int,
    val filterCutoff: Float = 20000f,
    val filterResonance: Float = 1.0f,
    val delayWet: Float = 0.2f,
    val reverbWet: Float = 0.15f,
    val timestampMs: Long = System.currentTimeMillis()
)

data class SliceMarker(
    val sliceIndex: Int,
    val startRatio: Float,
    val endRatio: Float,
    val name: String,
    val pitch: Float = 0f,
    val isReverse: Boolean = false
)

data class BeatProject(
    val id: String,
    val title: String,
    val bpm: Int = 120,
    val swingPercent: Int = 30, // "Schwung" groove percentage (0..75)
    val masterVolume: Float = 0.9f,
    val patterns: List<Pattern>,
    val activePatternIndex: Int = 0,
    val selectedSpliceSampleId: String = "lofi_guitar_01"
)
