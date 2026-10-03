package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppMode
import com.example.model.PadBank
import com.example.model.SplicePack
import com.example.model.SpliceSample
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel

@Composable
fun SpliceMcpView(
    viewModel: SchwungViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packs by viewModel.splicePacks.collectAsState()
    val selectedPack by viewModel.selectedPack.collectAsState()
    val selectedSample by viewModel.selectedSample.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()
    val mcpPrompt by viewModel.mcpPromptText.collectAsState()
    val isMcpProcessing by viewModel.isMcpProcessing.collectAsState()
    val mcpNotification by viewModel.mcpNotification.collectAsState()
    val selectedPad by viewModel.selectedPad.collectAsState()

    var textSearchQuery by remember { mutableStateOf("") }
    var isAuditionPlaying by remember { mutableStateOf(false) }

    val categories = listOf("ALL", "DRUM LOOP", "BASS 808", "KEYS", "MELODIC", "VOCAL", "FX & PERC")
    val genres = listOf("ALL", "Lo-Fi", "Trap", "Drill", "Boom Bap", "Synthwave", "Soul", "R&B")

    var selectedGenre by remember { mutableStateOf("ALL") }

    // Multi-criteria filtering
    val allPacksSamples = remember(packs) { packs.flatMap { it.samples } }

    val filteredSamples = allPacksSamples.filter { sample ->
        val matchesText = textSearchQuery.isBlank() ||
                sample.title.contains(textSearchQuery, ignoreCase = true) ||
                sample.instrument.contains(textSearchQuery, ignoreCase = true) ||
                sample.genre.contains(textSearchQuery, ignoreCase = true) ||
                sample.mood.contains(textSearchQuery, ignoreCase = true) ||
                sample.category.contains(textSearchQuery, ignoreCase = true) ||
                sample.tags.any { it.contains(textSearchQuery, ignoreCase = true) } ||
                "${sample.bpm}".contains(textSearchQuery)

        val matchesCategory = (searchFilter == "ALL" || sample.category.contains(searchFilter, ignoreCase = true))
        val matchesGenre = (selectedGenre == "ALL" || sample.genre.equals(selectedGenre, ignoreCase = true) || sample.tags.any { it.equals(selectedGenre, ignoreCase = true) })

        matchesText && matchesCategory && matchesGenre
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChassisBlack)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // TOP SECTION 1: SPLICE MCP AI SOUND SYNTHESIZER BAR
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ChassisDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(14.dp))
                        Text("SPLICE MCP // SYNTHESIZER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonAmber)
                    }
                    Text("Model Context Protocol v1.7", fontSize = 7.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = mcpPrompt,
                        onValueChange = { viewModel.setMcpPrompt(it) },
                        placeholder = { Text("Prompt e.g. 'Punchy 90s boom bap kick'", fontSize = 10.sp, color = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("splice_mcp_prompt_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonAmber,
                            unfocusedBorderColor = ChassisBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )

                    Button(
                        onClick = { viewModel.generateWithSpliceMcp(mcpPrompt) },
                        enabled = !isMcpProcessing && mcpPrompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("splice_mcp_generate_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        if (isMcpProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("SYNTH", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            }
                        }
                    }
                }

                // Quick Prompt Pills
                Spacer(modifier = Modifier.height(3.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val pills = listOf("Punchy 90s Kick", "Subzero 808 Bass", "Dusty Vinyl Snare", "Lush Rhodes", "Soul Vocal Chop")
                    items(pills) { pill ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(ChassisSurface)
                                .clickable {
                                    viewModel.setMcpPrompt(pill)
                                    viewModel.generateWithSpliceMcp(pill)
                                }
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(pill, fontSize = 8.sp, color = Color.LightGray)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // TOP SECTION 2: LIVE SEARCH & CATEGORY FILTERS
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = textSearchQuery,
                onValueChange = { textSearchQuery = it },
                placeholder = { Text("Search Splice samples...", fontSize = 10.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (textSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { textSearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                modifier = Modifier
                    .weight(1.3f)
                    .height(38.dp)
                    .testTag("splice_search_text_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = ChassisBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp)
            )

            if (textSearchQuery.isNotEmpty() || searchFilter != "ALL" || selectedGenre != "ALL") {
                Button(
                    onClick = {
                        textSearchQuery = ""
                        viewModel.setSearchFilter("ALL")
                        selectedGenre = "ALL"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChassisDark),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(38.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = NeonCoral, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("RESET", fontSize = 8.sp, color = NeonCoral, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // CATEGORY CHIPS SCROLL BAR
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(categories) { cat ->
                val isCatSelected = searchFilter == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isCatSelected) NeonCyan else ChassisSurface)
                        .border(1.dp, if (isCatSelected) NeonCyan else ChassisBorder, RoundedCornerShape(4.dp))
                        .clickable { viewModel.setSearchFilter(cat) }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCatSelected) Color.Black else Color.LightGray
                    )
                }
            }
        }

        // Notification banner if MCP created or sliced
        AnimatedVisibility(visible = mcpNotification != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                colors = CardDefaults.cardColors(containerColor = NeonAmber.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mcpNotification ?: "",
                        fontSize = 9.sp,
                        color = NeonAmber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearMcpNotification() },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NeonAmber, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ACTIVE AUDITION WAVEFORM PREVIEW BAR FOR SELECTED SAMPLE
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            colors = CardDefaults.cardColors(containerColor = ChassisDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play / Pause Audition Button
                IconButton(
                    onClick = {
                        isAuditionPlaying = !isAuditionPlaying
                        viewModel.audioEngine.triggerSound(
                            recipe = selectedSample.soundRecipe,
                            velocity = 0.9f,
                            loopKey = selectedSample.soundRecipe
                        )
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonCyan)
                ) {
                    Icon(
                        imageVector = if (isAuditionPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Audition Waveform",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Sample Details & Waveform Visualizer
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedSample.title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${selectedSample.bpm} BPM | ${selectedSample.category}", fontSize = 8.sp, color = NeonOrange, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Waveform Peak Canvas
                    SampleWaveformCanvas(
                        wavePeaks = selectedSample.wavePeaks,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                    )
                }

                // Assign to Active Pad Action Button
                Button(
                    onClick = {
                        viewModel.loadSampleToSelectedPad(selectedSample)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    selectedPad?.let { pad ->
                        Text("ASSIGN TO ${pad.bank.label} P${pad.id + 1}", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    } ?: Text("ASSIGN TO PAD", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // SEARCH RESULTS COUNT HEADER
        Text(
            text = "SEARCH RESULTS (${filteredSamples.size} SAMPLES)",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Color.Gray,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // SAMPLE RESULTS LIST
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(filteredSamples) { sample ->
                val isSampleSelected = selectedSample.id == sample.id
                SpliceSampleListItem(
                    sample = sample,
                    isSelected = isSampleSelected,
                    onSelect = {
                        viewModel.selectSpliceSample(sample)
                        viewModel.audioEngine.triggerSound(
                            recipe = sample.soundRecipe,
                            velocity = 0.9f,
                            loopKey = sample.soundRecipe
                        )
                    },
                    onLoadToPad = {
                        viewModel.loadSampleToSelectedPad(sample)
                    },
                    onSliceToGrid = {
                        viewModel.sliceSampleToGrid(sample)
                        viewModel.setAppMode(AppMode.PAD_GRID)
                    }
                )
            }
        }
    }
}

@Composable
fun SampleWaveformCanvas(
    wavePeaks: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(3.dp))) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        val peaks = if (wavePeaks.isNotEmpty()) wavePeaks else listOf(0.3f, 0.6f, 0.8f, 0.4f, 0.9f, 0.5f, 0.7f, 0.3f)
        val barWidth = (w / peaks.size).coerceAtLeast(2f)

        for (i in peaks.indices) {
            val p = peaks[i]
            val barH = (p * h * 0.8f).coerceAtLeast(2f)
            val x = i * barWidth
            val y = midY - (barH / 2f)

            drawRoundRect(
                color = NeonCyan,
                topLeft = Offset(x, y),
                size = Size(barWidth * 0.8f, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f, 1f)
            )
        }
    }
}

@Composable
fun SpliceSampleListItem(
    sample: SpliceSample,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onLoadToPad: () -> Unit,
    onSliceToGrid: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isSelected) NeonCyan else ChassisBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = ChassisDark),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onSelect,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) NeonCyan else NeonCyan.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Audition",
                        tint = if (isSelected) Color.Black else NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = sample.title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${sample.bpm} BPM", fontSize = 8.sp, color = NeonOrange, fontFamily = FontFamily.Monospace)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(NeonOrange.copy(alpha = 0.2f))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(sample.genre, fontSize = 7.sp, color = NeonOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onLoadToPad,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("LOAD PAD", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }

                Button(
                    onClick = onSliceToGrid,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("SLICE 4x4", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }
            }
        }
    }
}
