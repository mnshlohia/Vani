package com.example.vani.presentation

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vani.data.StreamRepository
import com.example.vani.data.Video
import com.example.vani.data.VideoRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val videoRepository: VideoRepository,
    private val streamRepository: StreamRepository
) : ViewModel() {

    private val _videos = mutableStateOf<List<Video>>(emptyList())
    val videos: State<List<Video>> = _videos

    private val _permissionGranted = mutableStateOf(false)
    val permissionGranted: State<Boolean> = _permissionGranted

    fun onPermissionResult(granted: Boolean) {
        _permissionGranted.value = granted
        if (granted) {
            fetchVideos()
        }
    }

    private fun fetchVideos() {
        viewModelScope.launch {
            // In a real app, use IO dispatcher
            _videos.value = videoRepository.getVideos()
        }
    }
}

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(VideoRepository(context), StreamRepository()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
