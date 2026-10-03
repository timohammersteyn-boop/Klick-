package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.OledScreenBg
import com.example.viewmodel.SchwungViewModel

@Composable
fun MasterWaveformOscilloscope(
    viewModel: SchwungViewModel,
    modifier: Modifier = Modifier,
    lineColor: Color = NeonCyan,
    showLabel: Boolean = true
) {
    val waveformSamples by viewModel.audioEngine.masterWaveform.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val vuL by viewModel.audioEngine.vuMeterLeft.collectAsState()
    val vuR by viewModel.audioEngine.vuMeterRight.collectAsState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(OledScreenBg)
            .border(1.dp, Color(0xFF262A3B), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (showLabel) {
                Column(modifier = Modifier.width(70.dp)) {
                    Text(
                        text = "REALTIME OSCO",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        color = lineColor,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "44.1kHz PCM",
                        fontSize = 6.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Oscilloscope Curve Canvas
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                val count = waveformSamples.size

                if (count > 1) {
                    val stepX = w / (count - 1)

                    // Draw Center Baseline Grid
                    drawLine(
                        color = Color(0xFF1E2333),
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1f
                    )

                    // Build Oscilloscope Path
                    val path = Path()
                    for (i in 0 until count) {
                        val x = i * stepX
                        val y = midY - (waveformSamples[i] * (midY * 0.9f))
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    // Outer Glow Line
                    drawPath(
                        path = path,
                        color = lineColor.copy(alpha = 0.35f),
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Core Oscilloscope Line
                    drawPath(
                        path = path,
                        color = if (vuL > 0.9f || vuR > 0.9f) Color.White else lineColor,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
    }
}
