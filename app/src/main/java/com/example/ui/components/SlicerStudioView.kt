package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppMode
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel

@Composable
fun SlicerStudioView(
    viewModel: SchwungViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSample by viewModel.selectedSample.collectAsState()
    val sliceMarkers by viewModel.sliceMarkers.collectAsState()
    val pitchSemitones by viewModel.slicerPitchSemitones.collectAsState()
    val isReverse by viewModel.slicerIsReverse.collectAsState()

    var activeAuditionSlice by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChassisBlack)
            .padding(10.dp)
    ) {
        // Slicer Header Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SAMPLE SLICER STUDIO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonCyan,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${selectedSample.title} (${selectedSample.bpm} BPM / ${selectedSample.key})",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = {
                    viewModel.sliceSampleToGrid(selectedSample)
                    viewModel.setAppMode(AppMode.PAD_GRID)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.GridView, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("APPLY TO PADS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Audio Waveform & Chop Boundaries Display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = OledScreenBg),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val clickedSlice = ((offset.x / size.width) * 16).toInt().coerceIn(0, 15)
                            activeAuditionSlice = clickedSlice
                            viewModel.audioEngine.triggerSound(
                                recipe = "splice_slice",
                                velocity = 0.95f,
                                sliceIndex = clickedSlice,
                                chokeGroup = 2,
                                loopKey = selectedSample.soundRecipe
                            )
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2f

                    // Draw center axis
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, centerY),
                        end = Offset(w, centerY),
                        strokeWidth = 1f
                    )

                    // Draw Waveform bars (simulated or real peak profile)
                    val numBars = 64
                    val barWidth = w / numBars
                    for (i in 0 until numBars) {
                        val sliceIdx = (i * 16) / numBars
                        val peak = selectedSample.wavePeaks.getOrNull(sliceIdx % selectedSample.wavePeaks.size) ?: 0.5f
                        val barHeight = (peak * (h * 0.8f)).coerceAtLeast(4f)
                        val x = i * barWidth

                        val isCurrentSliceAudition = activeAuditionSlice == sliceIdx

                        drawRect(
                            color = if (isCurrentSliceAudition) NeonAmber else NeonCyan.copy(alpha = 0.7f),
                            topLeft = Offset(x + 1f, centerY - barHeight / 2f),
                            size = Size(barWidth - 2f, barHeight)
                        )
                    }

                    // Draw 16 Vertical Slice Marker Lines
                    for (slice in 0..16) {
                        val x = slice * (w / 16f)
                        drawLine(
                            color = if (slice % 4 == 0) NeonAmber else Color(0xFF64748B),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = if (slice % 4 == 0) 1.5f else 1f
                        )
                    }
                }

                // Slice hint badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "16 TRANSIENT CHOPS // TAP TO AUDITION",
                        fontSize = 8.sp,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Slice Audition Matrix (16 Mini Slice Buttons)
        Text(
            text = "SLICE AUDITION MATRIX (PAD 1 TO 16)",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(sliceMarkers) { marker ->
                val isAuditioning = activeAuditionSlice == marker.sliceIndex

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isAuditioning) NeonAmber else ChassisDark)
                        .border(
                            1.dp,
                            if (isAuditioning) Color.White else ChassisBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            activeAuditionSlice = marker.sliceIndex
                            viewModel.audioEngine.triggerSound(
                                recipe = "splice_slice",
                                velocity = 0.9f,
                                sliceIndex = marker.sliceIndex,
                                chokeGroup = 2,
                                loopKey = selectedSample.soundRecipe
                            )
                        }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isAuditioning) Color.Black else NeonAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = marker.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAuditioning) Color.Black else Color.White
                        )
                    }
                }
            }
        }
    }
}
