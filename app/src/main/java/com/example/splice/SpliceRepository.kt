package com.example.splice

import com.example.data.SpliceSampleDao
import com.example.data.SpliceSampleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository providing offline-first access to Splice sample metadata.
 * Queries the app backend (which secures all 3rd-party API credentials)
 * and caches results in Room Database for instant offline playback on PadGrid.
 */
class SpliceRepository(
    private val spliceApi: SpliceApiService,
    private val spliceSampleDao: SpliceSampleDao
) {

    val cachedSamples: Flow<List<SpliceSampleEntity>> = spliceSampleDao.getAllSamples()

    fun getSamplesByCategory(category: String): Flow<List<SpliceSampleEntity>> {
        return spliceSampleDao.getSamplesByCategory(category)
    }

    fun getAssignedSamplesForBank(bank: String): Flow<List<SpliceSampleEntity>> {
        return spliceSampleDao.getAssignedSamplesForBank(bank)
    }

    suspend fun refreshSampleCatalog(query: String = "drums", category: String? = null) {
        withContext(Dispatchers.IO) {
            try {
                // Calls backend proxy without needing client-side API keys
                val response = try {
                    spliceApi.searchSamples(query = query, category = category)
                } catch (apiError: Exception) {
                    // Graceful fallback to default offline package if backend unreachable
                    createDefaultFallbackCatalog()
                }

                val entities = response.samples.map { dto ->
                    SpliceSampleEntity(
                        id = dto.id,
                        title = dto.title,
                        packName = dto.packName ?: "Splice Essentials",
                        category = dto.category ?: category ?: "DRUMS",
                        bpm = dto.bpm ?: 120,
                        keySignature = dto.keySignature ?: "C Minor",
                        audioUrl = dto.previewUrl,
                        durationMs = dto.durationMs ?: 1000L,
                        tags = dto.tags?.joinToString(",") ?: ""
                    )
                }

                spliceSampleDao.insertAllSamples(entities)
            } catch (e: Exception) {
                // Local Room cache remains fully accessible offline
            }
        }
    }

    suspend fun assignSampleToPad(sampleId: String, padId: Int, bank: String) {
        spliceSampleDao.assignSampleToPad(sampleId, padId, bank)
    }

    suspend fun updateLocalAudioPath(sampleId: String, localPath: String) {
        spliceSampleDao.updateLocalAudioPath(sampleId, localPath)
    }

    private fun createDefaultFallbackCatalog(): SpliceSearchResponse {
        return SpliceSearchResponse(
            samples = listOf(
                SpliceSampleDto(
                    id = "sp_kick_01",
                    title = "Punchy Sub Kick",
                    packName = "Splice Lo-Fi Drums",
                    category = "DRUMS",
                    bpm = 90,
                    keySignature = "C",
                    previewUrl = "https://example.com/audio/kick01.wav"
                ),
                SpliceSampleDto(
                    id = "sp_snare_01",
                    title = "Crisp Vintage Snare",
                    packName = "Splice Lo-Fi Drums",
                    category = "DRUMS",
                    bpm = 90,
                    keySignature = "C",
                    previewUrl = "https://example.com/audio/snare01.wav"
                ),
                SpliceSampleDto(
                    id = "sp_bass_808",
                    title = "Heavy 808 Glide",
                    packName = "Splice Trap Essentials",
                    category = "BASS",
                    bpm = 140,
                    keySignature = "F# Minor",
                    previewUrl = "https://example.com/audio/808_01.wav"
                ),
                SpliceSampleDto(
                    id = "sp_synth_lead",
                    title = "Chilled Poly Synth",
                    packName = "Splice Synth Vault",
                    category = "SYNTH",
                    bpm = 120,
                    keySignature = "A Minor",
                    previewUrl = "https://example.com/audio/synth01.wav"
                )
            )
        )
    }
}
