package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pioneer.PioneerDjCertifiedManager
import com.example.ui.theme.*

@Composable
fun PioneerRoutingTab(
    pioneerManager: PioneerDjCertifiedManager,
    modifier: Modifier = Modifier
) {
    val isConnected by pioneerManager.isConnected.collectAsState()
    val detectedModel by pioneerManager.detectedModel.collectAsState()
    val audioRouting by pioneerManager.audioRoutingChannels.collectAsState()
    val latencyBufferMs by pioneerManager.latencyBufferMs.collectAsState()
    val lastMidiCommand by pioneerManager.lastMidiCommand.collectAsState()

    val pioneerModels = listOf(
        "Pioneer CDJ-3000 Professional Player",
        "Pioneer CDJ-2000NXS2 Multi Player",
        "Pioneer XDJ-XZ Workstation",
        "Pioneer XDJ-RX3 All-In-One",
        "Pioneer DDJ-FLX10 4-Deck Controller",
        "Pioneer DJM-900NXS2 4-Ch Mixer"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = ChassisDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header: Connection status
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) NeonLime else Color.Gray)
                    )
                    Text(
                        text = "PIONEER DJ USB WORKSTATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Text(
                    text = "USB HOST CERTIFIED",
                    fontSize = 8.sp,
                    color = NeonLime,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Active Device Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(ChassisSurface)
                    .border(1.dp, ChassisBorder, RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cable,
                    contentDescription = "USB",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = detectedModel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Text(
                        text = "Vendor ID: 0x08E4 (Pioneer Corp) // Pro DJ Link Mode",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hardware Model Selector Row
            Text(
                text = "PIONEER HARDWARE PROFILES (ANY PLAYER VIA USB):",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(3.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(pioneerModels) { model ->
                    val isModelSelected = detectedModel.contains(model.take(16), ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isModelSelected) NeonCyan else ChassisSurface)
                            .border(1.dp, if (isModelSelected) Color.White else ChassisBorder, RoundedCornerShape(4.dp))
                            .clickable { pioneerManager.setPioneerModelManually(model) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = model.replace("Pioneer ", ""),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isModelSelected) Color.Black else Color.LightGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // USB Audio Routing & Channels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Master Channel 1/2
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = ChassisSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonOrange.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Speaker, contentDescription = null, tint = NeonOrange, modifier = Modifier.size(16.dp))
                        Column {
                            Text("MASTER OUT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonOrange)
                            Text("USB Ch 1 & 2", fontSize = 9.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Headphone / Cue Channel 3/4
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = ChassisSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Headphones, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                        Column {
                            Text("CUE / HEADPHONES", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                            Text("USB Ch 3 & 4", fontSize = 9.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Latency Buffer Selection & Live MIDI Monitor
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "USB DSP LATENCY: ${String.format("%.1f", latencyBufferMs)}ms",
                    fontSize = 9.sp,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1.5f, 2.1f, 3.0f, 5.8f).forEach { ms ->
                        val isSel = latencyBufferMs == ms
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isSel) NeonLime else ChassisSurface)
                                .clickable { pioneerManager.setLatencyBuffer(ms) }
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${ms}ms",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Last MIDI command monitor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .border(1.dp, ChassisBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "MIDI LOG: $lastMidiCommand",
                    fontSize = 8.sp,
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
