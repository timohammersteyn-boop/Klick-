package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bpm: Int = 120,
    val patternDataJson: String,
    val timestampMs: Long = System.currentTimeMillis()
)
