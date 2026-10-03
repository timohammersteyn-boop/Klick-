package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SpliceSampleDao {

    @Query("SELECT * FROM splice_sample_cache ORDER BY timestampMs DESC")
    fun getAllCachedSamples(): Flow<List<SpliceSampleEntity>>

    @Query("SELECT * FROM splice_sample_cache WHERE category = :category ORDER BY title ASC")
    fun getSamplesByCategory(category: String): Flow<List<SpliceSampleEntity>>

    @Query("SELECT * FROM splice_sample_cache WHERE assignedPadId IS NOT NULL AND assignedBank = :bank ORDER BY assignedPadId ASC")
    fun getAssignedPadSamplesForBank(bank: String): Flow<List<SpliceSampleEntity>>

    @Query("SELECT * FROM splice_sample_cache WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteSamples(): Flow<List<SpliceSampleEntity>>

    @Query("SELECT * FROM splice_sample_cache WHERE id = :sampleId LIMIT 1")
    suspend fun getSampleById(sampleId: String): SpliceSampleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSample(sample: SpliceSampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSamples(samples: List<SpliceSampleEntity>)

    @Query("UPDATE splice_sample_cache SET assignedPadId = :padId, assignedBank = :bank WHERE id = :sampleId")
    suspend fun assignSampleToPad(sampleId: String, padId: Int, bank: String)

    @Query("UPDATE splice_sample_cache SET cachedLocalPath = :localPath WHERE id = :sampleId")
    suspend fun updateLocalAudioPath(sampleId: String, localPath: String)

    @Query("UPDATE splice_sample_cache SET isFavorite = :isFavorite WHERE id = :sampleId")
    suspend fun toggleFavoriteStatus(sampleId: String, isFavorite: Boolean)

    @Delete
    suspend fun deleteSample(sample: SpliceSampleEntity)

    @Query("DELETE FROM splice_sample_cache WHERE id = :sampleId")
    suspend fun deleteSampleById(sampleId: String)

    @Query("DELETE FROM splice_sample_cache")
    suspend fun clearAllSampleCache()
}
