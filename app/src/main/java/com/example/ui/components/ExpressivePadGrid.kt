package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.example.model.PadBank
import com.example.model.PadData
import com.example.model.RollRate
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel

@Composable
fun ExpressivePadGrid(
    viewModel: SchwungViewModel,
    modifier: Modifier = Modifier
) {
    val currentBank by viewModel.currentBank.collectAsState()
    val activeTriggers by viewModel.activePadTriggers.collectAsState()
    val rollRate by viewModel.rollRate.collectAsState()
    val selectedPad by viewModel.selectedPad.collectAsState()
    val lastActivatedPad by viewModel.lastActivatedPad.collectAsState()
    val masterWaveform by viewModel.audioEngine.masterWaveform.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

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

    var showPadTweakPopup by remember { mutableStateOf(false) }
    var showSampleWaveformEditor by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChassisBlack)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Bank Selection Bar (Flat Material Design)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PadBank.values().forEach { bank ->
                val isBankSelected = currentBank == bank
                val bankColor = when (bank) {
                    PadBank.DRUMS -> BankDrumsColor
                    PadBank.BASS -> BankBassColor
                    PadBank.SYNTH -> BankSynthColor
                    PadBank.SPLICE -> BankSpliceColor
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isBankSelected) bankColor else ChassisDark)
                        .border(1.dp, if (isBankSelected) bankColor else ChassisBorder, RoundedCornerShape(4.dp))
                        .clickable { viewModel.setBank(bank) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BANK ${bank.label}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isBankSelected) Color.Black else Color.LightGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Sub Toolbar: Roll Selector + Real-time Waveform Visualizer + Tweak Pop-Up Trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Roll Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Roll",
                    tint = if (rollRate != RollRate.OFF) NeonOrange else Color.Gray,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "ROLL:",
                    fontSize = 8.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                RollRate.values().forEach { rate ->
                    val isRateActive = rollRate == rate
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isRateActive) NeonOrange else ChassisSurface)
                            .clickable { viewModel.setRollRate(rate) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = rate.label,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRateActive) Color.Black else Color.LightGray
                        )
                    }
                }
            }

            // Pad Tweak Pop-Up Trigger (Flat Button)
            Button(
                onClick = { showPadTweakPopup = true },
                colors = ButtonDefaults.buttonColors(containerColor = ChassisDark),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (showPadTweakPopup) NeonCyan else ChassisBorder),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Tweak Pad",
                    tint = NeonCyan,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "PAD TWEAK",
                    fontSize = 8.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // REAL-TIME WAVEFORM VISUALIZER COMPONENT (Displays Oboe Engine Sample)
        RealtimeSampleWaveformVisualizer(
            pad = lastActivatedPad ?: selectedPad,
            isPlaying = isPlaying || activeTriggers.isNotEmpty(),
            peaks = (lastActivatedPad ?: selectedPad)?.let { viewModel.getPadWaveformPeaks(it) } ?: FloatArray(32) { 0.25f },
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 4x4 PAD GRID LAYOUT
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (row in 0..3) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0..3) {
                            // MPC layout: row 3 is bottom (0..3), row 0 is top (12..15)
                            val padIndex = (3 - row) * 4 + col
                            val pad = currentPads.getOrNull(padIndex)
                            if (pad != null) {
                                ExpressivePadItem(
                                    pad = pad,
                                    isTriggered = activeTriggers.contains(pad.id),
                                    isSelected = selectedPad?.id == pad.id,
                                    waveformPeaks = viewModel.getPadWaveformPeaks(pad),
                                    masterWaveform = masterWaveform,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onDown = { vel ->
                                        viewModel.selectPad(pad)
                                        viewModel.onPadDown(pad, vel)
                                    },
                                    onUp = { viewModel.onPadUp(pad) }
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    // PARAMETER POP-UP DIALOG FOR SELECTED PAD
    if (showPadTweakPopup && selectedPad != null) {
        PadTweakModalPopup(
            pad = selectedPad!!,
            onPitchChange = { viewModel.updateSelectedPadPitch(it) },
            onDecayChange = { viewModel.updateSelectedPadDecay(it) },
            onVolumeChange = { viewModel.updateSelectedPadVolume(it) },
            onOpenSampleEditor = {
                showPadTweakPopup = false
                showSampleWaveformEditor = true
            },
            onDismiss = { showPadTweakPopup = false }
        )
    }

    // INTERACTIVE SAMPLE WAVEFORM EDITOR MODAL
    if (showSampleWaveformEditor && selectedPad != null) {
        SampleWaveformEditorModal(
            pad = selectedPad!!,
            viewModel = viewModel,
            onDismiss = { showSampleWaveformEditor = false }
        )
    }
}

// REAL-TIME WAVEFORM VISUALIZER COMPONENT
@Composable
fun RealtimeSampleWaveformVisualizer(
    pad: PadData?,
    isPlaying: Boolean,
    peaks: FloatArray,
    modifier: Modifier = Modifier
) {
    val padColor = pad?.let { Color(it.colorHex) } ?: NeonCyan
    val infiniteTransition = rememberInfiniteTransition(label = "wave_sweep")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "sweep_progress"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0D0F14))
            .border(1.dp, Color(0xFF222634), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sample Info
            Column(modifier = Modifier.width(90.dp)) {
                Text(
                    text = pad?.name ?: "No Sample",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "OBOE 44.1k DSP",
                    fontSize = 7.sp,
                    color = padColor,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Real-Time Waveform Canvas with bouncing peaks and playhead sweep
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                val count = peaks.size.coerceAtLeast(16)
                val barW = w / count

                for (i in 0 until count) {
                    val p = peaks.getOrElse(i) { 0.2f }
                    val vib = if (isPlaying) (kotlin.math.sin((i + waveOffset * 8f).toDouble()).toFloat() * 0.15f) else 0f
                    val barH = ((p + vib).coerceIn(0.08f, 1f) * (h * 0.85f))

                    drawRect(
                        color = padColor.copy(alpha = if (isPlaying) 0.9f else 0.5f),
                        topLeft = Offset(i * barW, midY - barH / 2f),
                        size = Size((barW - 1f).coerceAtLeast(1f), barH)
                    )
                }

                // Playhead sweep line
                if (isPlaying) {
                    val playheadX = waveOffset * w
                    drawLine(
                        color = Color.White,
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, h),
                        strokeWidth = 2f
                    )
                }
            }
        }
    }
}

// REAL-TIME AUDIO WAVEFORM CANVAS OVERLAY DISPLAYING EFFECTS RACK DSP
@Composable
fun PadFxWaveformCanvasOverlay(
    pad: PadData,
    isTriggered: Boolean,
    masterWaveform: FloatArray,
    modifier: Modifier = Modifier
) {
    val padColor = Color(pad.colorHex)
    val infiniteTransition = rememberInfiniteTransition(label = "pad_wave_anim")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing)
        ),
        label = "sweep_progress"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Effects Rack DSP parameter factors
        val cutoffFactor = if (pad.isFxChainBypassed) 1.0f else (pad.filterCutoff / 20000f).coerceIn(0.12f, 1.0f)
        val bitLevels = if (pad.isFxChainBypassed) 16 else pad.bitcrushBits
        val revWet = if (pad.isFxChainBypassed) 0f else pad.reverbSend
        val delWet = if (pad.isFxChainBypassed) 0f else pad.delaySend

        val points = 16
        val stepX = w / (points - 1)

        val path = Path()

        for (i in 0 until points) {
            val x = i * stepX
            val rawValue = if (isTriggered) {
                masterWaveform.getOrElse(i * 2) { (kotlin.math.sin(i * 0.4 + sweepProgress * 6.28).toFloat() * 0.75f) }
            } else {
                (kotlin.math.sin(i * 0.35 + (pad.id * 0.5)).toFloat() * 0.3f)
            }

            // 1. Low-Pass Filter Dampening
            var processed = rawValue * cutoffFactor

            // 2. Bitcrusher Quantization Steps
            if (bitLevels < 16) {
                val levels = bitLevels.coerceAtLeast(2)
                processed = (kotlin.math.round(processed * levels) / levels.toFloat())
            }

            val y = midY - (processed * (midY * 0.8f))

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                if (bitLevels < 16) {
                    // Step/Staircase line for Bitcrush quantification
                    path.lineTo(x, midY - (processed * (midY * 0.8f)))
                    path.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
        }

        // 3. Reverb Room Echo Shadow Line
        if (revWet > 0.05f) {
            drawPath(
                path = path,
                color = NeonCyan.copy(alpha = if (isTriggered) 0.5f else (revWet * 0.3f)),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // 4. Delay Repeat Shadow Line
        if (delWet > 0.05f) {
            drawPath(
                path = path,
                color = NeonTeal.copy(alpha = if (isTriggered) 0.6f else (delWet * 0.35f)),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 5. Main Processed Waveform Signal
        drawPath(
            path = path,
            color = if (isTriggered) Color.White else padColor.copy(alpha = 0.5f),
            style = Stroke(width = if (isTriggered) 2.dp.toPx() else 1.2.dp.toPx())
        )

        // 6. Signal Playhead Sweep
        if (isTriggered) {
            val sweepX = sweepProgress * w
            drawLine(
                color = Color.White,
                start = Offset(sweepX, 0f),
                end = Offset(sweepX, h),
                strokeWidth = 2f
            )
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = Offset(sweepX, midY)
            )
        }
    }
}

// EXPRESSIVE PAD ITEM WITH VISUAL FEEDBACK ANIMATIONS (SCALE & COLOR PULSE)
@Composable
fun ExpressivePadItem(
    pad: PadData,
    isTriggered: Boolean,
    isSelected: Boolean,
    waveformPeaks: FloatArray,
    masterWaveform: FloatArray,
    modifier: Modifier = Modifier,
    onDown: (Float) -> Unit,
    onUp: () -> Unit
) {
    val padColor = Color(pad.colorHex)

    var touchVelocity by remember { mutableFloatStateOf(0.8f) }
    var touchPosition by remember { mutableStateOf(Offset.Zero) }

    // PHYSICS ANIMATION 1: Spring Compression Scale linked to velocity
    val targetScale = if (isTriggered) (1.0f - touchVelocity * 0.16f).coerceIn(0.82f, 0.95f) else 1.0f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "physics_pad_scale"
    )

    // PHYSICS ANIMATION 2: Shockwave Ripple Expansion Radius
    val shockwaveAnim = remember { Animatable(0f) }
    LaunchedEffect(isTriggered) {
        if (isTriggered) {
            shockwaveAnim.snapTo(0f)
            shockwaveAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        } else {
            shockwaveAnim.snapTo(0f)
        }
    }

    // VISUAL FEEDBACK 2: Color Pulsing & Radial Burst Animation
    val glowPulse = remember { Animatable(0f) }
    LaunchedEffect(isTriggered) {
        if (isTriggered) {
            glowPulse.snapTo(1f)
            glowPulse.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }
    }

    // Animated Flat Material Fill with Flash Effect
    val animatedBg by animateColorAsState(
        targetValue = if (isTriggered) Color.White.copy(alpha = 0.85f + touchVelocity * 0.15f)
        else if (glowPulse.value > 0.4f) padColor.copy(alpha = 0.9f)
        else if (isSelected) padColor.copy(alpha = 0.25f)
        else ChassisSurface,
        animationSpec = tween(durationMillis = if (isTriggered) 10 else 120),
        label = "pad_flat_bg"
    )

    val borderColor = if (isTriggered || glowPulse.value > 0.3f) Color.White
    else if (isSelected) padColor
    else ChassisBorder

    val borderWidth = if (isTriggered) (2.dp + (2.dp * touchVelocity)) else if (isSelected) 1.5.dp else 1.dp

    Box(
        modifier = modifier
            .testTag("pad_${pad.id}")
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(pad.id) {
                detectTapGestures(
                    onPress = { offset ->
                        touchPosition = offset
                        val relativeY = (offset.y / size.height).coerceIn(0f, 1f)
                        val expressiveVelocity = (0.45f + relativeY * 0.55f).coerceIn(0.2f, 1.0f)
                        touchVelocity = expressiveVelocity
                        onDown(expressiveVelocity)
                        tryAwaitRelease()
                        onUp()
                    }
                )
            }
    ) {
        // VISUAL FEEDBACK 3: Outer Radiant Aura Glow Effect Canvas
        if (glowPulse.value > 0.01f || isTriggered) {
            Canvas(modifier = Modifier.fillMaxSize().padding(-4.dp)) {
                val expansion = glowPulse.value * (12.dp.toPx() * touchVelocity)
                val alpha = (glowPulse.value * 0.85f * touchVelocity).coerceIn(0f, 1f)
                drawRoundRect(
                    color = padColor.copy(alpha = alpha),
                    topLeft = Offset(-expansion / 2f, -expansion / 2f),
                    size = Size(size.width + expansion, size.height + expansion),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }
        }

        // Flat Pad Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(animatedBg)
                .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
                .padding(4.dp)
        ) {
            // PHYSICS VELOCITY SHOCKWAVE & RIPPLE CANVAS OVERLAY
            if (isTriggered || shockwaveAnim.value > 0.01f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxRadius = (size.width * 0.85f) * touchVelocity
                    val radius = shockwaveAnim.value * maxRadius
                    val alpha = (1.0f - shockwaveAnim.value).coerceIn(0f, 1f) * touchVelocity

                    // Physics Radial Shockwave Burst
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.9f),
                        radius = radius,
                        center = if (touchPosition != Offset.Zero) touchPosition else Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = (4f * (1f - shockwaveAnim.value)).coerceAtLeast(1f))
                    )

                    // Physics Velocity Level Indicator Bar on Right Edge
                    val barWidth = 3.dp.toPx()
                    val barHeight = size.height * touchVelocity * 0.8f
                    drawRoundRect(
                        color = when {
                            touchVelocity > 0.85f -> NeonRed
                            touchVelocity > 0.55f -> NeonAmber
                            else -> NeonLime
                        }.copy(alpha = 0.9f),
                        topLeft = Offset(size.width - barWidth - 1.dp.toPx(), size.height - barHeight - 2.dp.toPx()),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                }
            }
            // Pad Number
            Text(
                text = "${pad.id + 1}",
                fontSize = 8.sp,
                color = if (isTriggered) Color.Black else Color.Gray,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.TopStart)
            )

            // Center: Name & Pitch
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = pad.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTriggered) Color.Black else Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp,
                    maxLines = 1
                )
                if (pad.pitch != 0f) {
                    Text(
                        text = "${if (pad.pitch > 0) "+" else ""}${pad.pitch.toInt()}st",
                        fontSize = 7.sp,
                        color = if (isTriggered) Color.Black else padColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Real-Time Audio Waveform Canvas Overlay Displaying Effects Rack Processing
            PadFxWaveformCanvasOverlay(
                pad = pad,
                isTriggered = isTriggered,
                masterWaveform = masterWaveform,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(horizontal = 2.dp, vertical = 1.dp)
            )

            // Choke dot
            if (pad.chokeGroup > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(4.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (isTriggered) Color.Black else NeonAmber)
                )
            }
        }
    }
}

// PARAMETER TWEAK POPUP FOR PAD (Flat Material Dialog)
@Composable
fun PadTweakModalPopup(
    pad: PadData,
    onPitchChange: (Float) -> Unit,
    onDecayChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenSampleEditor: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(310.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Color(pad.colorHex), RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = ChassisDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PAD TWEAK", fontSize = 8.sp, color = Color(pad.colorHex), fontWeight = FontWeight.Bold)
                        Text(pad.name, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Tune, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pitch
                Text("PITCH: ${pad.pitch.toInt()} SEMITONES", fontSize = 8.sp, color = Color.Gray)
                Slider(
                    value = pad.pitch,
                    onValueChange = onPitchChange,
                    valueRange = -12f..12f,
                    colors = SliderDefaults.colors(thumbColor = Color(pad.colorHex), activeTrackColor = Color(pad.colorHex))
                )

                // Decay
                Text("DECAY: ${String.format("%.2f", pad.decay)}s", fontSize = 8.sp, color = Color.Gray)
                Slider(
                    value = pad.decay,
                    onValueChange = onDecayChange,
                    valueRange = 0.05f..2.5f,
                    colors = SliderDefaults.colors(thumbColor = Color(pad.colorHex), activeTrackColor = Color(pad.colorHex))
                )

                // Volume
                Text("VOLUME: ${(pad.volume * 100).toInt()}%", fontSize = 8.sp, color = Color.Gray)
                Slider(
                    value = pad.volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = Color(pad.colorHex), activeTrackColor = Color(pad.colorHex))
                )

                Spacer(modifier = Modifier.height(6.dp))

                // OPEN SAMPLE WAVEFORM EDITOR BUTTON
                Button(
                    onClick = onOpenSampleEditor,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222634)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .border(1.dp, NeonCyan, RoundedCornerShape(4.dp))
                ) {
                    Text("OPEN WAVEFORM EDITOR (TRIM/LOOP)", fontSize = 8.sp, fontWeight = FontWeight.Black, color = NeonCyan)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(pad.colorHex)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                ) {
                    Text("FERTIG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}
