package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "beat_projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val bpm: Int,
    val swingPercent: Int,
    val projectJson: String,
    val lastModified: Long = System.currentTimeMillis()
)
