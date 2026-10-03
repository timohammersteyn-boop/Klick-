package com.example

import com.example.audio.SchwungAudioEngine
import com.example.data.Presets
import com.example.model.PadBank
import com.example.model.RollRate
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SchwungLiveUnitTest {

    @Test
    fun testPadsInitialization() {
        val drums = Presets.getDefaultDrumsPads()
        assertEquals(16, drums.size)
        assertEquals(PadBank.DRUMS, drums.first().bank)

        val bass = Presets.getDefaultBassPads()
        assertEquals(16, bass.size)
        assertEquals(PadBank.BASS, bass.first().bank)

        val synth = Presets.getDefaultSynthPads()
        assertEquals(16, synth.size)
        assertEquals(PadBank.SYNTH, synth.first().bank)

        val splice = Presets.getDefaultSpliceSlicesPads()
        assertEquals(16, splice.size)
        assertEquals(PadBank.SPLICE, splice.first().bank)
    }

    @Test
    fun testSplicePacksAndSamples() {
        val packs = Presets.getSplicePacks()
        assertTrue(packs.isNotEmpty())
        for (pack in packs) {
            assertTrue(pack.samples.isNotEmpty())
            for (sample in pack.samples) {
                assertTrue(sample.wavePeaks.isNotEmpty())
                assertTrue(sample.bpm > 0)
            }
        }
    }

    @Test
    fun testAudioEnginePreloadedLoops() {
        val engine = SchwungAudioEngine()
        assertTrue(engine.preloadedLoops.containsKey("lofi_guitar"))
        assertTrue(engine.preloadedLoops.containsKey("trap_808"))
        assertTrue(engine.preloadedLoops.containsKey("neon_synth"))

        val loop = engine.preloadedLoops["lofi_guitar"]
        assertNotNull(loop)
        assertTrue(loop!!.isNotEmpty())
    }

    @Test
    fun testMcpSoundSynthesis() {
        val engine = SchwungAudioEngine()
        val kickBuffer = engine.synthesizeMcpSound("Punchy 90s boom bap kick")
        assertNotNull(kickBuffer)
        assertTrue(kickBuffer.isNotEmpty())

        val snareBuffer = engine.synthesizeMcpSound("Dusty vinyl acoustic snare")
        assertNotNull(snareBuffer)
        assertTrue(snareBuffer.isNotEmpty())
    }

    @Test
    fun testRollRateDivisions() {
        assertEquals(0.5f, RollRate.EIGHTH.division, 0.001f)
        assertEquals(0.25f, RollRate.SIXTEENTH.division, 0.001f)
        assertEquals(0.125f, RollRate.THIRTY_SECOND.division, 0.001f)
    }

    @Test
    fun testOboeAudioEngineWrapper() {
        val oboeEngine = com.example.audio.OboeAudioEngine()
        assertNotNull(oboeEngine)
        oboeEngine.setBpm(128)
        oboeEngine.setSchwung(40)
        oboeEngine.setMasterVolume(0.85f)
        oboeEngine.setFilter(12000f, 1.2f)
        oboeEngine.triggerSample(0, 0.9f, 0f, 0)
        oboeEngine.close()
    }

    @Test
    fun testPioneerMidiProcessing() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val pioneerManager = com.example.pioneer.PioneerDjCertifiedManager(context)

        var playReceived = false
        pioneerManager.onPlayPauseReceived = { deck ->
            if (deck == 0) playReceived = true
        }

        // Simulate Pioneer Play NoteOn byte: 0x90, 0x0B, 0x7F
        pioneerManager.processPioneerMidiBytes(byteArrayOf(0x90.toByte(), 0x0B.toByte(), 0x7F.toByte()), 0, 3)
        assertTrue(playReceived)

        var jogDelta = 0f
        pioneerManager.onJogWheelScratched = { deck, delta ->
            jogDelta = delta
        }

        // Simulate Pioneer Jog Wheel forward scratch CC 0x21: 0xB0, 0x21, 0x42
        pioneerManager.processPioneerMidiBytes(byteArrayOf(0xB0.toByte(), 0x21.toByte(), 0x42.toByte()), 0, 3)
        assertTrue(jogDelta > 0f)
    }

    @Test
    fun testSpotifyRepositoryTracks() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = com.example.spotify.SpotifyRepository(context)
        val playlists = repo.getPlaylists()
        assertTrue(playlists.isNotEmpty())
        val allTracks = repo.getAllTracks()
        assertTrue(allTracks.isNotEmpty())
        for (track in allTracks) {
            assertTrue(track.bpm > 0f)
            assertTrue(track.musicalKey.isNotEmpty())
            assertEquals(32, track.waveformData.size)
        }
    }
}
