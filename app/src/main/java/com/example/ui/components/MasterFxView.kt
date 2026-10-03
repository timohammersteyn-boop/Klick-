package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PadBank
import com.example.model.PadData
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel
import kotlin.math.roundToInt

enum class FxRackMode(val label: String) {
    MASTER_BUS("MASTER FX BUS"),
    PAD_CHAIN("PAD FX RACK CHAIN")
}

@Composable
fun MasterFxView(
    viewModel: SchwungViewModel,
    modifier: Modifier = Modifier
) {
    var fxMode by remember { mutableStateOf(FxRackMode.PAD_CHAIN) }

    val filterCutoff by viewModel.filterCutoff.collectAsState()
    val filterResonance by viewModel.filterResonance.collectAsState()
    val delayWet by viewModel.delayWet.collectAsState()
    val delayFeedback by viewModel.delayFeedback.collectAsState()
    val driveSaturation by viewModel.driveSaturation.collectAsState()
    val isTapeStop by viewModel.isTapeStop.collectAsState()
    val isGlitchRoll by viewModel.isGlitchRoll.collectAsState()

    val masterReverbWet by viewModel.masterReverbWet.collectAsState()
    val masterReverbRoom by viewModel.masterReverbRoom.collectAsState()
    val masterBitcrusherWet by viewModel.masterBitcrusherWet.collectAsState()
    val masterBitcrusherBits by viewModel.masterBitcrusherBits.collectAsState()

    var xyTouchPoint by remember { mutableStateOf(Offset(0.9f, 0.2f)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChassisBlack)
            .padding(8.dp)
    ) {
        // Mode Selector: [MASTER FX BUS] vs [PAD FX RACK CHAIN]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FxRackMode.values().forEach { mode ->
                val isSelected = fxMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) NeonPurple else ChassisDark)
                        .border(1.dp, if (isSelected) NeonPurple else ChassisBorder, RoundedCornerShape(4.dp))
                        .clickable { fxMode = mode },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSelected) Color.Black else Color.LightGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (fxMode == FxRackMode.PAD_CHAIN) {
            // PAD FX RACK CHAIN VIEW
            PadFxChainRackView(viewModel = viewModel)
        } else {
            // GLOBAL MASTER FX BUS VIEW
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                // XY Touch Pad for Filter Cutoff & Resonance
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                        .border(1.dp, NeonPurple.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = OledScreenBg),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("xy_filter_pad")
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val normX = (change.position.x / size.width).coerceIn(0.05f, 1f)
                                    val normY = (change.position.y / size.height).coerceIn(0.05f, 1f)
                                    xyTouchPoint = Offset(normX, normY)

                                    val newCutoff = 200f * Math.pow(100.0, normX.toDouble()).toFloat()
                                    val newResonance = 0.5f + (1f - normY) * 4.0f
                                    viewModel.setMasterFilterCutoff(newCutoff)
                                    viewModel.setMasterFilterResonance(newResonance)
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val gridLines = 8
                            val stepX = size.width / gridLines
                            val stepY = size.height / gridLines
                            for (i in 1 until gridLines) {
                                drawLine(Color(0xFF1E2234), Offset(i * stepX, 0f), Offset(i * stepX, size.height))
                                drawLine(Color(0xFF1E2234), Offset(0f, i * stepY), Offset(size.width, i * stepY))
                            }

                            val touchPxX = xyTouchPoint.x * size.width
                            val touchPxY = xyTouchPoint.y * size.height

                            drawCircle(NeonPurple.copy(alpha = 0.25f), radius = 32.dp.toPx(), center = Offset(touchPxX, touchPxY))
                            drawCircle(NeonPurple, radius = 8.dp.toPx(), center = Offset(touchPxX, touchPxY))
                            drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(touchPxX, touchPxY))
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            Text("RESONANT XY FILTER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonPurple)
                            Text("CUTOFF: ${filterCutoff.toInt()} Hz | RESON: ${String.format("%.1f", filterResonance)}", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tape Stop & Glitch Roll Performance FX Triggers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.setTapeStop(!isTapeStop) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isTapeStop) NeonRed else ChassisDark),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Icon(Icons.Default.SlowMotionVideo, contentDescription = "Tape Stop", tint = if (isTapeStop) Color.Black else NeonRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("VINYL TAPE STOP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isTapeStop) Color.Black else Color.White)
                    }

                    Button(
                        onClick = { viewModel.setGlitchRoll(!isGlitchRoll) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isGlitchRoll) NeonCyan else ChassisDark),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("GLITCH REPEAT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isGlitchRoll) Color.Black else Color.White)
                    }
                }
            }
        }
    }
}

// PAD FX RACK CHAIN VIEW: Real-time Audio FX for Specific Pads
@Composable
fun PadFxChainRackView(
    viewModel: SchwungViewModel,
    modifier: Modifier = Modifier
) {
    val currentBank by viewModel.currentBank.collectAsState()
    val selectedPad by viewModel.selectedPad.collectAsState()

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

    val activePad = selectedPad ?: currentPads.firstOrNull()

    Column(modifier = modifier.fillMaxSize()) {
        // Bank Selection Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PadBank.values().forEach { bank ->
                val isSelected = currentBank == bank
                val bankColor = when (bank) {
                    PadBank.DRUMS -> BankDrumsColor
                    PadBank.BASS -> BankBassColor
                    PadBank.SYNTH -> BankSynthColor
                    PadBank.SPLICE -> BankSpliceColor
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) bankColor else ChassisDark)
                        .clickable { viewModel.setBank(bank) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(bank.label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 16 Pad Selector Row (Select Pad to Edit FX)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            currentPads.take(8).forEach { pad ->
                val isPadSelected = activePad?.id == pad.id
                val padColor = Color(pad.colorHex)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isPadSelected) padColor else ChassisDark)
                        .border(1.dp, if (isPadSelected) Color.White else ChassisBorder, RoundedCornerShape(3.dp))
                        .clickable { viewModel.selectPad(pad) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${pad.id + 1}\n${pad.name.take(4)}",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPadSelected) Color.Black else Color.White,
                        lineHeight = 8.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (activePad != null) {
            // Signal Flow Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                colors = CardDefaults.cardColors(containerColor = ChassisDark),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "TARGET PAD ${activePad.id + 1}: ${activePad.name.uppercase()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(activePad.colorHex)
                        )
                    }

                    // Master FX Bypass Switch for Pad
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("FX BYPASS", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = activePad.isFxChainBypassed,
                            onCheckedChange = { bypassed ->
                                viewModel.updatePadFx(activePad.id, activePad.bank, isFxChainBypassed = bypassed)
                            },
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
            }

            // FX Rack Modules List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // MODULE 1: LOW-PASS RESONANT FILTER
                item {
                    FxModuleCard(
                        title = "1. LOW-PASS FILTER (LPF)",
                        accentColor = NeonPurple,
                        isBypassed = activePad.isFxChainBypassed
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CUTOFF: ${activePad.filterCutoff.toInt()} Hz", fontSize = 8.sp, color = Color.White)
                                Text("RESONANCE Q: ${String.format("%.1f", activePad.filterResonance)}", fontSize = 8.sp, color = NeonPurple)
                            }
                            Slider(
                                value = activePad.filterCutoff,
                                onValueChange = { cutoff ->
                                    viewModel.updatePadFx(activePad.id, activePad.bank, filterCutoff = cutoff)
                                },
                                valueRange = 200f..20000f,
                                colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                            )
                        }
                    }
                }

                // MODULE 2: BITCRUSHER & LO-FI REDUCTION
                item {
                    FxModuleCard(
                        title = "2. BITCRUSHER & LO-FI REDUCTION",
                        accentColor = NeonCoral,
                        isBypassed = activePad.isFxChainBypassed
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("RESOLUTION: ${activePad.bitcrushBits}-BIT", fontSize = 8.sp, color = Color.White)
                                Text(
                                    when (activePad.bitcrushBits) {
                                        16 -> "CRISP 16-BIT"
                                        12 -> "VINTAGE MPC12"
                                        8 -> "SP1200 8-BIT"
                                        else -> "EXTREME 4-BIT"
                                    },
                                    fontSize = 8.sp,
                                    color = NeonCoral
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(16 to "16-bit", 12 to "12-bit", 8 to "8-bit", 4 to "4-bit").forEach { (bits, label) ->
                                    val isSelected = activePad.bitcrushBits == bits
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isSelected) NeonCoral else ChassisSurface)
                                            .clickable { viewModel.updatePadFx(activePad.id, activePad.bank, bitcrushBits = bits) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                }

                // MODULE 3: REVERB SEND (AMBIE SPACE)
                item {
                    FxModuleCard(
                        title = "3. REVERB ROOM SEND",
                        accentColor = NeonCyan,
                        isBypassed = activePad.isFxChainBypassed
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("REVERB SEND: ${(activePad.reverbSend * 100).toInt()}%", fontSize = 8.sp, color = Color.White)
                            }
                            Slider(
                                value = activePad.reverbSend,
                                onValueChange = { send ->
                                    viewModel.updatePadFx(activePad.id, activePad.bank, reverbSend = send)
                                },
                                valueRange = 0f..1.0f,
                                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                            )
                        }
                    }
                }

                // MODULE 4: TAPE DELAY SEND
                item {
                    FxModuleCard(
                        title = "4. TAPE DELAY SEND",
                        accentColor = NeonTeal,
                        isBypassed = activePad.isFxChainBypassed
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("DELAY SEND: ${(activePad.delaySend * 100).toInt()}%", fontSize = 8.sp, color = Color.White)
                            }
                            Slider(
                                value = activePad.delaySend,
                                onValueChange = { send ->
                                    viewModel.updatePadFx(activePad.id, activePad.bank, delaySend = send)
                                },
                                valueRange = 0f..1.0f,
                                colors = SliderDefaults.colors(thumbColor = NeonTeal, activeTrackColor = NeonTeal)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FxModuleCard(
    title: String,
    accentColor: Color,
    isBypassed: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, if (isBypassed) Color.Gray else accentColor, RoundedCornerShape(6.dp)),
        colors = CardDefaults.cardColors(containerColor = ChassisDark),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isBypassed) Color.Gray else accentColor
                )
                if (isBypassed) {
                    Text("BYPASSED", fontSize = 7.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
            content()
        }
    }
}
