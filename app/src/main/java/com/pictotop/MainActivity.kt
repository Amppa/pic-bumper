package com.pictotop

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import com.pictotop.data.MediaRepository
import com.pictotop.ui.home.HomeScreen
import com.pictotop.ui.home.HomeViewModel
import com.pictotop.ui.settings.SettingsScreen
import com.pictotop.ui.settings.SettingsViewModel
import com.pictotop.ui.theme.PicToTopTheme

enum class Screen {
    HOME,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val mediaRepository by lazy { MediaRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PicToTopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PicToTopAppContent(
                        homeViewModel = homeViewModel,
                        settingsViewModel = settingsViewModel,
                        mediaRepository = mediaRepository
                    )
                }
            }
        }
    }
}

@Composable
fun PicToTopAppContent(
    homeViewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel,
    mediaRepository: MediaRepository
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
                val pendingIntent = mediaRepository.buildDeleteIntentSender(pendingUris)
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
                val pendingIntent = mediaRepository.buildWriteIntentSender(pendingUris)
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

    AnimatedContent(
        targetState = currentScreen,
        label = "ScreenTransition",
        transitionSpec = {
            if (targetState == Screen.SETTINGS) {
                (slideInHorizontally { fullWidth -> fullWidth } + fadeIn())
                    .togetherWith(slideOutHorizontally { fullWidth -> -fullWidth / 3 } + fadeOut())
            } else {
                (slideInHorizontally { fullWidth -> -fullWidth / 3 } + fadeIn())
                    .togetherWith(slideOutHorizontally { fullWidth -> fullWidth } + fadeOut())
            }
        }
    ) { screen ->
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
