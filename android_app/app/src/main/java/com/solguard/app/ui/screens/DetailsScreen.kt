package com.solguard.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solguard.app.session.SessionState
import com.solguard.app.uv.SunPositionCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    session: SessionState,
    currentLux: Float,
    currentHeading: Float,
    currentPressure: Float,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Technical Details") },
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

            SectionCard("Sun Position") {
                InfoRow("Sun altitude", "%.2f°".format(session.sunAltitude))
                InfoRow("Sun azimuth", "%.2f°".format(session.sunAzimuth))
                InfoRow("Solar zenith angle", "%.2f°".format(session.solarZenithAngle))
                InfoRow("Sun direction", SunPositionCalculator.getCompassDirection(session.sunAzimuth))
                InfoRow("Daytime", if (session.sunAltitude > 0) "Yes" else "No")
            }

            Spacer(modifier = Modifier.height(12.dp))

            SectionCard("UV Calculation Breakdown") {
                val est = session.uvEstimate
                if (est != null) {
                    val baseUV = session.uvIndex / (est.altitudeCorrection * est.cloudFactor * est.ozoneFactor * est.surfaceFactor)
                    InfoRow("Base UV (clear sky, sea level)", "%.2f".format(baseUV.coerceIn(0.0, 15.0)))
                    InfoRow("Altitude correction", "x%.3f (+%.1f%%)".format(est.altitudeCorrection, (est.altitudeCorrection - 1) * 100))
                    InfoRow("Cloud factor", "x%.3f".format(est.cloudFactor))
                    InfoRow("Ozone seasonal factor", "x%.3f".format(est.ozoneFactor))
                    InfoRow("Surface reflection", "x%.2f (%s)".format(est.surfaceFactor, session.surfaceType))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    InfoRow("Final UV Index", "%.1f".format(session.uvIndex), bold = true)
                } else {
                    Text("Assess UV first to see breakdown.", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SectionCard("Raw Sensor Readings") {
                InfoRow("Ambient light", "%.0f lux".format(currentLux))
                InfoRow("Device heading", "%.0f°".format(currentHeading))
                InfoRow("Barometric pressure", if (currentPressure > 0) "%.1f hPa".format(currentPressure) else "Sensor unavailable")
                InfoRow("Barometric altitude", if (currentPressure > 0) "%.0f m".format(session.altitudeMeters) else "N/A")
            }

            Spacer(modifier = Modifier.height(12.dp))

            SectionCard("Location") {
                InfoRow("Latitude", "%.5f°".format(session.latitude))
                InfoRow("Longitude", "%.5f°".format(session.longitude))
            }

            Spacer(modifier = Modifier.height(12.dp))

            SectionCard("Settings Applied") {
                InfoRow("Skin type", "Fitzpatrick Type ${session.skinTypeIndex + 1}")
                InfoRow("SPF", "${session.spf}")
                InfoRow("Surface type", session.surfaceType.replaceFirstChar { it.uppercase() })
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Text(
                    "UV estimates are approximate. Factors like altitude accuracy, local air quality, " +
                    "and nearby reflective surfaces can affect real UV levels. " +
                    "For medical decisions, consult a dermatologist.",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}
