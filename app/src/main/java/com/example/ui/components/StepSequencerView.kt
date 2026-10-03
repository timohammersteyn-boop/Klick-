package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StepData
import com.example.model.TrackData
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel

@Composable
fun StepSequencerView(
    viewModel: SchwungViewModel,
    modifier: Modifier = Modifier
) {
    val patterns by viewModel.patterns.collectAsState()
    val activePatIdx by viewModel.activePatternIndex.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val currentPattern = patterns.getOrNull(activePatIdx) ?: patterns.first()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChassisBlack)
            .padding(8.dp)
    ) {
        // Pattern Selector & Utilities Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pattern A / B / C / D
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                patterns.forEachIndexed { index, pat ->
                    val isSelected = activePatIdx == index
                    Box(
                        modifier = Modifier
                            .testTag("pattern_tab_$index")
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) NeonLime else ChassisSurface)
                            .border(
                                1.dp,
                                if (isSelected) NeonLime else ChassisBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.setActivePattern(index) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = pat.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color.LightGray
                        )
                    }
                }
            }

            // Clear Pattern Button
            Button(
                onClick = { viewModel.clearActivePattern() },
                colors = ButtonDefaults.buttonColors(containerColor = ChassisSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChassisBorder),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear",
                    tint = NeonRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("CLEAR", fontSize = 10.sp, color = NeonRed, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Step chase position indicator header (16 LEDs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 78.dp, end = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (step in 0..15) {
                val isChase = isPlaying && currentStep == step
                val isBeatDownbeat = step % 4 == 0

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isChase) NeonLime
                            else if (isBeatDownbeat) Color(0xFF475569)
                            else Color(0xFF1E293B)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Sequencer Tracks List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(currentPattern.tracks) { track ->
                TrackRowItem(
                    track = track,
                    currentStep = if (isPlaying) currentStep else -1,
                    onToggleStep = { stepIdx -> viewModel.toggleStep(track.trackId, stepIdx) },
                    onToggleMute = { viewModel.toggleTrackMute(track.trackId) }
                )
            }
        }
    }
}

@Composable
fun TrackRowItem(
    track: TrackData,
    currentStep: Int,
    onToggleStep: (Int) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor = Color(track.colorHex)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ChassisDark)
            .border(1.dp, ChassisBorder, RoundedCornerShape(8.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Header (Name, Mute)
        Column(
            modifier = Modifier
                .width(72.dp)
                .padding(end = 6.dp)
        ) {
            Text(
                text = track.name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (track.isMuted) Color.Gray else trackColor,
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "T${track.trackId + 1}",
                    fontSize = 8.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (track.isMuted) NeonRed else ChassisSurface)
                        .clickable { onToggleMute() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (track.isMuted) "M" else "ON",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (track.isMuted) Color.White else Color.Gray
                    )
                }
            }
        }

        // 16 Step Buttons
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            track.steps.forEach { step ->
                val isChase = currentStep == step.stepIndex
                val isBeat = step.stepIndex % 4 == 0

                val stepBg by animateColorAsState(
                    targetValue = when {
                        isChase && step.active -> Color.White
                        step.active -> trackColor
                        isChase -> NeonLime.copy(alpha = 0.3f)
                        isBeat -> ChassisElevated
                        else -> ChassisSurface
                    },
                    label = "step_bg"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(stepBg)
                        .border(
                            1.dp,
                            if (isChase) NeonLime else if (step.active) trackColor else ChassisBorder,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { onToggleStep(step.stepIndex) }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (step.active) {
                        // Velocity indicator line inside active step
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isChase) Color.Black else Color.Black.copy(alpha = 0.5f))
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}
