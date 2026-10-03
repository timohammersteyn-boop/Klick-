package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.PadBank
import com.example.ui.theme.*
import com.example.util.WavExportType
import com.example.util.WavExporter
import com.example.viewmodel.SchwungViewModel
import java.io.File

@Composable
fun WavExportModal(
    viewModel: SchwungViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val patterns by viewModel.patterns.collectAsState()
    val activePatIdx by viewModel.activePatternIndex.collectAsState()
    val currentPattern = patterns.getOrNull(activePatIdx) ?: patterns.first()
    val bpm by viewModel.bpm.collectAsState()
    val selectedPad by viewModel.selectedPad.collectAsState()

    val drumsPads by viewModel.drumsPads.collectAsState()
    val bassPads by viewModel.bassPads.collectAsState()
    val synthPads by viewModel.synthPads.collectAsState()
    val splicePads by viewModel.splicePads.collectAsState()

    val padsByBank = mapOf(
        PadBank.DRUMS to drumsPads,
        PadBank.BASS to bassPads,
        PadBank.SYNTH to synthPads,
        PadBank.SPLICE to splicePads
    )

    var isExporting by remember { mutableStateOf(false) }
    var lastExportedFile by remember { mutableStateOf<File?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, NeonAmber, RoundedCornerShape(12.dp))
                .testTag("wav_export_modal_dialog"),
            colors = CardDefaults.cardColors(containerColor = ChassisDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export WAV",
                            tint = NeonAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "EXPORT WAV AUDIO SEQUENCE & STEMS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Text(
                    text = "PATTERN: '${currentPattern.name.uppercase()}' | BPM: $bpm | FORMAT: 16-BIT 44.1KHZ STEREO WAV",
                    fontSize = 8.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Export Progress Notification Banner
                AnimatedVisibility(visible = statusMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = NeonLime.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Done", tint = NeonLime, modifier = Modifier.size(16.dp))
                                Text(
                                    text = statusMessage ?: "",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonLime
                                )
                            }

                            if (lastExportedFile != null) {
                                Button(
                                    onClick = {
                                        WavExporter.shareWavFile(context, lastExportedFile!!)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonLime),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Black, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("SHARE WAV", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                }
                            }
                        }
                    }
                }

                // EXPORT OPTIONS LIST
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 260.dp)
                ) {
                    // 1. FULL MASTER MIX WAV
                    item {
                        ExportOptionRow(
                            title = "FULL MASTER MIX WAV",
                            subtitle = "Exports complete 16-step sequence with all 4 tracks combined",
                            badgeText = "MASTER MIX",
                            badgeColor = NeonAmber,
                            onClick = {
                                isExporting = true
                                val file = WavExporter.exportSequenceToWav(
                                    context = context,
                                    pattern = currentPattern,
                                    padsByBank = padsByBank,
                                    bpm = bpm,
                                    exportType = WavExportType.FULL_MASTER_MIX
                                )
                                lastExportedFile = file
                                statusMessage = "Exported '${file.name}' (${file.length() / 1024} KB)"
                                isExporting = false
                                WavExporter.shareWavFile(context, file)
                            }
                        )
                    }

                    // 2. DRUMS STEM WAV
                    item {
                        ExportOptionRow(
                            title = "DRUMS TRACK STEM WAV",
                            subtitle = "Isolated Drum kit stem (Kick, Snare, Hi-Hat, Clap)",
                            badgeText = "DRUMS STEM",
                            badgeColor = NeonCyan,
                            onClick = {
                                isExporting = true
                                val file = WavExporter.exportSequenceToWav(
                                    context = context,
                                    pattern = currentPattern,
                                    padsByBank = padsByBank,
                                    bpm = bpm,
                                    exportType = WavExportType.STEM_DRUMS
                                )
                                lastExportedFile = file
                                statusMessage = "Exported '${file.name}' (${file.length() / 1024} KB)"
                                isExporting = false
                                WavExporter.shareWavFile(context, file)
                            }
                        )
                    }

                    // 3. BASS 808 STEM WAV
                    item {
                        ExportOptionRow(
                            title = "BASS 808 TRACK STEM WAV",
                            subtitle = "Isolated Sub-Bass & 808 sequence stem",
                            badgeText = "BASS 808 STEM",
                            badgeColor = NeonPurple,
                            onClick = {
                                isExporting = true
                                val file = WavExporter.exportSequenceToWav(
                                    context = context,
                                    pattern = currentPattern,
                                    padsByBank = padsByBank,
                                    bpm = bpm,
                                    exportType = WavExportType.STEM_BASS
                                )
                                lastExportedFile = file
                                statusMessage = "Exported '${file.name}' (${file.length() / 1024} KB)"
                                isExporting = false
                                WavExporter.shareWavFile(context, file)
                            }
                        )
                    }

                    // 4. SYNTH STEM WAV
                    item {
                        ExportOptionRow(
                            title = "SYNTH LEAD TRACK STEM WAV",
                            subtitle = "Isolated Synth Lead & Melody sequence stem",
                            badgeText = "SYNTH STEM",
                            badgeColor = NeonLime,
                            onClick = {
                                isExporting = true
                                val file = WavExporter.exportSequenceToWav(
                                    context = context,
                                    pattern = currentPattern,
                                    padsByBank = padsByBank,
                                    bpm = bpm,
                                    exportType = WavExportType.STEM_SYNTH
                                )
                                lastExportedFile = file
                                statusMessage = "Exported '${file.name}' (${file.length() / 1024} KB)"
                                isExporting = false
                                WavExporter.shareWavFile(context, file)
                            }
                        )
                    }

                    // 5. SPLICE CHOPS STEM WAV
                    item {
                        ExportOptionRow(
                            title = "SPLICE CHOPS TRACK STEM WAV",
                            subtitle = "Isolated Splice chop sample sequence stem",
                            badgeText = "SPLICE STEM",
                            badgeColor = NeonOrange,
                            onClick = {
                                isExporting = true
                                val file = WavExporter.exportSequenceToWav(
                                    context = context,
                                    pattern = currentPattern,
                                    padsByBank = padsByBank,
                                    bpm = bpm,
                                    exportType = WavExportType.STEM_SPLICE
                                )
                                lastExportedFile = file
                                statusMessage = "Exported '${file.name}' (${file.length() / 1024} KB)"
                                isExporting = false
                                WavExporter.shareWavFile(context, file)
                            }
                        )
                    }

                    // 6. SINGLE ACTIVE PAD WAV
                    selectedPad?.let { pad ->
                        item {
                            ExportOptionRow(
                                title = "EXPORT SINGLE PAD SOUND (${pad.name})",
                                subtitle = "Exports isolated WAV sample audio for selected ${pad.bank.label} Pad ${pad.id + 1}",
                                badgeText = "SINGLE PAD",
                                badgeColor = Color(pad.colorHex),
                                onClick = {
                                    isExporting = true
                                    val file = WavExporter.exportPadSoundToWav(
                                        context = context,
                                        pad = pad,
                                        bpm = bpm
                                    )
                                    lastExportedFile = file
                                    statusMessage = "Exported Pad WAV '${file.name}' (${file.length() / 1024} KB)"
                                    isExporting = false
                                    WavExporter.shareWavFile(context, file)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportOptionRow(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ChassisBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = ChassisSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(badgeColor)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = subtitle,
                    fontSize = 8.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Export",
                tint = badgeColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
