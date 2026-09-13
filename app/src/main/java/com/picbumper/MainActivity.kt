package com.picbumper

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.picbumper.data.MediaBumperRepository
import com.picbumper.ui.home.HomeScreen
import com.picbumper.ui.home.HomeViewModel
import com.picbumper.ui.settings.SettingsScreen
import com.picbumper.ui.settings.SettingsViewModel
import com.picbumper.ui.theme.PicBumperTheme

enum class Screen {
    HOME,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val bumperRepository by lazy { MediaBumperRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PicBumperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PicBumperAppContent(
                        homeViewModel = homeViewModel,
                        settingsViewModel = settingsViewModel,
                        bumperRepository = bumperRepository
                    )
                }
            }
        }
    }
}

@Composable
fun PicBumperAppContent(
    homeViewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel,
    bumperRepository: MediaBumperRepository
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val homeUiState by homeViewModel.uiState.collectAsState()

    // System delete launcher for Android 11+ (API 30+) external files
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val success = result.resultCode == Activity.RESULT_OK
        homeViewModel.onSystemDeleteFinished(success = success)
    }

    // System write launcher for Android 11+ (API 30+) rename/modify permissions
    val writeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val success = result.resultCode == Activity.RESULT_OK
        homeViewModel.onSystemWriteFinished(success = success)
    }

    LaunchedEffect(homeUiState.systemDeletePendingUris) {
        val pendingUris = homeUiState.systemDeletePendingUris
        if (!pendingUris.isNullOrEmpty()) {
            try {
                val pendingIntent = bumperRepository.buildDeleteIntentSender(pendingUris)
                if (pendingIntent != null) {
                    val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    deleteLauncher.launch(request)
                } else {
                    homeViewModel.onSystemDeleteFinished(success = false)
                }
            } catch (_: Exception) {
                homeViewModel.onSystemDeleteFinished(success = false)
            }
        }
    }

    LaunchedEffect(homeUiState.systemWritePendingUris) {
        val pendingUris = homeUiState.systemWritePendingUris
        if (!pendingUris.isNullOrEmpty()) {
            try {
                val pendingIntent = bumperRepository.buildWriteIntentSender(pendingUris)
                if (pendingIntent != null) {
                    val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    writeLauncher.launch(request)
                } else {
                    homeViewModel.onSystemWriteFinished(success = false)
                }
            } catch (_: Exception) {
                homeViewModel.onSystemWriteFinished(success = false)
            }
        }
    }

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            Screen.HOME -> HomeScreen(
                viewModel = homeViewModel,
                onNavigateToSettings = { currentScreen = Screen.SETTINGS }
            )
            Screen.SETTINGS -> SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { currentScreen = Screen.HOME }
            )
        }
    }
}
