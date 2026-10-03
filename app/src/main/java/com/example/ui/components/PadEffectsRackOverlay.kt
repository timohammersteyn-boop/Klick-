package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.PadBank
import com.example.model.PadData
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun PadEffectsRackOverlay(
    viewModel: SchwungViewModel,
    onDismiss: () -> Unit
) {
    val selectedPad by viewModel.selectedPad.collectAsState()
    val currentBank by viewModel.currentBank.collectAsState()

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

    val targetPad = selectedPad ?: currentPads.firstOrNull() ?: return
    val padColor = Color(targetPad.colorHex)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, padColor, RoundedCornerShape(12.dp))
                .testTag("pad_effects_rack_overlay_dialog"),
            colors = CardDefaults.cardColors(containerColor = ChassisDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // HEADER: Selected Pad Title, Bank Badge, Audition Button & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(padColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${targetPad.bank.label} P${targetPad.id + 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }

                        Text(
                            text = "PAD FX RACK: ${targetPad.name.uppercase()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Quick Audition Button
                        Button(
                            onClick = {
                                viewModel.audioEngine.triggerSound(
                                    recipe = targetPad.soundRecipe,
                                    velocity = targetPad.volume,
                                    pitchSemitones = targetPad.pitch,
                                    cutoff = if (targetPad.isFxChainBypassed) 20000f else targetPad.filterCutoff,
                                    decayMultiplier = targetPad.decay
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonLime),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Audition", tint = Color.Black, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("TEST PAD", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // PAD SELECTOR CAROUSEL (Switch active pad inside FX overlay)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("PAD:", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        items(currentPads) { p ->
                            val isSelected = p.id == targetPad.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSelected) padColor else ChassisSurface)
                                    .border(1.dp, if (isSelected) padColor else ChassisBorder, RoundedCornerShape(3.dp))
                                    .clickable { viewModel.selectPad(p) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "P${p.id + 1}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.LightGray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // MODULE 1: LOW-PASS FILTER DSP & INTERACTIVE FREQUENCY CURVE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ChassisBlack),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(14.dp))
                                Text("RESONANT LOW-PASS FILTER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonPurple)
                            }

                            // Bypass Toggle
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (targetPad.isFxChainBypassed) "FX BYPASSED" else "FX ACTIVE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetPad.isFxChainBypassed) NeonRed else NeonLime
                                )
                                Switch(
                                    checked = !targetPad.isFxChainBypassed,
                                    onCheckedChange = { active ->
                                        viewModel.updatePad(targetPad.copy(isFxChainBypassed = !active))
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = NeonLime, checkedTrackColor = NeonLime.copy(alpha = 0.3f)),
                                    modifier = Modifier.height(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Interactive Filter Response Curve Canvas
                        FilterResponseCurveCanvas(
                            cutoffHz = targetPad.filterCutoff,
                            resonanceQ = targetPad.filterResonance,
                            isBypassed = targetPad.isFxChainBypassed,
                            accentColor = padColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Cutoff Frequency Slider (100 Hz to 20,000 Hz)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("LOW-PASS CUTOFF FREQUENCY", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("${targetPad.filterCutoff.toInt()} Hz", fontSize = 9.sp, color = NeonPurple, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            }
                            Slider(
                                value = targetPad.filterCutoff,
                                onValueChange = { cutoff ->
                                    viewModel.updatePad(targetPad.copy(filterCutoff = cutoff))
                                },
                                valueRange = 100f..20000f,
                                colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple),
                                modifier = Modifier.height(24.dp)
                            )
                        }

                        // Filter Resonance (Q Factor: 0.5 to 4.5)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RESONANCE (Q FACTOR)", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(String.format("%.1f Q", targetPad.filterResonance), fontSize = 9.sp, color = NeonCyan, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            }
                            Slider(
                                value = targetPad.filterResonance,
                                onValueChange = { q ->
                                    viewModel.updatePad(targetPad.copy(filterResonance = q))
                                },
                                valueRange = 0.5f..4.5f,
                                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // MODULE 2: SAMPLE GAIN, PAN & MUTE/SOLO
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ChassisBlack),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(14.dp))
                            Text("SAMPLE GAIN & STEREO PAN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonAmber)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Volume / Gain Slider (0.0 to 1.5 = +6dB)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("SAMPLE GAIN / VOLUME", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                val gainDb = (20 * log10(targetPad.volume.coerceAtLeast(0.01f))).toInt()
                                Text("${(targetPad.volume * 100).toInt()}% (${if (gainDb >= 0) "+" else ""}${gainDb} dB)", fontSize = 9.sp, color = NeonAmber, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            }
                            Slider(
                                value = targetPad.volume,
                                onValueChange = { vol ->
                                    viewModel.updatePad(targetPad.copy(volume = vol))
                                },
                                valueRange = 0.0f..1.5f,
                                colors = SliderDefaults.colors(thumbColor = NeonAmber, activeTrackColor = NeonAmber),
                                modifier = Modifier.height(24.dp)
                            )
                        }

                        // Stereo Pan Slider (-1.0 to +1.0)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("STEREO PAN", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                val panLabel = when {
                                    targetPad.pan < -0.05f -> "L ${(targetPad.pan * -100).toInt()}%"
                                    targetPad.pan > 0.05f -> "R ${(targetPad.pan * 100).toInt()}%"
                                    else -> "CENTER"
                                }
                                Text(panLabel, fontSize = 9.sp, color = NeonOrange, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            }
                            Slider(
                                value = targetPad.pan,
                                onValueChange = { p ->
                                    viewModel.updatePad(targetPad.copy(pan = p))
                                },
                                valueRange = -1.0f..1.0f,
                                colors = SliderDefaults.colors(thumbColor = NeonOrange, activeTrackColor = NeonOrange),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // MODULE 3: PITCH, DECAY & REVERB/DELAY SENDS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ChassisBlack),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCoral.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCoral, modifier = Modifier.size(14.dp))
                            Text("TONE, ENVELOPE & SPATIAL SENDS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonCoral)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pitch Shift (-12 to +12 semitones)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("PITCH", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text("${if (targetPad.pitch > 0) "+" else ""}${targetPad.pitch.toInt()} st", fontSize = 8.sp, color = NeonCoral, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                }
                                Slider(
                                    value = targetPad.pitch,
                                    onValueChange = { pitch ->
                                        viewModel.updatePad(targetPad.copy(pitch = pitch))
                                    },
                                    valueRange = -12f..12f,
                                    colors = SliderDefaults.colors(thumbColor = NeonCoral, activeTrackColor = NeonCoral),
                                    modifier = Modifier.height(22.dp)
                                )
                            }

                            // Decay Time (0.05s to 2.5s)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("DECAY", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text(String.format("%.2fs", targetPad.decay), fontSize = 8.sp, color = NeonLime, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                }
                                Slider(
                                    value = targetPad.decay,
                                    onValueChange = { d ->
                                        viewModel.updatePad(targetPad.copy(decay = d))
                                    },
                                    valueRange = 0.05f..2.5f,
                                    colors = SliderDefaults.colors(thumbColor = NeonLime, activeTrackColor = NeonLime),
                                    modifier = Modifier.height(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterResponseCurveCanvas(
    cutoffHz: Float,
    resonanceQ: Float,
    isBypassed: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF1E212D), RoundedCornerShape(4.dp))
    ) {
        val w = size.width
        val h = size.height
        val midY = h * 0.7f

        // Grid lines (100Hz, 1kHz, 10kHz)
        val f100X = log10(100f / 20f) / log10(20000f / 20f) * w
        val f1kX = log10(1000f / 20f) / log10(20000f / 20f) * w
        val f10kX = log10(10000f / 20f) / log10(20000f / 20f) * w

        drawLine(Color.Gray.copy(alpha = 0.2f), Offset(f100X, 0f), Offset(f100X, h), strokeWidth = 1f)
        drawLine(Color.Gray.copy(alpha = 0.2f), Offset(f1kX, 0f), Offset(f1kX, h), strokeWidth = 1f)
        drawLine(Color.Gray.copy(alpha = 0.2f), Offset(f10kX, 0f), Offset(f10kX, h), strokeWidth = 1f)

        if (isBypassed) {
            // Flat pass line
            drawLine(
                color = Color.Gray,
                start = Offset(0f, h * 0.3f),
                end = Offset(w, h * 0.3f),
                strokeWidth = 2f
            )
            return@Canvas
        }

        val cutoffNormX = log10((cutoffHz.coerceIn(20f, 20000f)) / 20f) / log10(20000f / 20f) * w
        val path = Path()

        path.moveTo(0f, h * 0.3f)

        val numPoints = 80
        for (i in 0..numPoints) {
            val x = (i.toFloat() / numPoints) * w
            val freq = 20f * (20000f / 20f).pow(i.toFloat() / numPoints)

            // Low pass response calculation with Q peak
            val ratio = (freq / cutoffHz.coerceAtLeast(20f)).toDouble()
            val term1 = 1.0 - ratio * ratio
            val term2 = ratio / resonanceQ.toDouble()
            val magnitude = 1.0 / sqrt(term1 * term1 + term2 * term2)
            val db = 20.0 * log10(magnitude.coerceAtLeast(0.01))

            val minY = 4f
            val maxY = (h - 4f).coerceAtLeast(minY)
            val y = (h * 0.3f - (db * 1.5f).toFloat()).coerceIn(minY, maxY)

            path.lineTo(x, y)
        }

        // Draw Filter Curve
        drawPath(
            path = path,
            color = if (resonanceQ > 2.0f) NeonPurple else accentColor,
            style = Stroke(width = 2.5f)
        )

        // Draw Cutoff Marker Circle
        val minY = 4f
        val maxY = (h - 4f).coerceAtLeast(minY)
        val cutoffY = (h * 0.3f - ((resonanceQ - 1.0f) * 6f)).coerceIn(minY, maxY)
        drawCircle(
            color = Color.White,
            radius = 4f,
            center = Offset(cutoffNormX, cutoffY)
        )
    }
}
