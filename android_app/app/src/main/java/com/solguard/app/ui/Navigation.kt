package com.solguard.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.LocationServices
import com.solguard.app.ui.screens.*

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Details : Screen("details")
    data object Settings : Screen("settings")
}

@Composable
fun SolGuardNavigation(viewModel: SolGuardViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val session by viewModel.session.collectAsState()

    var liveLux by remember { mutableFloatStateOf(0f) }
    var liveHeading by remember { mutableFloatStateOf(0f) }
    var livePressure by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        viewModel.sensorReader.startListening()
    }

    LaunchedEffect(Unit) {
        while (true) {
            liveLux = viewModel.sensorReader.currentLux
            liveHeading = viewModel.sensorReader.currentHeading
            livePressure = viewModel.sensorReader.currentPressureHpa
            kotlinx.coroutines.delay(500)
        }
    }

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                session = session,
                currentLux = liveLux,
                onAssess = {
                    fetchLocation(context) { lat, lon -> viewModel.setLocation(lat, lon) }
                    viewModel.assessUV()
                },
                onNavigateDetails = { navController.navigate(Screen.Details.route) },
                onNavigateSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Details.route) {
            DetailsScreen(
                session = session,
                currentLux = liveLux,
                currentHeading = liveHeading,
                currentPressure = livePressure,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                session = session,
                onSkinTypeChanged = { viewModel.setSkinType(it) },
                onSPFChanged = { viewModel.setSPF(it) },
                onSurfaceChanged = { viewModel.setSurfaceType(it) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Suppress("MissingPermission")
private fun fetchLocation(
    context: android.content.Context,
    onResult: (Double, Double) -> Unit
) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onResult(location.latitude, location.longitude)
            }
        }
    } catch (_: SecurityException) {
        // Permission not yet granted
    }
}
