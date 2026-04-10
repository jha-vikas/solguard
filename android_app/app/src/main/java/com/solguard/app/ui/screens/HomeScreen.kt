package com.solguard.app.ui.screens

import android.Manifest
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.solguard.app.session.SessionState
import com.solguard.app.uv.HourlyUV
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    session: SessionState,
    currentLux: Float,
    onAssess: () -> Unit,
    onNavigateDetails: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val locationPermission = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermission.allPermissionsGranted) {
            locationPermission.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SolGuard") },
                actions = {
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (!session.hasAssessed) {
                WelcomeCard(
                    locationGranted = locationPermission.allPermissionsGranted,
                    onAssess = {
                        if (locationPermission.allPermissionsGranted) onAssess()
                        else locationPermission.launchMultiplePermissionRequest()
                    }
                )
            } else {
                UVGauge(uvIndex = session.uvIndex, categoryName = session.uvCategory?.name ?: "")
                Spacer(modifier = Modifier.height(16.dp))
                RecommendationCard(session)
                Spacer(modifier = Modifier.height(12.dp))
                ExposureCard(session)
                Spacer(modifier = Modifier.height(12.dp))
                if (session.hourlyCurve.isNotEmpty()) {
                    UVCurveCard(session.hourlyCurve)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                SensorStatusCard(currentLux = currentLux, session = session)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onAssess,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh")
                    }
                    OutlinedButton(
                        onClick = onNavigateDetails,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Details")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WelcomeCard(locationGranted: Boolean, onAssess: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.WbSunny,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "SolGuard",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "UV Index & Sun Safety",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Estimates real-time UV index using your phone's sensors. " +
                "No internet required. Tells you safe sun exposure time and Vitamin D needs.",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAssess,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (locationGranted) "Check UV Index" else "Grant Location & Check UV",
                    fontSize = 16.sp
                )
            }
            if (!locationGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Location is needed to compute sun position accurately.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun UVGauge(uvIndex: Double, categoryName: String) {
    val color = uvColor(uvIndex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("UV INDEX", fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val sweepAngle = (uvIndex / 15.0 * 240).toFloat()
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        style = Stroke(width = 16f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = color,
                        startAngle = 150f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 16f, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%.1f".format(uvIndex),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(categoryName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(session: SessionState) {
    val cat = session.uvCategory ?: return
    val color = uvColor(session.uvIndex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(cat.shortAdvice, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color)
            Spacer(modifier = Modifier.height(8.dp))
            cat.detailedActions.forEach { action ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("  \u2022  ", color = color, fontSize = 14.sp)
                    Text(action, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ExposureCard(session: SessionState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sun Exposure", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))
            DetailRow(
                "Safe time (no sunscreen)",
                if (session.safeExposureMinutes >= 999) "Safe all day"
                else "${session.safeExposureMinutes} min"
            )
            DetailRow(
                "Safe time (SPF ${session.spf})",
                if (session.safeExposureWithSPF >= 999) "Safe all day"
                else formatDuration(session.safeExposureWithSPF)
            )
            DetailRow(
                "Vitamin D (1000 IU)",
                if (session.vitaminDMinutes >= 999) "UV too low now"
                else "${session.vitaminDMinutes} min of face+arms"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Based on Fitzpatrick Type ${session.skinTypeIndex + 1}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun UVCurveCard(curve: List<HourlyUV>) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val maxUV = curve.maxOfOrNull { it.uvIndex }?.coerceAtLeast(1.0) ?: 1.0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Today's UV Forecast (clear sky)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                val w = size.width
                val h = size.height
                val points = curve.map { hv ->
                    val x = ((hv.hour - 5).toFloat() / 15f) * w
                    val y = h - (hv.uvIndex.toFloat() / maxUV.toFloat()) * h
                    Offset(x, y)
                }

                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = Color(0xFFFF9800),
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }

                val nowIdx = curve.indexOfFirst { it.hour == currentHour }
                if (nowIdx >= 0 && nowIdx < points.size) {
                    drawCircle(
                        color = Color(0xFFE65100),
                        radius = 8f,
                        center = points[nowIdx]
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("6am", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text("12pm", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text("6pm", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }

            val peakHour = curve.maxByOrNull { it.uvIndex }
            if (peakHour != null && peakHour.uvIndex > 0) {
                Text(
                    "Peak: UV %.1f at %d:00".format(peakHour.uvIndex, peakHour.hour),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SensorStatusCard(currentLux: Float, session: SessionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%.0f".format(currentLux), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("lux", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%.1f°".format(session.sunAltitude), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("sun alt", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%.0fm".format(session.altitudeMeters), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("altitude", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatDuration(minutes: Int): String {
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes} min"
}

private fun uvColor(uvIndex: Double): Color {
    return when {
        uvIndex < 3.0 -> Color(0xFF4CAF50)
        uvIndex < 6.0 -> Color(0xFFFFC107)
        uvIndex < 8.0 -> Color(0xFFFF9800)
        uvIndex < 11.0 -> Color(0xFFFF5722)
        else -> Color(0xFF9C27B0)
    }
}
