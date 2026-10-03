package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.Presets
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsSheet(
    viewModel: SchwungViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedProjects by viewModel.savedProjects.collectAsState()
    val demoProjects = remember { Presets.getDemoProjects() }

    var newProjectTitle by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(false) }
    var exportedFile by remember { mutableStateOf<File?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ChassisDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROJECTS & AUDIO EXPORT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonCyan,
                    letterSpacing = 0.5.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Project Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newProjectTitle,
                    onValueChange = { newProjectTitle = it },
                    placeholder = { Text("Project Title...", fontSize = 11.sp, color = Color.Gray) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = ChassisBorder,
                        focusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
                Button(
                    onClick = {
                        viewModel.saveProject(newProjectTitle)
                        newProjectTitle = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Text("SAVE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Master WAV Audio Recording & Export Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ChassisSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MASTER WAV RECORDER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonRed
                        )
                        Text(
                            text = if (isExporting) "Recording live master mix..." else "Record live audio performance to WAV",
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }

                    if (!isExporting) {
                        Button(
                            onClick = {
                                isExporting = true
                                viewModel.startExportRecording()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("START REC", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = {
                                isExporting = false
                                viewModel.stopExportRecording(context) { file ->
                                    exportedFile = file
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonLime),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("STOP & EXPORT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }

            // Share Export Dialog
            if (exportedFile != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        shareWavFile(context, exportedFile!!)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrange),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SHARE RECORDED WAV FILE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Demo Templates
            Text(
                text = "FACTORY DEMO BEATS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
            ) {
                items(demoProjects) { demo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ChassisBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                viewModel.loadDemoProject(demo)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(containerColor = ChassisSurface),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(demo.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    "${demo.bpm} BPM // ${demo.swingPercent}% Schwung Swing",
                                    fontSize = 9.sp,
                                    color = NeonOrange,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }

                if (savedProjects.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "SAVED BEATS (ROOM DB)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                    items(savedProjects) { proj ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ChassisBorder, RoundedCornerShape(6.dp)),
                            colors = CardDefaults.cardColors(containerColor = ChassisSurface),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(proj.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                    Text(
                                        "${proj.bpm} BPM // ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(proj.lastModified))}",
                                        fontSize = 8.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun shareWavFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share SchwungLive Beat WAV"))
    } catch (_: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(file))
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share SchwungLive Beat WAV"))
    }
}
