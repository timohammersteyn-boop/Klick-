package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppMode
import com.example.model.PadBank
import com.example.model.PadData
import com.example.model.RollRate
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel
import kotlin.math.cos
import kotlin.math.sin

enum class MovePadMode(val label: String) {
    MOVE_HYBRID("16 SEQ + 16 PADS"),
    FULL_DRUM_16("4x4 FINGER PADS"),
    EXTENDED_32_SEQ("32-STEP SEQUENCER")
}

@Composable
fun AbletonMoveHardwareConsole(
    viewModel: SchwungViewModel,
    modifier: Modifier = Modifier,
    onOpenProjects: () -> Unit
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val bpm by viewModel.bpm.collectAsState()
    val swingPercent by viewModel.swingPercent.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val currentBank by viewModel.currentBank.collectAsState()
    val patterns by viewModel.patterns.collectAsState()
    val activePatIdx by viewModel.activePatternIndex.collectAsState()
    val activePadTriggers by viewModel.activePadTriggers.collectAsState()
    val rollRate by viewModel.rollRate.collectAsState()
    val vuLeft by viewModel.audioEngine.vuMeterLeft.collectAsState()
    val vuRight by viewModel.audioEngine.vuMeterRight.collectAsState()

    val drumsPads by viewModel.drumsPads.collectAsState()
    val bassPads by viewModel.bassPads.collectAsState()
    val synthPads by viewModel.synthPads.collectAsState()
    val splicePads by viewModel.splicePads.collectAsState()

    val currentPads = when (currentBank) {
        PadBank.DRUMS -> drumsPads
        PadBank.BASS -> bassPads
        PadBank.SYNTH -> synthPads
        PadBank.SPLICE -> splicePads
    }

    val currentPattern = patterns.getOrNull(activePatIdx) ?: patterns.first()
    var activeTrackIndex by remember { mutableStateOf(0) }
    var padLayoutMode by remember { mutableStateOf(MovePadMode.MOVE_HYBRID) }
    var showWavExportModal by remember { mutableStateOf(false) }
    var showExpandedVisualizer by remember { mutableStateOf(false) }
    var touchStripPosition by remember { mutableStateOf(0.5f) }
    var isShiftHeld by remember { mutableStateOf(false) }

    // Encoders Values
    val filterCutoff by viewModel.filterCutoff.collectAsState()
    val filterResonance by viewModel.filterResonance.collectAsState()
    val delayWet by viewModel.delayWet.collectAsState()
    val delayFeedback by viewModel.delayFeedback.collectAsState()
    val driveSaturation by viewModel.driveSaturation.collectAsState()

    // Outer Chassis: Matte dark anodized aluminum with speaker grilles and beveled bezel
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1014))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .border(1.5.dp, Color(0xFF383C4D), RoundedCornerShape(12.dp))
                .shadow(16.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black, spotColor = Color.Black),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161821)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Top Rear Bezel: Ableton Logo, Speaker Grilles, USB & Audio Ports
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Ableton Move branding & Speaker Grille
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Speaker Grille vent pattern
                        SpeakerGrillePattern(modifier = Modifier.size(width = 32.dp, height = 12.dp))

                        Text(
                            text = "ABLETON",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE2E8F0),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "MOVE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonOrange,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF222634))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SCHWUNG v1.7",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Center: Hardware Port Status Indicators
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pioneer DJ USB Host indicator
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (viewModel.pioneerManager.isConnected.collectAsState().value) NeonCyan.copy(alpha = 0.2f) else Color(0xFF1E212D))
                                .border(1.dp, if (viewModel.pioneerManager.isConnected.collectAsState().value) NeonCyan else Color.Transparent, RoundedCornerShape(3.dp))
                                .clickable { viewModel.setAppMode(AppMode.PIONEER_SPOTIFY_DJ) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PIONEER USB LINK",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = if (viewModel.pioneerManager.isConnected.collectAsState().value) NeonCyan else Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Oboe Engine Status
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (viewModel.audioEngine.isOboeActive) NeonLime.copy(alpha = 0.2f) else Color(0xFF1E212D))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (viewModel.audioEngine.isOboeActive) "OBOE LOW-LATENCY" else "AUDIO ENGINE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.audioEngine.isOboeActive) NeonLime else Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Spotify DJ Quick Switch
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF1DB954).copy(alpha = 0.2f))
                                .clickable { viewModel.setAppMode(AppMode.PIONEER_SPOTIFY_DJ) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SPOTIFY DJ",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1DB954),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // EXPORT WAV BUTTON
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(NeonAmber.copy(alpha = 0.2f))
                                .border(1.dp, NeonAmber, RoundedCornerShape(3.dp))
                                .clickable { showWavExportModal = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("export_wav_top_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, contentDescription = "Export WAV", tint = NeonAmber, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "EXPORT WAV",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonAmber,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // PAD FX OVERLAY BUTTON
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(NeonPurple.copy(alpha = 0.2f))
                                .border(1.dp, NeonPurple, RoundedCornerShape(3.dp))
                                .clickable { viewModel.openPadFxOverlay() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("open_pad_fx_top_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = "Pad FX", tint = NeonPurple, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "PAD FX",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonPurple,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Right: Right Speaker Grille & Projects Folder
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onOpenProjects,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF222634))
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "Projects", tint = NeonCyan, modifier = Modifier.size(14.dp))
                        }
                        SpeakerGrillePattern(modifier = Modifier.size(width = 32.dp, height = 12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Ableton Move 9 Endless Rotary Encoders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoveRotaryEncoder(
                        name = "MASTER",
                        displayValue = "${(viewModel.audioEngine.masterVolume * 100).toInt()}%",
                        valueRatio = viewModel.audioEngine.masterVolume,
                        color = Color.White,
                        onValueChange = { viewModel.audioEngine.masterVolume = it }
                    )
                    MoveRotaryEncoder(
                        name = "TRACK",
                        displayValue = "T${activeTrackIndex + 1}",
                        valueRatio = 0.8f,
                        color = when (activeTrackIndex) {
                            0 -> BankDrumsColor
                            1 -> BankBassColor
                            2 -> BankSynthColor
                            else -> BankSpliceColor
                        },
                        onValueChange = {}
                    )
                    MoveRotaryEncoder(
                        name = "CUTOFF",
                        displayValue = "${(filterCutoff / 1000).toInt()}k",
                        valueRatio = (filterCutoff - 100f) / 19900f,
                        color = NeonPurple,
                        onValueChange = { viewModel.setMasterFilterCutoff(100f + it * 19900f) }
                    )
                    MoveRotaryEncoder(
                        name = "RESON",
                        displayValue = String.format("%.1f", filterResonance),
                        valueRatio = (filterResonance - 0.5f) / 4.5f,
                        color = NeonPurple,
                        onValueChange = { viewModel.setMasterFilterResonance(0.5f + it * 4.5f) }
                    )
                    MoveRotaryEncoder(
                        name = "DELAY",
                        displayValue = "${(delayWet * 100).toInt()}%",
                        valueRatio = delayWet,
                        color = NeonCyan,
                        onValueChange = { viewModel.setDelayWet(it) }
                    )
                    MoveRotaryEncoder(
                        name = "F-BACK",
                        displayValue = "${(delayFeedback * 100).toInt()}%",
                        valueRatio = delayFeedback,
                        color = NeonTeal,
                        onValueChange = { viewModel.setDelayFeedback(it) }
                    )
                    MoveRotaryEncoder(
                        name = "DRIVE",
                        displayValue = "${(driveSaturation * 100).toInt()}%",
                        valueRatio = driveSaturation,
                        color = NeonCoral,
                        onValueChange = { viewModel.setDriveSaturation(it) }
                    )
                    MoveRotaryEncoder(
                        name = "TEMPO",
                        displayValue = "$bpm",
                        valueRatio = (bpm - 60f) / 120f,
                        color = NeonYellow,
                        onValueChange = { viewModel.setBpm((60 + it * 120).toInt()) }
                    )
                    MoveRotaryEncoder(
                        name = "SCHWUNG",
                        displayValue = "$swingPercent%",
                        valueRatio = swingPercent / 75f,
                        color = NeonOrange,
                        onValueChange = { viewModel.setSwingPercent((it * 75).toInt()) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Track Selector Buttons Row (4 Illuminated Tracks: Drum, Bass, Synth, Splice)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val trackLabels = listOf("1 DRUMS", "2 BASS 808", "3 KEYS/SYNTH", "4 SPLICE SAMPLER")
                    val trackColors = listOf(BankDrumsColor, BankBassColor, BankSynthColor, BankSpliceColor)
                    val banks = listOf(PadBank.DRUMS, PadBank.BASS, PadBank.SYNTH, PadBank.SPLICE)

                    trackLabels.forEachIndexed { i, label ->
                        val isTrackActive = activeTrackIndex == i
                        val color = trackColors[i]

                        Button(
                            onClick = {
                                activeTrackIndex = i
                                viewModel.setBank(banks[i])
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTrackActive) color.copy(alpha = 0.25f) else Color(0xFF1E212D)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isTrackActive) color else Color(0xFF2C3040)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isTrackActive) color else Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Pad Layout Mode Switcher (16 Seq + 16 Pads / 4x4 Pads / 32 Seq)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF222634))
                            .border(1.dp, Color(0xFF383C4D), RoundedCornerShape(4.dp))
                            .clickable {
                                padLayoutMode = when (padLayoutMode) {
                                    MovePadMode.MOVE_HYBRID -> MovePadMode.FULL_DRUM_16
                                    MovePadMode.FULL_DRUM_16 -> MovePadMode.EXTENDED_32_SEQ
                                    MovePadMode.EXTENDED_32_SEQ -> MovePadMode.MOVE_HYBRID
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = padLayoutMode.label,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Middle Main Section: Left Function/OLED Cluster + Center Main 32-Pad Grid
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left Function Cluster: OLED Display, Big Push Wheel, Function Buttons
                    AbletonMoveLeftCluster(
                        viewModel = viewModel,
                        isPlaying = isPlaying,
                        isRecording = isRecording,
                        bpm = bpm,
                        swingPercent = swingPercent,
                        currentStep = currentStep,
                        activeTrackIndex = activeTrackIndex,
                        vuLeft = vuLeft,
                        vuRight = vuRight,
                        isShiftHeld = isShiftHeld,
                        onToggleShift = { isShiftHeld = !isShiftHeld },
                        modifier = Modifier
                            .width(136.dp)
                            .fillMaxHeight()
                    )

                    // Right Main Section: Horizontal Touch Strip + Move 32-Pad Silicone Matrix
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Move Horizontal Touch Strip (Pitch Bend & Modulation Ribbon) + Visualizer Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AbletonMoveTouchStrip(
                                position = touchStripPosition,
                                onPositionChange = { pos ->
                                    touchStripPosition = pos
                                    // Modulate filter cutoff in real-time
                                    viewModel.setMasterFilterCutoff(200f + pos * 18000f)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(16.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (showExpandedVisualizer) NeonCyan else Color(0xFF1E212D))
                                    .border(1.dp, NeonCyan, RoundedCornerShape(3.dp))
                                    .clickable { showExpandedVisualizer = !showExpandedVisualizer }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .testTag("toggle_expanded_visualizer_button")
                            ) {
                                Text(
                                    text = if (showExpandedVisualizer) "SPECTRUM OFF" else "SPECTRUM ON",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (showExpandedVisualizer) Color.Black else NeonCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // High-Resolution Expanded Oboe Audio Visualizer Canvas
                        AnimatedVisibility(visible = showExpandedVisualizer) {
                            OboeAudioVisualizerCanvas(
                                viewModel = viewModel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                            )
                        }

                        // Move 32-Pad Performance Matrix
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when (padLayoutMode) {
                                MovePadMode.MOVE_HYBRID -> {
                                    // Official Ableton Move Layout: Top 16 Step Sequencer + Bottom 16 Pads
                                    MoveHybrid32PadMatrix(
                                        viewModel = viewModel,
                                        currentTrack = currentPattern.tracks.getOrNull(activeTrackIndex) ?: currentPattern.tracks.first(),
                                        currentStep = if (isPlaying) currentStep else -1,
                                        pads = currentPads,
                                        activeTriggers = activePadTriggers,
                                        bankColor = when (activeTrackIndex) {
                                            0 -> BankDrumsColor
                                            1 -> BankBassColor
                                            2 -> BankSynthColor
                                            else -> BankSpliceColor
                                        }
                                    )
                                }
                                MovePadMode.FULL_DRUM_16 -> {
                                    // Big 4x4 Expressive Pad Grid Mode
                                    ExpressivePadGrid(
                                        viewModel = viewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                MovePadMode.EXTENDED_32_SEQ -> {
                                    // Full 32-Step running chase sequencer
                                    MoveExtended32Sequencer(
                                        viewModel = viewModel,
                                        track = currentPattern.tracks.getOrNull(activeTrackIndex) ?: currentPattern.tracks.first(),
                                        currentStep = if (isPlaying) currentStep else -1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Transport & Utility Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Roll / Repeat Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("ROLL:", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        RollRate.values().forEach { r ->
                            val isSel = rollRate == r
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSel) NeonOrange else Color(0xFF1E212D))
                                    .clickable { viewModel.setRollRate(r) }
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = r.label,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.Black else Color.LightGray
                                )
                            }
                        }
                    }

                    // Pattern Switcher (A, B, C, D)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("PATTERN:", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        patterns.forEachIndexed { i, p ->
                            val isSel = activePatIdx == i
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSel) NeonLime else Color(0xFF1E212D))
                                    .clickable { viewModel.setActivePattern(i) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = p.name.takeLast(1),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSel) Color.Black else Color.LightGray
                                )
                            }
                        }
                    }

                    // Glitch & Tape Stop Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (viewModel.isTapeStop.collectAsState().value) NeonRed else Color(0xFF1E212D))
                                .border(1.dp, NeonRed.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            viewModel.setTapeStop(true)
                                            tryAwaitRelease()
                                            viewModel.setTapeStop(false)
                                        }
                                    )
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("TAPE STOP", fontSize = 8.sp, fontWeight = FontWeight.Black, color = if (viewModel.isTapeStop.collectAsState().value) Color.Black else Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showWavExportModal) {
        WavExportModal(
            viewModel = viewModel,
            onDismiss = { showWavExportModal = false }
        )
    }

    if (viewModel.showPadFxOverlay.collectAsState().value) {
        PadEffectsRackOverlay(
            viewModel = viewModel,
            onDismiss = { viewModel.closePadFxOverlay() }
        )
    }
}

// Left Function Cluster: OLED Display, Big Push Wheel, Function Buttons
@Composable
fun AbletonMoveLeftCluster(
    viewModel: SchwungViewModel,
    isPlaying: Boolean,
    isRecording: Boolean,
    bpm: Int,
    swingPercent: Int,
    currentStep: Int,
    activeTrackIndex: Int,
    vuLeft: Float,
    vuRight: Float,
    isShiftHeld: Boolean,
    onToggleShift: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF101218))
            .border(1.dp, Color(0xFF262A3B), RoundedCornerShape(8.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // High-Contrast White OLED Display (Ableton Move style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .border(1.dp, Color(0xFF33384F), RoundedCornerShape(4.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "T${activeTrackIndex + 1} " + when (activeTrackIndex) {
                            0 -> "DRUM"
                            1 -> "BASS"
                            2 -> "KEYS"
                            else -> "SPLC"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$bpm BPM",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        VisualMetronomeIndicator(viewModel = viewModel, showLedsOnly = true)
                    }
                }

                Text(
                    text = "STEP ${currentStep + 1}/16 [${swingPercent}%]",
                    fontSize = 8.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace
                )

                // Real-time Oboe Audio Visualizer Canvas (Waveform & Spectrum Analyzer)
                OboeAudioVisualizerCanvas(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )

                // Mini Stereo VU Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { vuLeft.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp),
                        color = NeonLime,
                        trackColor = Color(0xFF1E293B)
                    )
                    LinearProgressIndicator(
                        progress = { vuRight.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp),
                        color = NeonLime,
                        trackColor = Color(0xFF1E293B)
                    )
                }
            }
        }

        // Ableton Move Big Jog Wheel / Push Encoder
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E212D))
                .border(2.dp, Color(0xFF3B4055), CircleShape)
                .clickable {
                    // Click encoder cycles through presets
                    viewModel.toggleMetronome()
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(Color(0xFF282C3D), radius = r - 4f, center = c)
                drawCircle(Color(0xFF0F1014), radius = r * 0.45f, center = c)
                // Detent tick mark
                drawCircle(NeonOrange, radius = 3f, center = Offset(c.x, c.y - (r - 8f)))
            }
            Text("PUSH", fontSize = 6.sp, fontWeight = FontWeight.Black, color = Color.Gray)
        }

        // Move Physical Function Buttons: Play, Record, Loop, Shift, Back, Undo
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // PLAY (Green Triangle)
                MoveHardwareButton(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    label = "",
                    isActive = isPlaying,
                    activeColor = NeonLime,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.togglePlayPause() }
                )

                // RECORD (Red Circle)
                MoveHardwareButton(
                    icon = Icons.Default.FiberManualRecord,
                    label = "",
                    isActive = isRecording,
                    activeColor = NeonRed,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.toggleRecording() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // LOOP Button
                MoveHardwareButton(
                    icon = Icons.Default.Repeat,
                    label = "",
                    isActive = true,
                    activeColor = NeonOrange,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setAppMode(AppMode.STEP_SEQUENCER) }
                )

                // SHIFT Button
                MoveHardwareButton(
                    icon = null,
                    label = "SHIFT",
                    isActive = isShiftHeld,
                    activeColor = NeonCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleShift
                )
            }
        }
    }
}

@Composable
fun MoveHardwareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) activeColor else Color(0xFF1E212D)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) Color.White else Color(0xFF2F3447)
        ),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.height(26.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(13.dp)
            )
        } else {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = if (isActive) Color.Black else Color.White
            )
        }
    }
}

// Ableton Move 9 Endless Rotary Encoders
@Composable
fun MoveRotaryEncoder(
    name: String,
    displayValue: String,
    valueRatio: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(54.dp)
            .clickable { onClick() }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Drag up/down modulates value
                    val delta = -dragAmount.y * 0.01f
                    onValueChange((valueRatio + delta).coerceIn(0f, 1f))
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name,
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF94A3B8),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Knob with circular LED ring
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF161821))
                .border(1.dp, Color(0xFF2F3447), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val c = Offset(size.width / 2f, size.height / 2f)

                // Background track ring
                drawCircle(Color(0xFF222634), radius = r - 3f, center = c, style = Stroke(width = 3f))

                // Active LED arc
                val sweep = valueRatio.coerceIn(0f, 1f) * 270f
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(3f, 3f),
                    size = Size(size.width - 6f, size.height - 6f),
                    style = Stroke(width = 3.5f)
                )

                // Center Cap
                drawCircle(Color(0xFF0F1014), radius = r * 0.55f, center = c)
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = displayValue,
            fontSize = 7.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// Ableton Move Horizontal Touch Strip (Pitch Bend & Modulation Ribbon)
@Composable
fun AbletonMoveTouchStrip(
    position: Float,
    onPositionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0F1014))
            .border(1.dp, Color(0xFF262A3B), RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onPositionChange((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onPositionChange((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Center marker line
            drawLine(Color(0xFF334155), Offset(w / 2f, 0f), Offset(w / 2f, h), strokeWidth = 1f)

            // Touch Cursor Position Indicator
            val cx = position.coerceIn(0f, 1f) * w
            drawCircle(NeonOrange.copy(alpha = 0.4f), radius = 8f, center = Offset(cx, h / 2f))
            drawCircle(NeonOrange, radius = 4f, center = Offset(cx, h / 2f))
        }

        Text(
            text = "TOUCH STRIP // PITCH BEND & FILTER MOD",
            fontSize = 6.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

// Official Ableton Move Hybrid 32-Pad Silicone Matrix:
// Top 16 pads: Step Sequencer (Bar 1 & 2)
// Bottom 16 pads: Velocity-sensitive Playable Pads
@Composable
fun MoveHybrid32PadMatrix(
    viewModel: SchwungViewModel,
    currentTrack: com.example.model.TrackData,
    currentStep: Int,
    pads: List<PadData>,
    activeTriggers: Set<Int>,
    bankColor: Color,
    modifier: Modifier = Modifier
) {
    val masterWaveform by viewModel.audioEngine.masterWaveform.collectAsState()
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // TOP 16 PADS: Step Sequencer (2 Rows of 8 = 16 Steps)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (row in 0..1) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (col in 0..7) {
                        val stepIndex = row * 8 + col
                        val step = currentTrack.steps.getOrNull(stepIndex)
                        val isChase = currentStep == stepIndex
                        val isStepActive = step?.active == true

                        val stepColor by animateColorAsState(
                            targetValue = when {
                                isChase && isStepActive -> Color.White
                                isStepActive -> bankColor
                                isChase -> NeonLime.copy(alpha = 0.5f)
                                stepIndex % 4 == 0 -> Color(0xFF262A3B)
                                else -> Color(0xFF181A24)
                            },
                            animationSpec = tween(durationMillis = 30),
                            label = "step_led"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(stepColor)
                                .border(
                                    1.dp,
                                    if (isChase) NeonLime else if (isStepActive) bankColor else Color(0xFF2A2E40),
                                    RoundedCornerShape(3.dp)
                                )
                                .clickable {
                                    viewModel.toggleStep(currentTrack.trackId, stepIndex)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${stepIndex + 1}",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isStepActive || isChase) Color.Black else Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // BOTTOM 16 PADS: Velocity-sensitive Playable Drum & Melodic Pads (2 Rows of 8 = 16 Pads)
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (row in 0..1) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (col in 0..7) {
                        val padIndex = row * 8 + col
                        val pad = pads.getOrNull(padIndex)

                        if (pad != null) {
                            val isTriggered = activeTriggers.contains(pad.id)
                            val padColor = Color(pad.colorHex)

                            var touchVelocity by remember { mutableFloatStateOf(0.8f) }

                            // PHYSICS SPRING COMPRESSION ANIMATION
                            val targetScale = if (isTriggered) (1.0f - touchVelocity * 0.15f).coerceIn(0.83f, 0.95f) else 1.0f
                            val scale by animateFloatAsState(
                                targetValue = targetScale,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "silicone_pad_scale"
                            )

                            val padBg by animateColorAsState(
                                targetValue = if (isTriggered) Color.White.copy(alpha = 0.85f + touchVelocity * 0.15f) else Color(0xFF1E212D),
                                animationSpec = tween(durationMillis = if (isTriggered) 10 else 90),
                                label = "pad_bg"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(padBg)
                                    .border(
                                        if (isTriggered) (2.dp + (1.5.dp * touchVelocity)) else 1.dp,
                                        if (isTriggered) Color.White else Color(0xFF33384F),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .pointerInput(pad.id) {
                                        detectTapGestures(
                                            onPress = { offset ->
                                                val relativeY = (offset.y / size.height).coerceIn(0f, 1f)
                                                val velocity = (0.45f + relativeY * 0.55f).coerceIn(0.2f, 1.0f)
                                                touchVelocity = velocity
                                                viewModel.onPadDown(pad, velocity)
                                                tryAwaitRelease()
                                                viewModel.onPadUp(pad)
                                            }
                                        )
                                    }
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = pad.name.take(6),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTriggered) Color.Black else Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }

                                // Real-Time Waveform Canvas Overlay Displaying Effects Rack DSP
                                PadFxWaveformCanvasOverlay(
                                    pad = pad,
                                    isTriggered = isTriggered,
                                    masterWaveform = masterWaveform,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(10.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// 32-Step running chase sequencer mode
@Composable
fun MoveExtended32Sequencer(
    viewModel: SchwungViewModel,
    track: com.example.model.TrackData,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (row in 0..3) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (col in 0..7) {
                    val stepIdx = (row * 8 + col) % 16
                    val step = track.steps.getOrNull(stepIdx)
                    val isChase = currentStep == stepIdx
                    val isStepActive = step?.active == true

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isChase && isStepActive) Color.White
                                else if (isStepActive) NeonCyan
                                else if (isChase) NeonLime.copy(alpha = 0.5f)
                                else Color(0xFF1E212D)
                            )
                            .border(1.dp, if (isChase) NeonLime else Color(0xFF2C3040), RoundedCornerShape(3.dp))
                            .clickable { viewModel.toggleStep(track.trackId, stepIdx) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${stepIdx + 1}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isStepActive || isChase) Color.Black else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// Decorative Speaker Grille Dot Pattern
@Composable
fun SpeakerGrillePattern(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val rows = 3
        val cols = 7
        val dx = size.width / cols
        val dy = size.height / rows
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                drawCircle(
                    color = Color(0xFF262A3B),
                    radius = 1.2f,
                    center = Offset((c + 0.5f) * dx, (r + 0.5f) * dy)
                )
            }
        }
    }
}
