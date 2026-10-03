package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PadBank
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel

enum class VisualizerMode {
    SPECTRUM,
    WAVEFORM,
    HYBRID
}

@Composable
fun OboeAudioVisualizerCanvas(
    viewModel: SchwungViewModel,
    modifier: Modifier = Modifier
) {
    val masterWaveform by viewModel.audioEngine.masterWaveform.collectAsState()
    val masterSpectrum by viewModel.audioEngine.masterSpectrum.collectAsState()
    val isOboeActive = viewModel.audioEngine.isOboeActive
    val vuLeft by viewModel.audioEngine.vuMeterLeft.collectAsState()
    val vuRight by viewModel.audioEngine.vuMeterRight.collectAsState()
    val activeTriggers by viewModel.activePadTriggers.collectAsState()
    val currentBank by viewModel.currentBank.collectAsState()

    var vizMode by remember { mutableStateOf(VisualizerMode.SPECTRUM) }

    val activeBankColor = when (currentBank) {
        PadBank.DRUMS -> BankDrumsColor
        PadBank.BASS -> BankBassColor
        PadBank.SYNTH -> BankSynthColor
        PadBank.SPLICE -> BankSpliceColor
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, ChassisBorder, RoundedCornerShape(8.dp))
            .testTag("oboe_audio_visualizer_card"),
        colors = CardDefaults.cardColors(containerColor = OledScreenBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            // Header: Visualizer Mode Selector & Oboe Engine Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Toggle Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VisualizerMode.values().forEach { mode ->
                        val isSelected = vizMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isSelected) activeBankColor else ChassisDark)
                                .border(1.dp, if (isSelected) activeBankColor else ChassisBorder, RoundedCornerShape(3.dp))
                                .clickable { vizMode = mode }
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = mode.name,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) Color.Black else Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Oboe Low-Latency Engine Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isOboeActive) NeonLime else NeonCyan)
                    )
                    Text(
                        text = if (isOboeActive) "OBOE NATIVE DSP" else "AUDIO TRACK DSP",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOboeActive) NeonLime else NeonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Audio Visualizer Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF1E212D), RoundedCornerShape(4.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val midY = h / 2f

                    // 1. SPECTRUM ANALYZER VIEW
                    if (vizMode == VisualizerMode.SPECTRUM || vizMode == VisualizerMode.HYBRID) {
                        val numBands = masterSpectrum.size.coerceAtLeast(1)
                        val gap = 3f
                        val barWidth = (w - (numBands + 1) * gap) / numBands

                        for (i in 0 until numBands) {
                            val mag = masterSpectrum.getOrElse(i) { 0f }.coerceIn(0f, 1f)
                            val barH = (mag * (h * 0.9f)).coerceAtLeast(3f)
                            val x = gap + i * (barWidth + gap)
                            val y = h - barH

                            // Logarithmic frequency band colors (Sub Bass -> Bass -> Mid -> Highs)
                            val barColor = when {
                                i in 0..2 -> NeonAmber // Sub-Bass
                                i in 3..6 -> NeonCyan  // Bass & Kick Punch
                                i in 7..11 -> NeonOrange // Mids & Snare/Keys
                                else -> NeonPurple    // Highs & Air
                            }

                            // Glowing Spectrum Bar
                            drawRoundRect(
                                color = barColor.copy(alpha = if (vizMode == VisualizerMode.HYBRID) 0.65f else 0.9f),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(2f, 2f)
                            )

                            // Peak Hold Indicator Line
                            val minY = 2f
                            val maxY = (h - 2f).coerceAtLeast(minY)
                            val peakY = (h - (mag * 1.05f * h)).coerceIn(minY, maxY)
                            drawLine(
                                color = Color.White,
                                start = Offset(x, peakY),
                                end = Offset(x + barWidth, peakY),
                                strokeWidth = 2f
                            )
                        }
                    }

                    // 2. OSCILLOGRAM WAVEFORM VIEW
                    if (vizMode == VisualizerMode.WAVEFORM || vizMode == VisualizerMode.HYBRID) {
                        val wavePoints = masterWaveform
                        if (wavePoints.isNotEmpty()) {
                            val stepX = w / (wavePoints.size - 1).coerceAtLeast(1)
                            val path = Path()

                            for (i in wavePoints.indices) {
                                val s = wavePoints[i]
                                val x = i * stepX
                                val y = midY - (s * (h * 0.45f))

                                if (i == 0) path.moveTo(x, y)
                                else path.lineTo(x, y)
                            }

                            // Draw Waveform Glow Line
                            drawPath(
                                path = path,
                                color = if (vizMode == VisualizerMode.HYBRID) NeonLime else activeBankColor,
                                style = Stroke(width = 2.5f)
                            )

                            // Center Zero dB Line
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.2f),
                                start = Offset(0f, midY),
                                end = Offset(w, midY),
                                strokeWidth = 1f
                            )
                        }
                    }

                    // 3. ACTIVE PAD TRIGGER SPECTRUM FLASH OVERLAY
                    if (activeTriggers.isNotEmpty()) {
                        drawRect(
                            color = activeBankColor.copy(alpha = 0.08f),
                            topLeft = Offset(0f, 0f),
                            size = Size(w, h)
                        )
                    }
                }

                // VU Meter Peak Indicators at Bottom Left
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "L: ${(vuLeft * 100).toInt()}% | R: ${(vuRight * 100).toInt()}%",
                        fontSize = 7.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
