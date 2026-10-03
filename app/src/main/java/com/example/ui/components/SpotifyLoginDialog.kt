package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LibraryMusic
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
import com.example.ui.theme.*
import com.example.viewmodel.SchwungViewModel

@Composable
fun SpotifyLoginDialog(
    viewModel: SchwungViewModel,
    onDismiss: () -> Unit
) {
    val authState by viewModel.spotifyRepository.authState.collectAsState()
    var clientKeyInput by remember { mutableStateOf(authState.clientKey) }

    val spotifyGreen = Color(0xFF1DB954)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(spotifyGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = "Spotify",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "SPOTIFY DJ STREAMING",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Pioneer CDJ / XDJ Workstation Integration",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Connection Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ChassisSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, spotifyGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Connected",
                            tint = spotifyGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = authState.userDisplayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = authState.userTier,
                                fontSize = 10.sp,
                                color = spotifyGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Spotify Web API Access Token (Optional):",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = clientKeyInput,
                    onValueChange = { clientKeyInput = it },
                    placeholder = { Text("Paste Bearer Token or API Key...", fontSize = 10.sp, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = spotifyGreen,
                        unfocusedBorderColor = ChassisBorder,
                        focusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Feature Note: Pioneer CDJ players stream full audio features, beat grids, BPM, key, and real-time audio previews across USB Channels 1/2 and 3/4.",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    lineHeight = 13.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.spotifyRepository.updateSpotifyCredentials(clientKeyInput)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("SAVE & CONNECT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color.Gray, fontSize = 11.sp)
            }
        },
        containerColor = ChassisDark
    )
}
