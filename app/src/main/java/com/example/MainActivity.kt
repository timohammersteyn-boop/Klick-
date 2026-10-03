package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SchwungLiveTheme {
                val viewModel: SchwungViewModel = viewModel()
                var showProjectsSheet by remember { mutableStateOf(false) }
                var isStatusHeaderExpanded by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                val isPlaying by viewModel.isPlaying.collectAsState()
                val bpm by viewModel.bpm.collectAsState()
                val swingPercent by viewModel.swingPercent.collectAsState()
                val isPioneerLocked by viewModel.midiClockService.isClockLocked.collectAsState()

                // 5 Swipable Modules: Ableton Move Console (Startseite) -> Sequencer -> Splice -> Pioneer DJ -> Master FX
                val modules = listOf(
                    "ABLETON MOVE CONSOLE",
                    "STEP SEQUENCER",
                    "SPLICE MCP AI",
                    "PIONEER DJ & SPOTIFY",
                    "MASTER FX RACK"
                )
                val pagerState = rememberPagerState(initialPage = 0) { modules.size }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ChassisBlack),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = ChassisBlack
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(ChassisBlack)
                    ) {
                        // COLLAPSIBLE STATUSLEISTE (Auf- und Zuklappbar)
                        AnimatedVisibility(
                            visible = isStatusHeaderExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                HeaderTransportBar(
                                    viewModel = viewModel,
                                    onOpenProjects = { showProjectsSheet = true }
                                )
                                // Minimal collapse trigger strip
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF141722))
                                        .clickable { isStatusHeaderExpanded = false }
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Zuklappen",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "STATUSLEISTE ZUKLAPPEN (MINIMIZE)",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        // KOMPAKTE STATUSLEISTE (Default - schlank, aufgeräumt, keine Unruhe)
                        if (!isStatusHeaderExpanded) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .background(Color(0xFF12141C))
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left: Brand Pill & Expand button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(NeonOrange)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "ABLETON MOVE",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black
                                        )
                                    }

                                    // Pioneer MIDI Sync indicator
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isPioneerLocked) Color(0xFF0F2D1F) else Color(0xFF262A3B))
                                            .border(1.dp, if (isPioneerLocked) NeonLime else Color.Gray, RoundedCornerShape(3.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isPioneerLocked) NeonLime else Color.Gray)
                                            )
                                            Text(
                                                text = "PIONEER 24 PPQN",
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPioneerLocked) NeonLime else Color.Gray,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                // Center: Realtime Master Waveform Oscilloscope & Active Module Name
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MasterWaveformOscilloscope(
                                        viewModel = viewModel,
                                        modifier = Modifier
                                            .width(110.dp)
                                            .height(20.dp),
                                        showLabel = false
                                    )

                                    Text(
                                        text = "SWIPE: ${modules[pagerState.currentPage]}",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    // Pager dots
                                    modules.indices.forEach { idx ->
                                        Box(
                                            modifier = Modifier
                                                .size(if (pagerState.currentPage == idx) 6.dp else 4.dp)
                                                .clip(CircleShape)
                                                .background(if (pagerState.currentPage == idx) NeonCyan else Color(0xFF33384F))
                                                .clickable {
                                                    coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                                                }
                                        )
                                    }
                                }

                                // Right: Quick BPM, Play Toggle & Aufklappen trigger
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$bpm BPM [${swingPercent}%]",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonYellow,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // Quick Play/Stop
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlaying) NeonLime else Color(0xFF222634))
                                            .clickable { viewModel.togglePlayPause() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Stop",
                                            tint = if (isPlaying) Color.Black else Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    // Expand Header Button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF1E2230))
                                            .clickable { isStatusHeaderExpanded = true }
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Aufklappen",
                                                tint = NeonCyan,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "AUFKLAPPEN",
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonCyan
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Performance Grid Snapshots Bar (Save & Recall Live Performance States)
                        GridSnapshotBar(
                            viewModel = viewModel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        // HORIZONTAL PAGER: SWIPEN PRO MODUL
                        // Startseite = Page 0: Ableton Move Hardware Console responsive fixiert im Landscape Frame
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) { page ->
                            when (page) {
                                0 -> {
                                    // Startseite: Ableton Move Original Hardware Oberfläche responsive fixiert
                                    AbletonMoveHardwareConsole(
                                        viewModel = viewModel,
                                        onOpenProjects = { showProjectsSheet = true }
                                    )
                                }
                                1 -> {
                                    // Modul 2: 16-Step Sequencer & Pattern Groove
                                    StepSequencerView(viewModel = viewModel)
                                }
                                2 -> {
                                    // Modul 3: Splice MCP AI Sound Synthesizer & Tag Filtered Sample Crate
                                    SpliceMcpView(viewModel = viewModel)
                                }
                                3 -> {
                                    // Modul 4: Pioneer DJ CDJ-3000 Link & Spotify Streaming Workstation
                                    DjWorkstationView(viewModel = viewModel)
                                }
                                4 -> {
                                    // Modul 5: Master FX Rack (Reverb, Delay, Bitcrusher, Filter)
                                    MasterFxView(viewModel = viewModel)
                                }
                            }
                        }

                        // BOTTOM MODULE NAVIGATION BAR (Tap or Swipe)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .background(Color(0xFF0F1017))
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            modules.forEachIndexed { index, moduleTitle ->
                                val isCurrent = pagerState.currentPage == index
                                val shortTitle = when (index) {
                                    0 -> "1. MOVE CONSOLE"
                                    1 -> "2. SEQUENCER"
                                    2 -> "3. SPLICE MCP"
                                    3 -> "4. PIONEER DJ"
                                    else -> "5. MASTER FX"
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(0.85f)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isCurrent) NeonCyan else Color(0xFF181B26))
                                        .border(
                                            1.dp,
                                            if (isCurrent) NeonCyan else Color(0xFF262A3B),
                                            RoundedCornerShape(3.dp)
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shortTitle,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isCurrent) Color.Black else Color.LightGray,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    if (showProjectsSheet) {
                        ProjectsSheet(
                            viewModel = viewModel,
                            onDismiss = { showProjectsSheet = false }
                        )
                    }
                }
            }
        }
    }
}
