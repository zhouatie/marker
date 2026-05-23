package com.atie.marker.watch.capture

import android.Manifest
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.atie.marker.watch.MarkerWatchApp
import kotlinx.coroutines.delay

class CaptureActivity : ComponentActivity() {
    private val viewModel: CaptureViewModel by viewModels {
        val app = application as MarkerWatchApp
        CaptureViewModelFactory(
            repository = app.markerRepository,
            locationReader = WatchLocationReader(this),
            haptics = WatchHaptics(this),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val state by viewModel.uiState.collectAsState()
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    viewModel.onLocationPermissionResult(granted)
                }

                LaunchedEffect(Unit) {
                    val sourceDeviceId = Settings.Secure.getString(
                        contentResolver,
                        Settings.Secure.ANDROID_ID,
                    )
                    viewModel.startIfNeeded(sourceDeviceId)
                }
                LaunchedEffect(state) {
                    if (state == CaptureUiState.WaitingForPermission) {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
                LaunchedEffect(state) {
                    if (state == CaptureUiState.Completed || state is CaptureUiState.Failed) {
                        delay(700)
                        finishAndRemoveTask()
                    }
                }

                CaptureStatusScreen(state)
            }
        }
    }
}

@Composable
private fun CaptureStatusScreen(state: CaptureUiState) {
    val text = when (state) {
        CaptureUiState.Idle -> "准备记录"
        CaptureUiState.Accepted -> "已记录时间"
        CaptureUiState.WaitingForPermission -> "需要定位权限"
        CaptureUiState.Completed -> "已记录时间"
        is CaptureUiState.Failed -> state.message
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
