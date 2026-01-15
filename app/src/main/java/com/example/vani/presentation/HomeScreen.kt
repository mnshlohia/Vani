package com.example.vani.presentation

import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import com.example.vani.data.Video
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel
) {
    val context = LocalContext.current
    val permissionState = rememberMultiplePermissionsState(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    )

    LaunchedEffect(key1 = permissionState.allPermissionsGranted) {
        viewModel.onPermissionResult(permissionState.allPermissionsGranted)
    }

    var urlInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Vani Universal Player") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // URL Input Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Enter Stream/Magnet URL") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (urlInput.isNotEmpty()) {
                            val encodedUrl = URLEncoder.encode(urlInput, StandardCharsets.UTF_8.toString())
                            navController.navigate("player/$encodedUrl")
                        }
                    }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                    }
                }
            }

            if (!permissionState.allPermissionsGranted) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Permission needed to load local videos")
                        Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else {
                VideoGrid(videos = viewModel.videos.value, onVideoClick = { video ->
                    val encodedUrl = URLEncoder.encode(video.uri.toString(), StandardCharsets.UTF_8.toString())
                    navController.navigate("player/$encodedUrl")
                })
            }
        }
    }
}

@Composable
fun VideoGrid(videos: List<Video>, onVideoClick: (Video) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(videos) { video ->
            VideoItem(video, onVideoClick)
        }
    }
}

@Composable
fun VideoItem(video: Video, onClick: (Video) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick(video) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                 AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.uri)
                        .decoderFactory(VideoFrameDecoder.Factory())
                        .build(),
                    contentDescription = video.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = video.name,
                modifier = Modifier.padding(8.dp),
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
