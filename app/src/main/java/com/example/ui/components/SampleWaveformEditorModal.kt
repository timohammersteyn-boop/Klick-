package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.PadData
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel

@Composable
fun SampleWaveformEditorModal(
    pad: PadData,
    viewModel: SchwungViewModel,
    onDismiss: () -> Unit
) {
    val peaks = remember(pad.soundRecipe) { viewModel.getPadWaveformPeaks(pad) }
    val padColor = Color(pad.colorHex)

    var isAuditioning by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "audition_sweep")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = pad.sampleStartRatio,
        targetValue = pad.sampleEndRatio,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing)
        ),
        label = "sweep"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, padColor, RoundedCornerShape(12.dp))
                .testTag("sample_waveform_editor_dialog"),
            colors = CardDefaults.cardColors(containerColor = ChassisDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Modal Header Bar
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
                                .clip(RoundedCornerShape(4.dp))
                                .background(padColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PAD ${pad.id + 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                        Text(
                            text = "SAMPLE WAVEFORM EDITOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Text(
                    text = "SAMPLE: '${pad.name}' | RECIPE: ${pad.soundRecipe.uppercase()}",
                    fontSize = 8.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // INTERACTIVE WAVEFORM CANVAS WITH DRAGGABLE START & END HANDLES
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF262A3B), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = OledScreenBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pad.id) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val touchRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                                    val distToStart = kotlin.math.abs(touchRatio - pad.sampleStartRatio)
                                    val distToEnd = kotlin.math.abs(touchRatio - pad.sampleEndRatio)

                                    if (distToStart < distToEnd) {
                                        viewModel.updatePadSampleEditor(pad.id, pad.bank, sampleStartRatio = touchRatio)
                                    } else {
                                        viewModel.updatePadSampleEditor(pad.id, pad.bank, sampleEndRatio = touchRatio)
                                    }
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val midY = h / 2f
                            val count = peaks.size.coerceAtLeast(20)
                            val barW = w / count

                            val startX = pad.sampleStartRatio * w
                            val endX = pad.sampleEndRatio * w
                            val loopStartX = pad.loopStartRatio * w
                            val loopEndX = pad.loopEndRatio * w

                            // 1. Dimmed Out-of-Bounds Region (Before Start & After End)
                            drawRect(
                                color = Color.Black.copy(alpha = 0.65f),
                                topLeft = Offset(0f, 0f),
                                size = Size(startX, h)
                            )
                            drawRect(
                                color = Color.Black.copy(alpha = 0.65f),
                                topLeft = Offset(endX, 0f),
                                size = Size(w - endX, h)
                            )

                            // 2. Loop Region Shading (if loop enabled)
                            if (pad.isLoopEnabled) {
                                drawRect(
                                    color = NeonCyan.copy(alpha = 0.15f),
                                    topLeft = Offset(loopStartX, 0f),
                                    size = Size(loopEndX - loopStartX, h)
                                )
                            }

                            // 3. Waveform Bars
                            for (i in 0 until count) {
                                val x = i * barW
                                val p = peaks.getOrElse(i) { 0.3f }
                                val barH = (p * (h * 0.8f)).coerceAtLeast(2f)

                                val isInActiveTrim = x >= startX && x <= endX
                                val barColor = if (isInActiveTrim) padColor else Color.Gray.copy(alpha = 0.3f)

                                drawRect(
                                    color = barColor,
                                    topLeft = Offset(x, midY - barH / 2f),
                                    size = Size((barW - 1f).coerceAtLeast(1f), barH)
                                )
                            }

                            // 4. Start Point Marker (Green Line + Handle Tag)
                            drawLine(
                                color = NeonLime,
                                start = Offset(startX, 0f),
                                end = Offset(startX, h),
                                strokeWidth = 3f
                            )
                            drawCircle(
                                color = NeonLime,
                                radius = 8f,
                                center = Offset(startX, 12f)
                            )

                            // 5. End Point Marker (Red Line + Handle Tag)
                            drawLine(
                                color = NeonRed,
                                start = Offset(endX, 0f),
                                end = Offset(endX, h),
                                strokeWidth = 3f
                            )
                            drawCircle(
                                color = NeonRed,
                                radius = 8f,
                                center = Offset(endX, h - 12f)
                            )

                            // 6. Loop Start & End Lines (if enabled)
                            if (pad.isLoopEnabled) {
                                drawLine(
                                    color = NeonCyan,
                                    start = Offset(loopStartX, 0f),
                                    end = Offset(loopStartX, h),
                                    strokeWidth = 2f
                                )
                                drawLine(
                                    color = NeonYellow,
                                    start = Offset(loopEndX, 0f),
                                    end = Offset(loopEndX, h),
                                    strokeWidth = 2f
                                )
                            }

                            // 7. Audition Playhead Sweep
                            if (isAuditioning) {
                                val sweepX = sweepProgress * w
                                drawLine(
                                    color = Color.White,
                                    start = Offset(sweepX, 0f),
                                    end = Offset(sweepX, h),
                                    strokeWidth = 2f
                                )
                            }
                        }

                        // Labels on Canvas
                        Text(
                            text = "START: ${(pad.sampleStartRatio * 100).toInt()}%",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonLime,
                            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                        )
                        Text(
                            text = "END: ${(pad.sampleEndRatio * 100).toInt()}%",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonRed,
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // TRIM CONTROL SLIDERS
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // SAMPLE START POINT SLIDER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SAMPLE START", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonLime)
                        Text("${String.format("%.1f", pad.sampleStartRatio * 100)}%", fontSize = 9.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = pad.sampleStartRatio,
                        onValueChange = { ratio ->
                            viewModel.updatePadSampleEditor(pad.id, pad.bank, sampleStartRatio = ratio)
                        },
                        valueRange = 0f..0.95f,
                        colors = SliderDefaults.colors(thumbColor = NeonLime, activeTrackColor = NeonLime)
                    )

                    // SAMPLE END POINT SLIDER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SAMPLE END", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonRed)
                        Text("${String.format("%.1f", pad.sampleEndRatio * 100)}%", fontSize = 9.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = pad.sampleEndRatio,
                        onValueChange = { ratio ->
                            viewModel.updatePadSampleEditor(pad.id, pad.bank, sampleEndRatio = ratio)
                        },
                        valueRange = 0.05f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = NeonRed, activeTrackColor = NeonRed)
                    )

                    // LOOP TOGGLE & LOOP RANGE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Repeat, contentDescription = "Loop", tint = if (pad.isLoopEnabled) NeonCyan else Color.Gray, modifier = Modifier.size(16.dp))
                            Text("SAMPLE LOOP MODE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (pad.isLoopEnabled) NeonCyan else Color.Gray)
                        }
                        Switch(
                            checked = pad.isLoopEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updatePadSampleEditor(pad.id, pad.bank, isLoopEnabled = enabled)
                            },
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // QUICK PRESETS & AUDITION ACTION BAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Reset Button
                    Button(
                        onClick = {
                            viewModel.updatePadSampleEditor(
                                pad.id, pad.bank,
                                sampleStartRatio = 0f,
                                sampleEndRatio = 1.0f,
                                loopStartRatio = 0f,
                                loopEndRatio = 1.0f,
                                isLoopEnabled = false
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChassisDark),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESET", fontSize = 8.sp, color = Color.LightGray)
                    }

                    // Audition Play Button
                    Button(
                        onClick = {
                            isAuditioning = !isAuditioning
                            viewModel.onPadDown(pad, 0.9f)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isAuditioning) NeonLime else padColor),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1.5f).height(36.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Audition", tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAuditioning) "STOP PREVIEW" else "AUDITION SAMPLE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}
