package com.picbumper

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
    ) {
        homeViewModel.onSystemDeleteFinished()
    }

    LaunchedEffect(homeUiState.systemDeletePendingUris) {
        val pendingUris = homeUiState.systemDeletePendingUris
        if (!pendingUris.isNullOrEmpty()) {
            val pendingIntent = bumperRepository.buildDeleteIntentSender(pendingUris)
            if (pendingIntent != null) {
                val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                deleteLauncher.launch(request)
            } else {
                homeViewModel.onSystemDeleteFinished()
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
