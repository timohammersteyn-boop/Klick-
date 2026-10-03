package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import com.example.model.GridSnapshot
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel

@Composable
fun GridSnapshotBar(
    viewModel: SchwungViewModel,
    modifier: Modifier = Modifier
) {
    val snapshots by viewModel.snapshots.collectAsState()
    val activeSlot by viewModel.activeSnapshotSlot.collectAsState()
    val notification by viewModel.snapshotNotification.collectAsState()

    var showSaveModalForSlot by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Notification pill
        AnimatedVisibility(visible = notification != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(NeonAmber.copy(alpha = 0.2f))
                    .border(1.dp, NeonAmber, RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = notification ?: "",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonAmber
                )
                IconButton(
                    onClick = { viewModel.clearSnapshotNotification() },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NeonAmber,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        // 4 Flat Snapshot Slots Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Snapshots",
                    tint = NeonYellow,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "SNAPS:",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonYellow
                )
            }

            // 4 Slot buttons
            (0..3).forEach { slotIdx ->
                val snap = snapshots[slotIdx]
                val isActive = activeSlot == slotIdx
                val slotTitle = snap?.name ?: "EMPTY ${slotIdx + 1}"

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isActive) NeonYellow else if (snap != null) ChassisSurface else ChassisDark)
                        .border(
                            1.dp,
                            if (isActive) NeonYellow else if (snap != null) ChassisHighlight else ChassisBorder,
                            RoundedCornerShape(3.dp)
                        )
                        .clickable {
                            if (snap != null) {
                                viewModel.recallSnapshot(slotIdx)
                            } else {
                                viewModel.saveSnapshot(slotIdx)
                            }
                        }
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${slotIdx + 1}. $slotTitle",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isActive) Color.Black else if (snap != null) Color.White else Color.Gray,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        // Save button
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Save to slot",
                            tint = if (isActive) Color.Black else Color.Gray,
                            modifier = Modifier
                                .size(10.dp)
                                .clickable { showSaveModalForSlot = slotIdx }
                        )
                    }
                }
            }
        }
    }

    // Save Snapshot Modal
    showSaveModalForSlot?.let { slot ->
        SaveSnapshotModal(
            slotIndex = slot,
            currentName = snapshots[slot]?.name ?: "SET ${slot + 1}",
            onConfirm = { customName ->
                viewModel.saveSnapshot(slot, customName)
                showSaveModalForSlot = null
            },
            onDismiss = { showSaveModalForSlot = null }
        )
    }
}

@Composable
fun SaveSnapshotModal(
    slotIndex: Int,
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, NeonYellow, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = ChassisDark),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "SAVE SNAPSHOT SLOT ${slotIndex + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonYellow
                )
                Text(
                    text = "Speichert die aktuelle PadGrid-Belegung, Step-Pattern & Master-FX.",
                    fontSize = 8.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Snapshot Name", fontSize = 8.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonYellow,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Abbrechen", fontSize = 9.sp, color = Color.Gray)
                    }

                    Button(
                        onClick = { onConfirm(nameInput.ifBlank { "SET ${slotIndex + 1}" }) },
                        modifier = Modifier.weight(1f).height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonYellow),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Speichern", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}
