package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppMode
import com.example.ui.theme.*

@Composable
fun ModeNavigationTabs(
    currentMode: AppMode,
    onSelectMode: (AppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple(AppMode.PAD_GRID, "PAD GRID", Icons.Default.GridView),
        Triple(AppMode.PIONEER_SPOTIFY_DJ, "PIONEER & SPOTIFY", Icons.Default.Album),
        Triple(AppMode.STEP_SEQUENCER, "STEP SEQ", Icons.Default.LinearScale),
        Triple(AppMode.SPLICE_MCP, "SPLICE MCP", Icons.Default.CloudSync),
        Triple(AppMode.SLICER_STUDIO, "SLICER", Icons.Default.ContentCut),
        Triple(AppMode.FX_RACK, "FX RACK", Icons.Default.Tune)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ChassisDark)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (mode, label, icon) ->
            val isSelected = currentMode == mode
            val activeColor = when (mode) {
                AppMode.PAD_GRID -> NeonOrange
                AppMode.PIONEER_SPOTIFY_DJ -> Color(0xFF1DB954)
                AppMode.STEP_SEQUENCER -> NeonLime
                AppMode.SPLICE_MCP -> NeonAmber
                AppMode.SLICER_STUDIO -> NeonCyan
                AppMode.FX_RACK -> NeonPurple
                else -> Color.White
            }

            val bgColor = if (isSelected) activeColor.copy(alpha = 0.18f) else ChassisSurface
            val borderColor = if (isSelected) activeColor else ChassisBorder
            val contentColor = if (isSelected) activeColor else Color.Gray

            Row(
                modifier = Modifier
                    .testTag("tab_${mode.name.lowercase()}")
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .clickable { onSelectMode(mode) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
