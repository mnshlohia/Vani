package com.example.vani.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.vani.data.StreamRepository
import com.example.vani.data.StreamType
import kotlinx.coroutines.launch
import java.util.Locale

class PlayerViewModel(
    private val context: Context,
    private val streamRepository: StreamRepository
) : ViewModel() {

    private val _player = mutableStateOf<ExoPlayer?>(null)
    val player: State<ExoPlayer?> = _player

    private val _isCaptionsEnabled = mutableStateOf(false)
    val isCaptionsEnabled: State<Boolean> = _isCaptionsEnabled

    private val _captionText = mutableStateOf("")
    val captionText: State<String> = _captionText

    private val _showTorrentDialog = mutableStateOf(false)
    val showTorrentDialog: State<Boolean> = _showTorrentDialog

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private var speechRecognizer: SpeechRecognizer? = null
    private var torrentLink: String? = null

    init {
        _player.value = ExoPlayer.Builder(context).build()
    }

    fun initializePlayer(url: String) {
        viewModelScope.launch {
            val streamInfo = streamRepository.resolveStream(url)

            when (streamInfo.type) {
                StreamType.TORRENT -> {
                    // ExoPlayer cannot play magnets. Signal UI to open external app.
                    torrentLink = streamInfo.url
                    _showTorrentDialog.value = true
                }
                StreamType.ERROR -> {
                    _errorMessage.value = streamInfo.title ?: "Error loading stream"
                }
                else -> {
                    val mediaItem = MediaItem.fromUri(Uri.parse(streamInfo.url))
                    _player.value?.apply {
                        setMediaItem(mediaItem)
                        prepare()
                        playWhenReady = true
                    }
                }
            }
        }
    }

    fun openTorrentExternally() {
        torrentLink?.let { link ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "No app found to handle magnet link", e)
                _errorMessage.value = "No app found to handle Magnet links."
            }
        }
        _showTorrentDialog.value = false
    }

    fun dismissTorrentDialog() {
        _showTorrentDialog.value = false
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun toggleCaptions() {
        _isCaptionsEnabled.value = !_isCaptionsEnabled.value
        if (_isCaptionsEnabled.value) {
            startSpeechRecognition()
        } else {
            stopSpeechRecognition()
            _captionText.value = ""
        }
    }

    private fun startSpeechRecognition() {
        // Real implementation using Android SpeechRecognizer.
        // This listens to the microphone (the "standard" way apps hear audio if not system signed).
        // It requires RECORD_AUDIO permission which we will handle in UI.
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        // Restart listening loop
                        if (_isCaptionsEnabled.value) {
                            startSpeechRecognition()
                        }
                    }
                    override fun onError(error: Int) {
                         if (_isCaptionsEnabled.value) {
                            // Add slight delay or just restart
                            // startSpeechRecognition() // Potential loop risk, kept simple
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _captionText.value = matches[0]
                        }
                        if (_isCaptionsEnabled.value) {
                             startSpeechRecognition()
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _captionText.value = matches[0]
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                speechRecognizer?.startListening(intent)
            } else {
                _captionText.value = "Speech Recognition not available on device"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _captionText.value = "Error starting captions"
        }
    }

    private fun stopSpeechRecognition() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onCleared() {
        super.onCleared()
        _player.value?.release()
        stopSpeechRecognition()
    }
}

class PlayerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            return PlayerViewModel(context.applicationContext, StreamRepository()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
