package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SpliceSampleDao {

    @Query("SELECT * FROM splice_samples ORDER BY lastUpdatedMs DESC")
    fun getAllSamples(): Flow<List<SpliceSampleEntity>>

    @Query("SELECT * FROM splice_samples WHERE category = :category ORDER BY title ASC")
    fun getSamplesByCategory(category: String): Flow<List<SpliceSampleEntity>>

    @Query("SELECT * FROM splice_samples WHERE assignedBank = :bank AND assignedPadId IS NOT NULL ORDER BY assignedPadId ASC")
    fun getAssignedSamplesForBank(bank: String): Flow<List<SpliceSampleEntity>>

    @Query("SELECT * FROM splice_samples WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteSamples(): Flow<List<SpliceSampleEntity>>

    @Query("SELECT * FROM splice_samples WHERE id = :sampleId LIMIT 1")
    suspend fun getSampleById(sampleId: String): SpliceSampleEntity?

    @Query("SELECT * FROM splice_samples WHERE title LIKE '%' || :query || '%' OR packName LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchLocalSamples(query: String): Flow<List<SpliceSampleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: SpliceSampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSamples(samples: List<SpliceSampleEntity>)

    @Query("UPDATE splice_samples SET assignedPadId = :padId, assignedBank = :bank WHERE id = :sampleId")
    suspend fun assignSampleToPad(sampleId: String, padId: Int, bank: String)

    @Query("UPDATE splice_samples SET cachedLocalPath = :localPath WHERE id = :sampleId")
    suspend fun updateLocalAudioPath(sampleId: String, localPath: String)

    @Query("UPDATE splice_samples SET isFavorite = :isFavorite WHERE id = :sampleId")
    suspend fun updateFavoriteStatus(sampleId: String, isFavorite: Boolean)

    @Delete
    suspend fun deleteSample(sample: SpliceSampleEntity)

    @Query("DELETE FROM splice_samples WHERE id = :sampleId")
    suspend fun deleteSampleById(sampleId: String)

    @Query("DELETE FROM splice_samples")
    suspend fun clearCache()
}
