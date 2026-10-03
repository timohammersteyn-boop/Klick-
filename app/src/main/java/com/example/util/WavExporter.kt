package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.PadBank
import com.example.model.PadData
import com.example.model.Pattern
import com.example.model.StepData
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

enum class WavExportType {
    FULL_MASTER_MIX,
    STEM_DRUMS,
    STEM_BASS,
    STEM_SYNTH,
    STEM_SPLICE
}

object WavExporter {

    private const val SAMPLE_RATE = 44100
    private const val CHANNELS = 2 // Stereo
    private const val BITS_PER_SAMPLE = 16

    fun exportSequenceToWav(
        context: Context,
        pattern: Pattern,
        padsByBank: Map<PadBank, List<PadData>>,
        bpm: Int,
        exportType: WavExportType,
        customFilename: String? = null
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val filename = customFilename ?: when (exportType) {
            WavExportType.FULL_MASTER_MIX -> "Master_Mix_${pattern.name.replace(" ", "_")}.wav"
            WavExportType.STEM_DRUMS -> "Stem_Drums_${pattern.name.replace(" ", "_")}.wav"
            WavExportType.STEM_BASS -> "Stem_Bass808_${pattern.name.replace(" ", "_")}.wav"
            WavExportType.STEM_SYNTH -> "Stem_Synth_${pattern.name.replace(" ", "_")}.wav"
            WavExportType.STEM_SPLICE -> "Stem_Splice_${pattern.name.replace(" ", "_")}.wav"
        }

        val wavFile = File(exportDir, filename)
        if (wavFile.exists()) wavFile.delete()

        // Calculate sequence duration (16 steps in seconds)
        val stepDurationSec = (60.0 / bpm) / 4.0
        val totalDurationSec = stepDurationSec * 16.0
        val totalNumSamples = (totalDurationSec * SAMPLE_RATE).toInt()

        // Buffer for stereo float audio samples (-1.0f to 1.0f)
        val leftBuffer = FloatArray(totalNumSamples)
        val rightBuffer = FloatArray(totalNumSamples)

        // Synthesize active tracks
        val tracksToRender = when (exportType) {
            WavExportType.FULL_MASTER_MIX -> pattern.tracks
            WavExportType.STEM_DRUMS -> pattern.tracks.filter { it.trackId == 0 }
            WavExportType.STEM_BASS -> pattern.tracks.filter { it.trackId == 1 }
            WavExportType.STEM_SYNTH -> pattern.tracks.filter { it.trackId == 2 }
            WavExportType.STEM_SPLICE -> pattern.tracks.filter { it.trackId == 3 }
        }

        for (track in tracksToRender) {
            val bank = when (track.trackId) {
                0 -> PadBank.DRUMS
                1 -> PadBank.BASS
                2 -> PadBank.SYNTH
                else -> PadBank.SPLICE
            }
            val bankPads = padsByBank[bank] ?: emptyList()

            for (step in track.steps) {
                if (!step.active) continue

                val stepStartSample = (step.stepIndex * stepDurationSec * SAMPLE_RATE).toInt()
                val targetPad = bankPads.getOrNull(step.stepIndex % bankPads.size.coerceAtLeast(1))
                    ?: bankPads.firstOrNull() ?: continue

                // Synthesize pad sound waveform into PCM buffer
                synthesizePadIntoBuffer(
                    pad = targetPad,
                    step = step,
                    startSample = stepStartSample,
                    leftBuffer = leftBuffer,
                    rightBuffer = rightBuffer
                )
            }
        }

        // Write WAV file with RIFF 44-byte Header
        writeWavFile(wavFile, leftBuffer, rightBuffer)
        return wavFile
    }

    fun exportPadSoundToWav(
        context: Context,
        pad: PadData,
        bpm: Int
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val wavFile = File(exportDir, "Pad_${pad.bank.label}_${pad.id + 1}_${pad.name.replace(" ", "_")}.wav")
        if (wavFile.exists()) wavFile.delete()

        val durationSec = pad.decay.toDouble().coerceIn(0.1, 3.0)
        val totalNumSamples = (durationSec * SAMPLE_RATE).toInt()

        val leftBuffer = FloatArray(totalNumSamples)
        val rightBuffer = FloatArray(totalNumSamples)

        val dummyStep = StepData(stepIndex = 0, active = true, velocity = pad.volume)
        synthesizePadIntoBuffer(pad, dummyStep, 0, leftBuffer, rightBuffer)

        writeWavFile(wavFile, leftBuffer, rightBuffer)
        return wavFile
    }

    private fun synthesizePadIntoBuffer(
        pad: PadData,
        step: StepData,
        startSample: Int,
        leftBuffer: FloatArray,
        rightBuffer: FloatArray
    ) {
        val recipe = pad.soundRecipe
        val decaySec = pad.decay.toDouble().coerceIn(0.05, 2.0)
        val soundSampleLength = (decaySec * SAMPLE_RATE).toInt()
        val endSample = (startSample + soundSampleLength).coerceAtMost(leftBuffer.size)

        val vol = step.velocity * pad.volume
        val pan = pad.pan.coerceIn(-1.0f, 1.0f)
        val leftGain = (vol * (1.0f - pan) / 2.0f).coerceIn(0f, 1f)
        val rightGain = (vol * (1.0f + pan) / 2.0f).coerceIn(0f, 1f)

        // Pitch multiplier
        val pitchFactor = Math.pow(2.0, (pad.pitch + step.pitchOffset) / 12.0)
        val baseFreq = when {
            recipe.contains("kick", true) -> 60.0
            recipe.contains("808", true) || recipe.contains("bass", true) -> 55.0
            recipe.contains("snare", true) -> 220.0
            recipe.contains("hat", true) -> 800.0
            recipe.contains("rhodes", true) || recipe.contains("synth", true) -> 440.0
            else -> 330.0
        } * pitchFactor

        val cutoffFactor = if (pad.isFxChainBypassed) 1.0 else (pad.filterCutoff / 20000.0).coerceIn(0.1, 1.0)

        for (i in startSample until endSample) {
            val t = (i - startSample).toDouble() / SAMPLE_RATE
            val env = Math.exp(-t * (4.0 / decaySec))

            var sampleVal = when {
                recipe.contains("kick", true) -> {
                    val freqPitchEnv = baseFreq * (1.0 + Math.exp(-t * 35.0) * 3.0)
                    sin(2.0 * Math.PI * freqPitchEnv * t)
                }
                recipe.contains("808", true) -> {
                    val sub = sin(2.0 * Math.PI * baseFreq * t)
                    val dist = (sub * 1.5).coerceIn(-1.0, 1.0)
                    dist
                }
                recipe.contains("snare", true) -> {
                    val tone = sin(2.0 * Math.PI * baseFreq * t) * 0.4
                    val noise = (Math.random() * 2.0 - 1.0) * 0.6
                    tone + noise
                }
                recipe.contains("hat", true) -> {
                    (Math.random() * 2.0 - 1.0) * env
                }
                else -> {
                    val fundamental = sin(2.0 * Math.PI * baseFreq * t)
                    val harmonic = sin(2.0 * Math.PI * baseFreq * 2.0 * t) * 0.3
                    (fundamental + harmonic)
                }
            } * env * cutoffFactor

            // Bitcrusher
            if (!pad.isFxChainBypassed && pad.bitcrushBits < 16) {
                val levels = pad.bitcrushBits.coerceAtLeast(2)
                sampleVal = (Math.round(sampleVal * levels) / levels.toDouble())
            }

            leftBuffer[i] = (leftBuffer[i] + (sampleVal * leftGain).toFloat()).coerceIn(-1.0f, 1.0f)
            rightBuffer[i] = (rightBuffer[i] + (sampleVal * rightGain).toFloat()).coerceIn(-1.0f, 1.0f)
        }
    }

    private fun writeWavFile(file: File, left: FloatArray, right: FloatArray) {
        val numSamples = left.size
        val audioDataSize = numSamples * CHANNELS * (BITS_PER_SAMPLE / 8)
        val fileSizeBytes = 36 + audioDataSize

        val fos = FileOutputStream(file)

        // 44-Byte RIFF WAV Header
        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put("RIFF".toByteArray())
            putInt(fileSizeBytes)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // Subchunk1Size for PCM
            putShort(1.toShort()) // AudioFormat = 1 (PCM)
            putShort(CHANNELS.toShort())
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)) // ByteRate
            putShort((CHANNELS * (BITS_PER_SAMPLE / 8)).toShort()) // BlockAlign
            putShort(BITS_PER_SAMPLE.toShort())
            put("data".toByteArray())
            putInt(audioDataSize)
        }

        fos.write(header.array())

        // PCM Sample Payload (16-Bit Signed Stereo Short)
        val pcmBuffer = ByteBuffer.allocate(audioDataSize).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until numSamples) {
                val lShort = (left[i] * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                val rShort = (right[i] * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                putShort(lShort)
                putShort(rShort)
            }
        }

        fos.write(pcmBuffer.array())
        fos.flush()
        fos.close()
    }

    fun shareWavFile(context: Context, file: File, title: String = "Export WAV Sequence") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                putExtra(Intent.EXTRA_TEXT, "Exported sequence WAV file from Schwung Groovebox!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, title))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
