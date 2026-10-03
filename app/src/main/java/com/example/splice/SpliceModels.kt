package com.example.splice

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpliceSampleDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "pack_name") val packName: String? = "Splice Vault",
    @Json(name = "category") val category: String? = "DRUMS",
    @Json(name = "bpm") val bpm: Int? = 120,
    @Json(name = "key") val keySignature: String? = "C Minor",
    @Json(name = "preview_url") val previewUrl: String,
    @Json(name = "duration_ms") val durationMs: Long? = 1000L,
    @Json(name = "tags") val tags: List<String>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class SpliceSearchResponse(
    @Json(name = "data") val samples: List<SpliceSampleDto>,
    @Json(name = "page") val page: Int? = 1,
    @Json(name = "total") val total: Int? = 0
)
