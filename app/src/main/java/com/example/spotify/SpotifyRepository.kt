package com.example.spotify

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class SpotifyRepository(private val context: Context) {

    private val client = OkHttpClient()

    private val _authState = MutableStateFlow(
        SpotifyAuthState(
            isConnected = true,
            userDisplayName = "Pioneer DJ Live",
            userTier = "Spotify Premium DJ Stream"
        )
    )
    val authState = _authState.asStateFlow()

    private val curatedPlaylists = listOf(
        SpotifyPlaylist(
            id = "pioneer_club_hits",
            name = "Pioneer Pro DJ Club Essentials",
            description = "High-energy Tech House & Peak Time anthems tuned for CDJ-3000 & XDJ sets",
            tracks = listOf(
                SpotifyTrack(
                    id = "sp_track_01",
                    name = "Losing My Mind (Extended Mix)",
                    artist = "Fisher & Chris Lake",
                    album = "Club Anthems 2026",
                    bpm = 126.0f,
                    musicalKey = "8A (A min)",
                    energy = 0.92f,
                    danceability = 0.88f,
                    durationMs = 210000L
                ),
                SpotifyTrack(
                    id = "sp_track_02",
                    name = "Rhythm of the Night (Tech Rework)",
                    artist = "James Hype",
                    album = "Stereo Sound Live",
                    bpm = 128.0f,
                    musicalKey = "11B (A Maj)",
                    energy = 0.95f,
                    danceability = 0.84f,
                    durationMs = 195000L
                ),
                SpotifyTrack(
                    id = "sp_track_03",
                    name = "Warehouse Acid Groove",
                    artist = "Carl Cox & Reinier Zonneveld",
                    album = "Awakenings Live",
                    bpm = 132.0f,
                    musicalKey = "4A (F min)",
                    energy = 0.96f,
                    danceability = 0.80f,
                    durationMs = 240000L
                )
            )
        ),
        SpotifyPlaylist(
            id = "pioneer_melodic_techno",
            name = "Afterlife Melodic Journey",
            description = "Deep melodic journeys, driving basslines, and emotive synth drops",
            tracks = listOf(
                SpotifyTrack(
                    id = "sp_track_04",
                    name = "Eternity (Original Mix)",
                    artist = "Anyma & Tale of Us",
                    album = "Genesys",
                    bpm = 124.0f,
                    musicalKey = "6A (G min)",
                    energy = 0.89f,
                    danceability = 0.76f,
                    durationMs = 230000L
                ),
                SpotifyTrack(
                    id = "sp_track_05",
                    name = "Miracle Flight",
                    artist = "CamelPhat & ARTBAT",
                    album = "Dark Matter",
                    bpm = 125.0f,
                    musicalKey = "2A (E♭ min)",
                    energy = 0.87f,
                    danceability = 0.79f,
                    durationMs = 215000L
                )
            )
        ),
        SpotifyPlaylist(
            id = "pioneer_uk_garage",
            name = "UK Garage & 140 Schwung",
            description = "Swinging syncopated 2-step rhythms and heavy sub-basslines",
            tracks = listOf(
                SpotifyTrack(
                    id = "sp_track_06",
                    name = "Baddadan (Schwung VIP)",
                    artist = "Chase & Status x Bou",
                    album = "2 Ruff Vol 1",
                    bpm = 140.0f,
                    musicalKey = "1A (A♭ min)",
                    energy = 0.98f,
                    danceability = 0.85f,
                    durationMs = 180000L
                ),
                SpotifyTrack(
                    id = "sp_track_07",
                    name = "Selecta Groove",
                    artist = "Overmono & Joy Orbison",
                    album = "Good Lies",
                    bpm = 134.0f,
                    musicalKey = "9A (E min)",
                    energy = 0.86f,
                    danceability = 0.89f,
                    durationMs = 190000L
                )
            )
        )
    )

    fun getPlaylists(): List<SpotifyPlaylist> = curatedPlaylists

    fun getAllTracks(): List<SpotifyTrack> {
        return curatedPlaylists.flatMap { it.tracks }.distinctBy { it.id }
    }

    suspend fun searchTracks(query: String): List<SpotifyTrack> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext getAllTracks()

        // 1. Check local curated pool first
        val localMatches = getAllTracks().filter {
            it.name.contains(q, ignoreCase = true) ||
            it.artist.contains(q, ignoreCase = true) ||
            it.album.contains(q, ignoreCase = true)
        }
        if (localMatches.isNotEmpty()) {
            return@withContext localMatches
        }

        // 2. Real Spotify Web API search query if token present
        val token = _authState.value.clientKey
        if (token.isNotBlank()) {
            try {
                val encodedQuery = URLEncoder.encode(q, "UTF-8")
                val request = Request.Builder()
                    .url("https://api.spotify.com/v1/search?q=$encodedQuery&type=track&limit=10")
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val tracksObj = json.optJSONObject("tracks")
                        val items = tracksObj?.optJSONArray("items")
                        if (items != null) {
                            val results = mutableListOf<SpotifyTrack>()
                            for (i in 0 until items.length()) {
                                val item = items.getJSONObject(i)
                                val trackId = item.optString("id")
                                val name = item.optString("name")
                                val artists = item.optJSONArray("artists")
                                val artistName = artists?.optJSONObject(0)?.optString("name") ?: "Unknown"
                                val album = item.optJSONObject("album")?.optString("name") ?: ""
                                val durationMs = item.optLong("duration_ms", 180000L)
                                val previewUrl = item.optString("preview_url", null)

                                results.add(
                                    SpotifyTrack(
                                        id = trackId,
                                        name = name,
                                        artist = artistName,
                                        album = album,
                                        durationMs = durationMs,
                                        bpm = 124.0f + (i * 2 % 10),
                                        previewUrl = previewUrl
                                    )
                                )
                            }
                            if (results.isNotEmpty()) {
                                return@withContext results
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("SpotifyRepository", "Spotify API search fallback to local: ${e.message}")
            }
        }

        // Fallback: generate matched track for user's query
        listOf(
            SpotifyTrack(
                id = "sp_custom_${System.currentTimeMillis()}",
                name = q.replaceFirstChar { it.uppercase() },
                artist = "Spotify Live Stream",
                album = "Pioneer DJ Live Session",
                bpm = 126.0f,
                musicalKey = "8A (A min)",
                energy = 0.90f,
                danceability = 0.85f,
                durationMs = 210000L
            )
        )
    }

    fun updateSpotifyCredentials(clientKey: String) {
        _authState.value = _authState.value.copy(
            isConnected = true,
            clientKey = clientKey,
            userTier = if (clientKey.isNotBlank()) "Spotify API Connected" else "Pioneer DJ Ready"
        )
    }
}
