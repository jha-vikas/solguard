package com.solguard.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solguard.app.session.SessionState

private val skinTypeDescriptions = listOf(
    "Type I — Very fair, always burns, never tans",
    "Type II — Fair, burns easily, tans minimally",
    "Type III — Medium, sometimes burns, tans gradually",
    "Type IV — Olive/light brown, rarely burns, tans easily",
    "Type V — Brown, very rarely burns",
    "Type VI — Dark brown/black, never burns"
)

private val spfOptions = listOf(0, 15, 30, 50)

private val surfaceOptions = listOf("grass", "sand", "water", "snow", "concrete")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    session: SessionState,
    onSkinTypeChanged: (Int) -> Unit,
    onSPFChanged: (Int) -> Unit,
    onSurfaceChanged: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text("Skin Type (Fitzpatrick Scale)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            skinTypeDescriptions.forEachIndexed { index, desc ->
                val selected = session.skinTypeIndex == index
                val shape = RoundedCornerShape(8.dp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(shape)
                        .clickable { onSkinTypeChanged(index) }
                        .then(
                            if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                            else Modifier
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected, onClick = { onSkinTypeChanged(index) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(desc, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Sunscreen SPF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                spfOptions.forEach { spf ->
                    val selected = session.spf == spf
                    val label = if (spf == 0) "None" else "SPF $spf"
                    FilterChip(
                        selected = selected,
                        onClick = { onSPFChanged(spf) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Surface Type", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Reflective surfaces like snow and sand increase UV exposure",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                surfaceOptions.forEach { surface ->
                    val selected = session.surfaceType == surface
                    val emoji = when (surface) {
                        "grass" -> "Grass"
                        "sand" -> "Sand"
                        "water" -> "Water"
                        "snow" -> "Snow"
                        "concrete" -> "City"
                        else -> surface
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { onSurfaceChanged(surface) },
                        label = { Text(emoji, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Text(
                    "Settings are saved automatically and persist between app launches.",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
