package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppMode
import com.example.spotify.DjDeckId
import com.example.spotify.DjDeckState
import com.example.spotify.SpotifyTrack
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel
import kotlin.math.sin

@Composable
fun DjWorkstationView(
    viewModel: SchwungViewModel,
    modifier: Modifier = Modifier
) {
    val deckA by viewModel.spotifyPlayer.deckAState.collectAsState()
    val deckB by viewModel.spotifyPlayer.deckBState.collectAsState()
    val crossfader by viewModel.spotifyPlayer.crossfader.collectAsState()
    val spotifyTracks by viewModel.spotifyTracks.collectAsState()
    val searchQuery by viewModel.spotifySearchQuery.collectAsState()

    var showSpotifyDialog by remember { mutableStateOf(false) }
    var showTrackBrowser by remember { mutableStateOf(false) }
    var selectedDeckForLoading by remember { mutableStateOf(DjDeckId.DECK_A) }

    // Initialize initial demo tracks if empty
    LaunchedEffect(Unit) {
        if (deckA.loadedTrack == null && spotifyTracks.isNotEmpty()) {
            viewModel.loadSpotifyTrackToDeck(DjDeckId.DECK_A, spotifyTracks[0])
        }
        if (deckB.loadedTrack == null && spotifyTracks.size > 1) {
            viewModel.loadSpotifyTrackToDeck(DjDeckId.DECK_B, spotifyTracks[1])
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChassisBlack)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Pioneer USB Status & Spotify Connect Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonLime)
                )
                Text(
                    text = "PIONEER DJ WORKSTATION // USB HOST",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Spotify Account Dialog Trigger
                Button(
                    onClick = { showSpotifyDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SPOTIFY CONNECT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }

                // Track Browser Toggle
                Button(
                    onClick = { showTrackBrowser = !showTrackBrowser },
                    colors = ButtonDefaults.buttonColors(containerColor = ChassisSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChassisBorder),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (showTrackBrowser) "HIDE TRACKS" else "LOAD TRACKS", fontSize = 8.sp, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Pioneer USB Audio Routing Summary Card
        PioneerRoutingTab(
            pioneerManager = viewModel.pioneerManager,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Main Dual-Deck Workspace
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // DECK A
            DeckPanel(
                deckState = deckA,
                deckColor = NeonCyan,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onPlayPause = { viewModel.spotifyPlayer.togglePlayPause(DjDeckId.DECK_A) },
                onCue = { viewModel.spotifyPlayer.triggerCue(DjDeckId.DECK_A) },
                onJogScratch = { delta -> viewModel.spotifyPlayer.jogWheelScratch(DjDeckId.DECK_A, delta) },
                onPitchChange = { pitch -> viewModel.spotifyPlayer.setPitchPercent(DjDeckId.DECK_A, pitch) },
                onHotCue = { idx -> viewModel.spotifyPlayer.triggerHotCue(DjDeckId.DECK_A, idx) },
                onAutoLoop = { beats -> viewModel.spotifyPlayer.toggleAutoLoop(DjDeckId.DECK_A, beats) },
                onSliceToPadGrid = { track ->
                    viewModel.sliceSpotifyTrackToPads(track)
                    viewModel.setAppMode(AppMode.PAD_GRID)
                },
                onLoadTrackClicked = {
                    selectedDeckForLoading = DjDeckId.DECK_A
                    showTrackBrowser = true
                }
            )

            // DECK B
            DeckPanel(
                deckState = deckB,
                deckColor = NeonOrange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onPlayPause = { viewModel.spotifyPlayer.togglePlayPause(DjDeckId.DECK_B) },
                onCue = { viewModel.spotifyPlayer.triggerCue(DjDeckId.DECK_B) },
                onJogScratch = { delta -> viewModel.spotifyPlayer.jogWheelScratch(DjDeckId.DECK_B, delta) },
                onPitchChange = { pitch -> viewModel.spotifyPlayer.setPitchPercent(DjDeckId.DECK_B, pitch) },
                onHotCue = { idx -> viewModel.spotifyPlayer.triggerHotCue(DjDeckId.DECK_B, idx) },
                onAutoLoop = { beats -> viewModel.spotifyPlayer.toggleAutoLoop(DjDeckId.DECK_B, beats) },
                onSliceToPadGrid = { track ->
                    viewModel.sliceSpotifyTrackToPads(track)
                    viewModel.setAppMode(AppMode.PAD_GRID)
                },
                onLoadTrackClicked = {
                    selectedDeckForLoading = DjDeckId.DECK_B
                    showTrackBrowser = true
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Pioneer Mixer & Crossfader Row
        PioneerCrossfaderSection(
            crossfader = crossfader,
            onCrossfaderChange = { viewModel.spotifyPlayer.setCrossfader(it) }
        )

        // Modal Spotify Track Search Browser
        if (showTrackBrowser) {
            SpotifyTrackBrowserSheet(
                tracks = spotifyTracks,
                searchQuery = searchQuery,
                onSearch = { viewModel.searchSpotifyTracks(it) },
                targetDeck = selectedDeckForLoading,
                onLoad = { track ->
                    viewModel.loadSpotifyTrackToDeck(selectedDeckForLoading, track)
                    showTrackBrowser = false
                },
                onDismiss = { showTrackBrowser = false }
            )
        }

        // Spotify Login / API Setup Dialog
        if (showSpotifyDialog) {
            SpotifyLoginDialog(
                viewModel = viewModel,
                onDismiss = { showSpotifyDialog = false }
            )
        }
    }
}

@Composable
fun DeckPanel(
    deckState: DjDeckState,
    deckColor: Color,
    modifier: Modifier = Modifier,
    onPlayPause: () -> Unit,
    onCue: () -> Unit,
    onJogScratch: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onHotCue: (Int) -> Unit,
    onAutoLoop: (Float) -> Unit,
    onSliceToPadGrid: (SpotifyTrack) -> Unit,
    onLoadTrackClicked: () -> Unit
) {
    val track = deckState.loadedTrack

    Card(
        modifier = modifier.border(1.dp, deckColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = ChassisDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            // Deck Header: Deck ID, Track Title, BPM, Key
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(deckColor)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = deckState.deckId.name.replace("DECK_", "DECK "),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                    Text(
                        text = track?.name ?: "No Track Loaded",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                TextButton(
                    onClick = onLoadTrackClicked,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(22.dp)
                ) {
                    Text("LOAD", fontSize = 8.sp, color = deckColor, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "${track?.artist ?: "Unknown Artist"} // ${String.format("%.1f", deckState.effectiveBpm)} BPM // ${track?.musicalKey ?: "8A"}",
                fontSize = 8.sp,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Scrolling Waveform Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .border(1.dp, ChassisBorder, RoundedCornerShape(4.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val progress = if (deckState.durationMs > 0) deckState.currentPositionMs.toFloat() / deckState.durationMs else 0f
                    val playheadX = progress * w

                    // Draw waveform bars
                    val bars = track?.waveformData ?: (0..31).map { 0.5f }
                    val barWidth = w / bars.size
                    bars.forEachIndexed { i, peak ->
                        val barHeight = peak * (h * 0.8f)
                        drawRect(
                            color = if (i * barWidth < playheadX) deckColor else Color(0xFF334155),
                            topLeft = Offset(i * barWidth, (h - barHeight) / 2f),
                            size = Size(barWidth - 1f, barHeight)
                        )
                    }

                    // Playhead Line
                    drawLine(
                        color = Color.White,
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, h),
                        strokeWidth = 2f
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Pioneer Jog Wheel & Pitch Fader Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Interactive Pioneer Jog Wheel
                PioneerJogWheel(
                    isPlaying = deckState.isPlaying,
                    deckColor = deckColor,
                    onScratch = onJogScratch,
                    modifier = Modifier.size(90.dp)
                )

                // Pitch Fader & Tempo Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp)
                ) {
                    Text(
                        text = "${if (deckState.pitchPercent >= 0) "+" else ""}${String.format("%.1f", deckState.pitchPercent)}%",
                        fontSize = 8.sp,
                        color = deckColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = deckState.pitchPercent,
                        onValueChange = onPitchChange,
                        valueRange = -16.0f..16.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = deckColor,
                            activeTrackColor = deckColor
                        ),
                        modifier = Modifier.height(20.dp)
                    )
                    OutlinedButton(
                        onClick = { onPitchChange(0f) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .height(20.dp)
                            .width(42.dp),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text("RESET", fontSize = 7.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Transport: Play (Green) & Cue (Amber)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Cue Button
                Button(
                    onClick = onCue,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("CUE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }

                // Play / Pause Button
                Button(
                    onClick = onPlayPause,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (deckState.isPlaying) Color(0xFF00FF66) else ChassisSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (deckState.isPlaying) Color(0xFF00FF66) else Color(0xFF00FF66).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(30.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (deckState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (deckState.isPlaying) Color.Black else Color(0xFF00FF66),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (deckState.isPlaying) "PLAYING" else "PLAY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (deckState.isPlaying) Color.Black else Color(0xFF00FF66)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Hot Cues 1-8 Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (i in 0..7) {
                    val hasCue = deckState.hotCues.getOrNull(i) != null
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (hasCue) deckColor else ChassisSurface)
                            .border(1.dp, if (hasCue) Color.White else ChassisBorder, RoundedCornerShape(2.dp))
                            .clickable { onHotCue(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${i + 1}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasCue) Color.Black else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Slicing to PadGrid Action Button
            if (track != null) {
                Button(
                    onClick = { onSliceToPadGrid(track) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, tint = Color.Black, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SLICE SPOTIFY TO 4x4 PADS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun PioneerJogWheel(
    isPlaying: Boolean,
    deckColor: Color,
    onScratch: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jog_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "jog_rotation"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black)
            .border(2.dp, deckColor, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Drag X or Y determines scratch scrub delta
                    val scratchDelta = (dragAmount.x + dragAmount.y) * 15.0f
                    onScratch(scratchDelta)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().rotate(if (isPlaying) rotation else 0f)) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)

            // Outer Illuminated Ring
            drawCircle(deckColor.copy(alpha = 0.25f), radius = r - 4f, center = c)
            drawCircle(deckColor, radius = r - 4f, center = c, style = Stroke(width = 3f))

            // Pioneer LED Marker on outer platter
            drawCircle(
                color = Color.White,
                radius = 5f,
                center = Offset(c.x + (r - 12f) * sin(0.0).toFloat(), c.y - (r - 12f))
            )

            // Inner Jog Display
            drawCircle(Color(0xFF14161F), radius = r * 0.55f, center = c)
            drawCircle(Color.DarkGray, radius = r * 0.55f, center = c, style = Stroke(width = 1.5f))
        }

        // Center Pioneer Label
        Text(
            text = "PIONEER",
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            color = Color.LightGray,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun PioneerCrossfaderSection(
    crossfader: Float,
    onCrossfaderChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ChassisDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChassisBorder),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("DECK A", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            Slider(
                value = crossfader,
                onValueChange = onCrossfaderChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = NeonOrange,
                    inactiveTrackColor = NeonCyan
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
                    .padding(horizontal = 8.dp)
            )
            Text("DECK B", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyTrackBrowserSheet(
    tracks: List<SpotifyTrack>,
    searchQuery: String,
    onSearch: (String) -> Unit,
    targetDeck: DjDeckId,
    onLoad: (SpotifyTrack) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ChassisDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOAD TO ${targetDeck.name.replace("_", " ")}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1DB954)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearch,
                placeholder = { Text("Search Spotify Tracks, Artists...", fontSize = 11.sp, color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1DB954),
                    unfocusedBorderColor = ChassisBorder,
                    focusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(tracks) { track ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLoad(track) },
                        colors = CardDefaults.cardColors(containerColor = ChassisSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChassisBorder),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    "${track.artist} // ${track.bpm.toInt()} BPM // ${track.musicalKey}",
                                    fontSize = 9.sp,
                                    color = Color(0xFF1DB954),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Button(
                                onClick = { onLoad(track) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("LOAD", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
