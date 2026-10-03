package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel

@Composable
fun VisualMetronomeIndicator(
    viewModel: SchwungViewModel,
    modifier: Modifier = Modifier,
    showLedsOnly: Boolean = false
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val bpm by viewModel.bpm.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val metronomeEnabled by viewModel.metronomeEnabled.collectAsState()

    // Calculate active quarter note beat (0 = Beat 1, 1 = Beat 2, 2 = Beat 3, 3 = Beat 4)
    val activeBeatIndex = if (isPlaying) (currentStep / 4).coerceIn(0, 3) else 0

    // Pulse animation for the active beat
    val pulseAnim = remember { Animatable(0f) }

    LaunchedEffect(currentStep, isPlaying) {
        if (isPlaying && currentStep % 4 == 0) {
            pulseAnim.snapTo(1f)
            pulseAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = (60_000 / bpm.coerceAtLeast(30)).coerceAtMost(500),
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    if (showLedsOnly) {
        // Compact 4-Beat LED Bar for Headers & Displays
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(4.dp))
                .background(ChassisBlack)
                .border(1.dp, ChassisBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("visual_metronome_leds_compact"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Metronome Toggle Icon
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.toggleMetronome() }
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Toggle Metronome",
                    tint = if (metronomeEnabled) NeonLime else Color.Gray,
                    modifier = Modifier.size(10.dp)
                )
            }

            // 4 Beat LEDs (Beat 1, Beat 2, Beat 3, Beat 4)
            for (beat in 0..3) {
                val isBeatActive = isPlaying && activeBeatIndex == beat
                val isDownbeat = beat == 0

                val ledColor by animateColorAsState(
                    targetValue = when {
                        isBeatActive && isDownbeat -> NeonCoral
                        isBeatActive -> NeonLime
                        metronomeEnabled -> Color(0xFF33384F)
                        else -> Color(0xFF1E212D)
                    },
                    animationSpec = tween(if (isBeatActive) 10 else 120),
                    label = "metronome_led_color"
                )

                val ledScale = if (isBeatActive) (1.0f + pulseAnim.value * 0.3f) else 1.0f

                Box(
                    modifier = Modifier
                        .size((7 * ledScale).dp)
                        .clip(CircleShape)
                        .background(ledColor)
                )
            }
        }
    } else {
        // Full Visual Metronome Card with Pendulum Pulse & Beat LEDs
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, if (isPlaying) NeonLime.copy(alpha = 0.6f) else ChassisBorder, RoundedCornerShape(8.dp))
                .testTag("visual_metronome_full_card"),
            colors = CardDefaults.cardColors(containerColor = ChassisDark),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Metronome Status & BPM
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.toggleMetronome() },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (metronomeEnabled) NeonLime else ChassisSurface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Toggle Audio Metronome",
                            tint = if (metronomeEnabled) Color.Black else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isPlaying) "METRONOME LIVE" else "METRONOME READY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isPlaying) NeonLime else Color.Gray
                        )
                        Text(
                            text = "$bpm BPM [4/4]",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Center: Pulsing Beat Ring Canvas
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val baseRadius = size.width / 2.4f
                        val pulseRadius = baseRadius + (pulseAnim.value * 8.dp.toPx())

                        // Outer Pulse Ripple
                        if (isPlaying && pulseAnim.value > 0.01f) {
                            drawCircle(
                                color = (if (activeBeatIndex == 0) NeonCoral else NeonLime).copy(alpha = pulseAnim.value * 0.7f),
                                radius = pulseRadius,
                                center = center,
                                style = Stroke(width = 2.dp.toPx() * pulseAnim.value)
                            )
                        }

                        // Core Beat Circle
                        drawCircle(
                            color = if (isPlaying) (if (activeBeatIndex == 0) NeonCoral else NeonLime) else Color.DarkGray,
                            radius = baseRadius * (if (isPlaying && pulseAnim.value > 0.5f) 1.2f else 1.0f),
                            center = center
                        )
                    }

                    Text(
                        text = if (isPlaying) "${activeBeatIndex + 1}" else "•",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }

                // Right: 4-Beat Quarter Note LED Meters
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (beat in 0..3) {
                        val isBeatActive = isPlaying && activeBeatIndex == beat
                        val isDownbeat = beat == 0

                        val ledColor by animateColorAsState(
                            targetValue = when {
                                isBeatActive && isDownbeat -> NeonCoral
                                isBeatActive -> NeonLime
                                else -> Color(0xFF1E212D)
                            },
                            animationSpec = tween(if (isBeatActive) 10 else 100),
                            label = "beat_meter_color"
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(ledColor)
                                    .border(1.dp, if (isBeatActive) Color.White else ChassisBorder, RoundedCornerShape(3.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${beat + 1}",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBeatActive) Color.Black else Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
