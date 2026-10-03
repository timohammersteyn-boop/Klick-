package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "splice_samples")
data class SpliceSampleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val packName: String,
    val category: String,
    val bpm: Int = 120,
    val keySignature: String = "C Minor",
    val audioUrl: String,
    val cachedLocalPath: String? = null,
    val assignedPadId: Int? = null,
    val assignedBank: String? = null,
    val durationMs: Long = 1000L,
    val tags: String = "",
    val isFavorite: Boolean = false,
    val lastUpdatedMs: Long = System.currentTimeMillis()
)
