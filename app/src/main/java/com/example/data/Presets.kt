package com.example.data

import com.example.model.*

object Presets {

    fun getDefaultDrumsPads(): List<PadData> {
        val names = listOf(
            "808 Kick" to "kick_sub",
            "Punch Kick" to "kick_punch",
            "Trap Clap" to "clap_trap",
            "Ghost Clap" to "clap_ghost",
            "Tight Snare" to "snare_tight",
            "Acoustic Snare" to "snare_acoustic",
            "Closed Hat" to "hat_closed",
            "Open Hat" to "hat_open",
            "Low Tom" to "tom_low",
            "High Tom" to "tom_high",
            "Shaker" to "hat_closed",
            "Cowbell" to "perc_cowbell",
            "Rim Click" to "snare_tight",
            "Laser Zap" to "perc_zap",
            "Ride Cymbal" to "hat_open",
            "Vinyl FX" to "clap_trap"
        )

        return names.mapIndexed { index, (name, recipe) ->
            PadData(
                id = index,
                name = name,
                bank = PadBank.DRUMS,
                colorHex = 0xFFFF5252,
                soundRecipe = recipe,
                decay = when {
                    recipe.contains("hat_open") -> 1.1f
                    recipe.contains("kick") -> 0.8f
                    recipe.contains("snare") -> 0.6f
                    recipe.contains("closed") -> 0.2f
                    else -> 0.45f
                },
                chokeGroup = if (name.contains("Hat")) 1 else 0
            )
        }
    }

    fun getDefaultBassPads(): List<PadData> {
        val noteNames = listOf(
            "C1 Sub", "C#1 Sub", "D1 Sub", "D#1 Sub",
            "E1 Sub", "F1 Sub", "F#1 Sub", "G1 Sub",
            "G#1 Sub", "A1 Sub", "A#1 Sub", "B1 Sub",
            "C2 Glide", "D2 Glide", "E2 Glide", "G2 Acid"
        )

        return noteNames.mapIndexed { index, name ->
            val semitoneOffset = index.toFloat() - 7f // center around 0
            PadData(
                id = index,
                name = name,
                bank = PadBank.BASS,
                colorHex = 0xFFA855F7,
                soundRecipe = if (index == 15) "acid_303" else "808_bass",
                pitch = semitoneOffset,
                decay = 1.2f,
                chokeGroup = 3 // Bass monophonic choke
            )
        }
    }

    fun getDefaultSynthPads(): List<PadData> {
        val chordNames = listOf(
            "Cmaj7", "Dm7", "Em7", "Fmaj7",
            "G7", "Am7", "Bm7b5", "C6",
            "E Lead", "G Lead", "A Lead", "C High",
            "Vocal A", "Vocal E", "Vocal Ooh", "Vocal Chop"
        )

        return chordNames.mapIndexed { index, name ->
            val recipe = when {
                index >= 12 -> "vocal_chop"
                index >= 8 -> "synth_lead"
                else -> "lofi_keys"
            }
            PadData(
                id = index,
                name = name,
                bank = PadBank.SYNTH,
                colorHex = 0xFF00E5FF,
                soundRecipe = recipe,
                pitch = (index % 8).toFloat() - 4f,
                decay = 0.9f
            )
        }
    }

    fun getDefaultSpliceSlicesPads(sampleTitle: String = "Lo-Fi Guitar"): List<PadData> {
        return (0..15).map { index ->
            PadData(
                id = index,
                name = "Slice ${index + 1}",
                bank = PadBank.SPLICE,
                colorHex = 0xFFFFB300,
                soundRecipe = "splice_slice",
                sliceIndex = index,
                chokeGroup = 2 // MPC auto choke for slices
            )
        }
    }

    fun getSplicePacks(): List<SplicePack> {
        return listOf(
            SplicePack(
                id = "pack_lofi_soul",
                title = "Lo-Fi Soul & Vinyl Tape",
                author = "Schwung Sounds",
                artworkDrawable = "splice_lofi_art_1791012386979",
                description = "Warm dusty guitar chords, lush Rhodes keys, vinyl crackle and vintage MPC chops recorded through tube preamps.",
                tags = listOf("LO-FI", "HIP-HOP", "CHILL", "GUITAR", "VINYL"),
                samples = listOf(
                    SpliceSample("lofi_guitar_01", "Midnight Guitar Lick", "Lo-Fi Soul & Vinyl Tape", "MELODIC", 85, "C Maj", 4.0f, listOf(0.2f, 0.6f, 0.9f, 0.4f, 0.7f, 0.5f, 0.8f, 0.3f, 0.6f, 0.9f, 0.4f, 0.5f, 0.7f, 0.8f, 0.4f, 0.2f), "lofi_guitar", genre = "Lo-Fi", instrument = "Guitar", mood = "Chill", tags = listOf("Lo-Fi", "Guitar", "Chill", "Soulful")),
                    SpliceSample("lofi_rhodes_02", "Rainy Window Rhodes", "Lo-Fi Soul & Vinyl Tape", "KEYS", 80, "A Min", 4.0f, listOf(0.4f, 0.8f, 0.5f, 0.7f, 0.3f, 0.6f, 0.9f, 0.4f, 0.5f, 0.7f, 0.4f, 0.6f, 0.8f, 0.5f, 0.3f, 0.2f), "lofi_keys", genre = "Lo-Fi", instrument = "Keys", mood = "Warm", tags = listOf("Lo-Fi", "Keys", "Warm", "Dusty")),
                    SpliceSample("lofi_vocal_03", "Soulful Angel Chop", "Lo-Fi Soul & Vinyl Tape", "VOCAL", 85, "F Maj", 4.0f, listOf(0.1f, 0.5f, 0.8f, 0.9f, 0.6f, 0.4f, 0.7f, 0.9f, 0.3f, 0.6f, 0.8f, 0.5f, 0.3f, 0.2f, 0.5f, 0.1f), "vocal_soul", genre = "Soul", instrument = "Vocal", mood = "Emotional", tags = listOf("Soul", "Vocal", "Emotional")),
                    SpliceSample("lofi_drums_04", "Dusty Boom Bap Break", "Lo-Fi Soul & Vinyl Tape", "DRUM LOOP", 90, "N/A", 4.0f, listOf(0.9f, 0.3f, 0.7f, 0.4f, 0.8f, 0.2f, 0.7f, 0.4f, 0.9f, 0.3f, 0.8f, 0.4f, 0.7f, 0.2f, 0.6f, 0.3f), "kick_punch", genre = "Boom Bap", instrument = "Drums", mood = "Punchy", tags = listOf("Boom Bap", "Drums", "Punchy", "Vinyl"))
                )
            ),
            SplicePack(
                id = "pack_trap_vault",
                title = "808 Vault & Drill Heat",
                author = "Metro Grid",
                artworkDrawable = "splice_trap_art_1791012407976",
                description = "Bone-crushing 808 sub glides, razor-sharp hi-hat rolls, and dark brass stabs crafted for modern trap and drill.",
                tags = listOf("TRAP", "DRILL", "808", "SUB", "DARK"),
                samples = listOf(
                    SpliceSample("trap_808_01", "Subzero 808 Glide", "808 Vault & Drill Heat", "BASS 808", 140, "F Min", 4.0f, listOf(0.9f, 0.8f, 0.7f, 0.6f, 0.8f, 0.9f, 0.7f, 0.6f, 0.8f, 0.8f, 0.7f, 0.5f, 0.9f, 0.7f, 0.6f, 0.4f), "trap_808", genre = "Trap", instrument = "Bass 808", mood = "Dark", tags = listOf("Trap", "808", "Dark", "Heavy")),
                    SpliceSample("trap_melody_02", "Phantom Drill Bells", "808 Vault & Drill Heat", "MELODIC", 140, "D Min", 4.0f, listOf(0.3f, 0.7f, 0.4f, 0.8f, 0.2f, 0.6f, 0.9f, 0.4f, 0.3f, 0.7f, 0.5f, 0.8f, 0.3f, 0.6f, 0.8f, 0.2f), "synth_lead", genre = "Drill", instrument = "Melodic", mood = "Eerie", tags = listOf("Drill", "Bells", "Eerie")),
                    SpliceSample("trap_drums_03", "Spin Hat Stutter Loop", "808 Vault & Drill Heat", "DRUM LOOP", 140, "N/A", 4.0f, listOf(0.7f, 0.9f, 0.6f, 0.8f, 0.7f, 0.9f, 0.5f, 0.8f, 0.6f, 0.9f, 0.7f, 0.8f, 0.6f, 0.9f, 0.5f, 0.7f), "hat_closed", genre = "Trap", instrument = "Hi-Hat", mood = "Energetic", tags = listOf("Trap", "Hi-Hat", "Fast", "Stutter"))
                )
            ),
            SplicePack(
                id = "pack_neon_synth",
                title = "Neon Waves 1984",
                author = "Retrowave Lab",
                artworkDrawable = "splice_lofi_art_1791012386979",
                description = "Classic Jupiter & Juno analog synthesizers, punchy gated linndrum beats, and driving cyberpunk basslines.",
                tags = listOf("SYNTHWAVE", "80S", "RETRO", "CYBERPUNK"),
                samples = listOf(
                    SpliceSample("synth_arp_01", "Cyber Drive Arpeggio", "Neon Waves 1984", "SYNTH LOOP", 118, "A Min", 4.0f, listOf(0.5f, 0.7f, 0.9f, 0.6f, 0.8f, 0.5f, 0.7f, 0.9f, 0.6f, 0.8f, 0.5f, 0.7f, 0.9f, 0.6f, 0.8f, 0.4f), "neon_synth", genre = "Synthwave", instrument = "Synth Arp", mood = "Driving", tags = listOf("Synthwave", "Arp", "80s", "Driving")),
                    SpliceSample("synth_pad_02", "Sunset Highway Pad", "Neon Waves 1984", "KEYS", 118, "E Min", 4.0f, listOf(0.6f, 0.7f, 0.8f, 0.8f, 0.7f, 0.7f, 0.8f, 0.8f, 0.6f, 0.7f, 0.8f, 0.8f, 0.7f, 0.6f, 0.7f, 0.5f), "lofi_keys", genre = "Synthwave", instrument = "Pad", mood = "Warm", tags = listOf("Synthwave", "Pad", "Warm", "Nostalgic"))
                )
            )
        )
    }

    fun getDefaultInitialPatterns(): List<Pattern> {
        return listOf(
            createPattern("Pattern A", 0),
            createPattern("Pattern B", 1),
            createPattern("Pattern C", 2),
            createPattern("Pattern D", 3)
        )
    }

    private fun createPattern(name: String, patternIdx: Int): Pattern {
        // Track 0: Kick & Snare Drums
        val drumSteps = (0..15).map { step ->
            val active = when (step) {
                0, 7, 8, 10 -> true // Kick
                4, 12 -> true       // Snare
                else -> false
            }
            StepData(stepIndex = step, active = active, velocity = if (step == 0 || step == 4 || step == 12) 1.0f else 0.75f)
        }

        // Track 1: Hi-Hats & Percussion (Swing groove)
        val hatSteps = (0..15).map { step ->
            val active = step % 2 == 0 || (patternIdx > 0 && step % 4 == 3)
            StepData(stepIndex = step, active = active, velocity = if (step % 4 == 0) 0.9f else 0.65f)
        }

        // Track 2: 808 Sub Bass
        val bassSteps = (0..15).map { step ->
            val active = when (step) {
                0, 6, 10, 14 -> true
                else -> false
            }
            StepData(stepIndex = step, active = active, pitchOffset = if (step == 10) 3 else 0)
        }

        // Track 3: Splice Melodic Chops
        val spliceSteps = (0..15).map { step ->
            val active = when (step) {
                0, 4, 8, 11, 14 -> true
                else -> false
            }
            StepData(stepIndex = step, active = active, pitchOffset = (step % 4))
        }

        val tracks = listOf(
            TrackData(trackId = 0, name = "KICK / SNARE", soundRecipe = "kick_punch", colorHex = 0xFFFF5252, steps = drumSteps),
            TrackData(trackId = 1, name = "HATS / PERC", soundRecipe = "hat_closed", colorHex = 0xFFFFB300, steps = hatSteps),
            TrackData(trackId = 2, name = "808 BASS", soundRecipe = "808_bass", colorHex = 0xFFA855F7, steps = bassSteps),
            TrackData(trackId = 3, name = "SPLICE CHOPS", soundRecipe = "splice_slice", colorHex = 0xFF00E5FF, steps = spliceSteps)
        )

        return Pattern(id = "pat_${name.lowercase().replace(" ", "_")}", name = name, tracks = tracks)
    }

    fun getDemoProjects(): List<BeatProject> {
        return listOf(
            BeatProject(
                id = "proj_boombap",
                title = "90s Boom Bap Schwung",
                bpm = 92,
                swingPercent = 54, // classic heavy MPC swing
                patterns = getDefaultInitialPatterns(),
                activePatternIndex = 0,
                selectedSpliceSampleId = "lofi_guitar_01"
            ),
            BeatProject(
                id = "proj_lofi",
                title = "Midnight Lofi Tape",
                bpm = 84,
                swingPercent = 42,
                patterns = getDefaultInitialPatterns(),
                activePatternIndex = 0,
                selectedSpliceSampleId = "lofi_rhodes_02"
            ),
            BeatProject(
                id = "proj_trap",
                title = "Drill & Trap 140",
                bpm = 140,
                swingPercent = 20,
                patterns = getDefaultInitialPatterns(),
                activePatternIndex = 0,
                selectedSpliceSampleId = "trap_808_01"
            )
        )
    }
}
