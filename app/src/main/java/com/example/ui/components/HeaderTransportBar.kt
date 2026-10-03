package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel

@Composable
fun HeaderTransportBar(
    viewModel: SchwungViewModel,
    modifier: Modifier = Modifier,
    onOpenProjects: () -> Unit
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val bpm by viewModel.bpm.collectAsState()
    val swingPercent by viewModel.swingPercent.collectAsState()
    val metronomeEnabled by viewModel.metronomeEnabled.collectAsState()
    val vuLeft by viewModel.audioEngine.vuMeterLeft.collectAsState()
    val vuRight by viewModel.audioEngine.vuMeterRight.collectAsState()

    var showBpmDialog by remember { mutableStateOf(false) }
    var showSwingDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ChassisDark)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand & Version Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) NeonLime else Color(0xFF475569))
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SCHWUNG",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = " LIVE",
                            color = NeonOrange,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "v1.7 // PadGrid + SpliceMCP",
                            color = OledScreenSecondary,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (viewModel.audioEngine.isOboeActive) NeonCyan.copy(alpha = 0.2f) else ChassisSurface)
                                .border(1.dp, if (viewModel.audioEngine.isOboeActive) NeonCyan else Color.DarkGray, RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (viewModel.audioEngine.isOboeActive) "OBOE C++" else "OBOE READY",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.audioEngine.isOboeActive) NeonCyan else Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Stereo VU Meter Bars
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .border(1.dp, ChassisBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "VU",
                    color = OledScreenSecondary,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                VuMeterBar(level = vuLeft)
                VuMeterBar(level = vuRight)
            }

            // Project / Export Menu Button
            IconButton(
                onClick = onOpenProjects,
                modifier = Modifier
                    .testTag("projects_menu_button")
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ChassisSurface)
                    .border(1.dp, ChassisBorder, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Projects",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Transport & Groove Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // PLAY / PAUSE Button
            val playBg by animateColorAsState(
                targetValue = if (isPlaying) NeonLime else ChassisSurface,
                label = "play_bg"
            )
            val playContentColor = if (isPlaying) Color.Black else NeonLime

            Button(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .testTag("play_pause_button")
                    .weight(1.1f)
                    .height(42.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = playBg),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = playContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isPlaying) "STOP" else "PLAY",
                        color = playContentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // RECORD Button
            val recBg = if (isRecording) NeonRed else ChassisSurface
            val recColor = if (isRecording) Color.White else NeonRed

            Button(
                onClick = { viewModel.toggleRecording() },
                modifier = Modifier
                    .testTag("record_button")
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = recBg),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(recColor)
                    )
                    Text(
                        text = "REC",
                        color = recColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // BPM Display & Tap Control + Visual Metronome
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OledScreenBg)
                    .border(1.dp, ChassisBorder, RoundedCornerShape(8.dp))
                    .clickable { showBpmDialog = true }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$bpm",
                            color = OledScreenText,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "BPM",
                            color = OledScreenSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Compact 4-Beat Metronome Visual Indicator
                    VisualMetronomeIndicator(viewModel = viewModel, showLedsOnly = true)
                }
            }

            // Schwung Groove / Swing Dial Control
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OledScreenBg)
                    .border(1.dp, NeonOrange.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { showSwingDialog = true }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SCHWUNG",
                        color = NeonOrange,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "$swingPercent% SWING",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Metronome Button
            IconButton(
                onClick = { viewModel.toggleMetronome() },
                modifier = Modifier
                    .testTag("metronome_button")
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (metronomeEnabled) NeonCyan.copy(alpha = 0.2f) else ChassisSurface)
                    .border(
                        1.dp,
                        if (metronomeEnabled) NeonCyan else ChassisBorder,
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Metronome",
                    tint = if (metronomeEnabled) NeonCyan else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // BPM Dialog
    if (showBpmDialog) {
        AlertDialog(
            onDismissRequest = { showBpmDialog = false },
            title = { Text("Tempo (BPM)", color = Color.White) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$bpm BPM",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = bpm.toFloat(),
                        onValueChange = { viewModel.setBpm(it.toInt()) },
                        valueRange = 60f..180f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(85, 92, 118, 128, 140).forEach { presetBpm ->
                            OutlinedButton(
                                onClick = { viewModel.setBpm(presetBpm) },
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text("$presetBpm", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBpmDialog = false }) {
                    Text("OK", color = NeonCyan)
                }
            },
            containerColor = ChassisDark
        )
    }

    // Schwung Groove / Swing Dialog
    if (showSwingDialog) {
        AlertDialog(
            onDismissRequest = { showSwingDialog = false },
            title = {
                Column {
                    Text("Schwung Groove Amount", color = Color.White)
                    Text(
                        "Microtiming shuffle applied to 16th notes",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$swingPercent%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonOrange,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = swingPercent.toFloat(),
                        onValueChange = { viewModel.setSwingPercent(it.toInt()) },
                        valueRange = 0f..75f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonOrange,
                            activeTrackColor = NeonOrange
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            0 to "Straight",
                            30 to "Light",
                            54 to "MPC 90s",
                            66 to "Triplet"
                        ).forEach { (amt, label) ->
                            OutlinedButton(
                                onClick = { viewModel.setSwingPercent(amt) },
                                modifier = Modifier.padding(1.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$amt%", fontSize = 11.sp, color = NeonOrange)
                                    Text(label, fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSwingDialog = false }) {
                    Text("DONE", color = NeonOrange)
                }
            },
            containerColor = ChassisDark
        )
    }
}

@Composable
fun VuMeterBar(level: Float, modifier: Modifier = Modifier) {
    val clamped = level.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .width(6.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.BottomCenter
    ) {
        val barBrush = Brush.verticalGradient(
            colors = listOf(NeonRed, NeonYellow, NeonLime)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(clamped)
                .background(barBrush)
        )
    }
}
