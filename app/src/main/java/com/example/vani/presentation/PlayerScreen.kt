package com.example.vani.presentation

import android.Manifest
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.vani.MainActivity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(UnstableApi::class, ExperimentalPermissionsApi::class)
@Composable
fun PlayerScreen(
    url: String,
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Permission state for Microphone (needed for Auto Captions)
    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // Force Landscape for player
    DisposableEffect(Unit) {
        val activity = context as? MainActivity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(url) {
        viewModel.initializePlayer(url)
    }

    // Torrent Dialog
    if (viewModel.showTorrentDialog.value) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTorrentDialog() },
            title = { Text("Torrent Link Detected") },
            text = { Text("This player cannot stream torrents directly. Open in external torrent app?") },
            confirmButton = {
                Button(onClick = { viewModel.openTorrentExternally() }) {
                    Text("Open")
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.dismissTorrentDialog(); onBack() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error Dialog
    viewModel.errorMessage.value?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissError(); onBack() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                Button(onClick = { viewModel.dismissError(); onBack() }) {
                    Text("OK")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player.value
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Close Button
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Caption Toggle
            Button(
                onClick = {
                    if (micPermissionState.hasPermission) {
                        viewModel.toggleCaptions()
                    } else {
                        micPermissionState.launchPermissionRequest()
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isCaptionsEnabled.value) MaterialTheme.colorScheme.primary else Color.Gray
                )
            ) {
                Text(if (micPermissionState.hasPermission) "CC" else "Enable CC")
            }

            // Captions Overlay
            if (viewModel.isCaptionsEnabled.value && viewModel.captionText.value.isNotEmpty()) {
                Text(
                    text = viewModel.captionText.value,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 50.dp)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
