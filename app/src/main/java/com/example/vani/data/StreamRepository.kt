package com.example.vani.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

enum class StreamType {
    LOCAL,
    YOUTUBE,
    TORRENT,
    DIRECT_LINK,
    ERROR
}

data class StreamInfo(
    val url: String,
    val type: StreamType,
    val title: String? = null
)

class StreamRepository {

    suspend fun resolveStream(input: String): StreamInfo = withContext(Dispatchers.IO) {
        when {
            input.startsWith("magnet:?") -> {
                StreamInfo(input, StreamType.TORRENT, "Torrent Link")
            }
            input.contains("youtube.com") || input.contains("youtu.be") -> {
                val extractedUrl = extractYoutubeStream(input)
                if (extractedUrl != null) {
                    StreamInfo(extractedUrl, StreamType.DIRECT_LINK, "YouTube Stream") // Treat as direct link once extracted
                } else {
                    StreamInfo(input, StreamType.ERROR, "Could not extract stream from YouTube link")
                }
            }
            input.startsWith("content://") -> {
                StreamInfo(input, StreamType.LOCAL, "Local Video")
            }
            else -> {
                StreamInfo(input, StreamType.DIRECT_LINK, "Network Stream")
            }
        }
    }

    /**
     * A basic attempt to extract a playable HLS manifest from YouTube.
     * Checks for 'hlsManifestUrl' or 'dashManifestUrl'.
     */
    private fun extractYoutubeStream(youtubeUrl: String): String? {
        return try {
            val url = URL(youtubeUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            connection.requestMethod = "GET"
            connection.connect()

            val inputStream = connection.inputStream
            val content = inputStream.bufferedReader().use { it.readText() }

            // 1. Try HLS Manifest
            var pattern = Pattern.compile("\"hlsManifestUrl\":\"(.*?)\"")
            var matcher = pattern.matcher(content)

            if (matcher.find()) {
                return matcher.group(1)?.replace("\\/", "/")
            }

            // 2. Try DASH Manifest
            pattern = Pattern.compile("\"dashManifestUrl\":\"(.*?)\"")
            matcher = pattern.matcher(content)

            if (matcher.find()) {
                return matcher.group(1)?.replace("\\/", "/")
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
