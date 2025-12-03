package com.tjnuman.sensemeplayer

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.room.Room
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.mfcc.MFCC
import com.google.gson.Gson
import com.tjnuman.sensemeplayer.data.AppDatabase
import com.tjnuman.sensemeplayer.data.SongFeature
import com.tjnuman.sensemeplayer.ui.theme.SenseMePlayerTheme
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {

    // Compose-observable playback state (activity-level)
    var currentPlaylist by mutableStateOf<List<SongFeature>>(emptyList())
    var currentSongIndex by mutableStateOf(0)

    private val TAG = "MusicScanner"

    private var mediaPlayer: MediaPlayer? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startProcessing() else Log.e(TAG, "Permission denied")
    }

    private lateinit var db: AppDatabase
    private var clusterSongsState by mutableStateOf<Map<Int, List<SongFeature>>>(emptyMap())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "senseme_db"
        ).build()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startProcessing()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        setContent {
            SenseMePlayerTheme {

                var progress by remember { mutableStateOf(0f) }
                var statusText by remember { mutableStateOf("Idle") }

                var currentPosition by remember { mutableStateOf(0) }
                var duration by remember { mutableStateOf(1) } // avoid divide-by-zero
                var isUserSeeking by remember { mutableStateOf(false) }

                // Receive processing updates
                LaunchedEffect(Unit) {
                    ProcessingState.progressState = { pct -> progress = pct }
                    ProcessingState.statusState = { txt -> statusText = txt }
                }


                LaunchedEffect(mediaPlayer, currentPlaylist, currentSongIndex) {
                    while (true) {
                        val mp = mediaPlayer
                        if (mp != null && mp.isPlaying && !isUserSeeking) {
                            currentPosition = mp.currentPosition
                            duration = mp.duration
                        }
                        delay(500)
                    }
                }



                // ---------- WHOLE SCREEN IS SCROLLABLE ----------
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Top section: status, progress, refresh button
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = statusText)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Progress indicator (value param is a Float)
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    statusText = "Refreshing mood clusters..."
                                    CoroutineScope(Dispatchers.Default).launch {
                                        val newClusters = rebuildClusters()
                                        withContext(Dispatchers.Main) {
                                            clusterSongsState = newClusters
                                            statusText = "Mood clusters updated!"
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Text("Refresh Mood Clusters")
                            }

                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    // ---------- MUSIC PLAYER CONTROL BAR (visible when playlist not empty) ----------
                    item {
                        if (currentPlaylist.isNotEmpty()) {
                            val curIndex = currentSongIndex.coerceIn(0, currentPlaylist.lastIndex)
                            val nowPlaying = currentPlaylist.getOrNull(curIndex)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Now Playing: ${nowPlaying?.path?.substringAfterLast("/") ?: "None"}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(onClick = { playPrevious() }) { Text("Prev")


                                    }

                                    Button(onClick = {
                                        mediaPlayer?.let {
                                            if (it.isPlaying) it.pause() else it.start()
                                        }
                                    }) {
                                        Text(if (mediaPlayer?.isPlaying == true) "Pause" else "Play")
                                    }

                                    Button(onClick = { playNext() }) { Text("Next") }
                                }

                                Slider(
                                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                    onValueChange = { value ->
                                        isUserSeeking = true
                                        currentPosition = (value * duration).toInt()
                                    },
                                    onValueChangeFinished = {
                                        mediaPlayer?.seekTo(currentPosition)
                                        isUserSeeking = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )


                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    // ---------- CLUSTER LIST ----------
                    // Each cluster is its own item so list scrolls fine
                    clusterSongsState.toSortedMap().forEach { (clusterId, songs) ->
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Mood $clusterId (${songs.size} songs)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                songs.forEachIndexed { index, song ->
                                    Text(
                                        text = song.path.substringAfterLast("/"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clickable {
                                                // IMPORTANT: set UI state before heavy work
                                                currentPlaylist = songs
                                                currentSongIndex = index

                                                // Play asynchronously (non-blocking prepare)
                                                playSong(song, songs, index)
                                            }
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Ensure MediaPlayer released on destroy
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun startProcessing() {
        CoroutineScope(Dispatchers.Default).launch {
            val songs = scanMusic()
            Log.d(TAG, "Found ${songs.size} songs")
            ProcessingState.statusState?.invoke("Processing 0 / ${songs.size}")

            val mfccFeatures = mutableListOf<FloatArray>()

            songs.forEachIndexed { index, path ->
                val mfcc = extractPCMAndMFCC(path) { /* song-level progress */ }

                if (mfcc != null) {
                    mfccFeatures.add(mfcc)
                    val mfccJson = Gson().toJson(mfcc)
                    db.songFeatureDao().insert(SongFeature(path = path, mfcc = mfccJson))
                }

                val overallProgress = (index + 1).toFloat() / songs.size
                ProcessingState.progressState?.invoke(overallProgress)
                ProcessingState.statusState?.invoke("Processing ${index + 1} / ${songs.size}")
            }

            ProcessingState.statusState?.invoke("Processing Complete! Stored MFCC for ${mfccFeatures.size} songs")
            Log.d(TAG, "All songs processed and saved to DB")

            if (mfccFeatures.isNotEmpty()) {
                ProcessingState.statusState?.invoke("Clustering songs...")
                val clusterIndices = kMeans(mfccFeatures, k = 5)

                val allSongs = db.songFeatureDao().getAll()
                val clusterMap = clusterIndices.indices.groupBy { clusterIndices[it] }
                    .mapValues { entry -> entry.value.map { allSongs[it] } }

                withContext(Dispatchers.Main) {
                    clusterSongsState = clusterMap
                    ProcessingState.statusState?.invoke("Mood clustering complete!")
                }
            }
        }
    }

    // Play song non-blocking (prepareAsync) and update state immediately
    private fun playSong(song: SongFeature, playlist: List<SongFeature>, index: Int) {
        try {
            // set UI state first so player UI becomes visible immediately
            currentPlaylist = playlist
            currentSongIndex = index

            // release previous
            mediaPlayer?.reset()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()

            mediaPlayer?.apply {
                setDataSource(song.path)
                setOnPreparedListener {
                    start()
                    Log.d(TAG, "Playback started: ${song.path.substringAfterLast("/")}")
                }
                setOnCompletionListener {
                    // auto next
                    playNext()
                }
                prepareAsync() // non-blocking
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting playback for ${song.path}", e)
        }
    }

    private fun playNext() {
        if (currentPlaylist.isEmpty()) return
        currentSongIndex = (currentSongIndex + 1) % currentPlaylist.size
        val next = currentPlaylist[currentSongIndex]
        playSong(next, currentPlaylist, currentSongIndex)
    }

    private fun playPrevious() {
        if (currentPlaylist.isEmpty()) return
        currentSongIndex = if (currentSongIndex - 1 < 0) currentPlaylist.size - 1 else currentSongIndex - 1
        val prev = currentPlaylist[currentSongIndex]
        playSong(prev, currentPlaylist, currentSongIndex)
    }

    // --- K-Means Implementation ---
    private fun kMeans(data: List<FloatArray>, k: Int, maxIter: Int = 100): IntArray {
        val n = data.size
        val dim = data[0].size
        val centers = Array(k) { FloatArray(dim) }
        val rnd = java.util.Random()

        data.shuffled(rnd).take(k).forEachIndexed { i, arr -> arr.copyInto(centers[i]) }

        val labels = IntArray(n)
        repeat(maxIter) {
            for (i in 0 until n) labels[i] = centers.indices.minByOrNull { c -> euclidean(data[i], centers[c]) }!!

            val counts = IntArray(k)
            val newCenters = Array(k) { FloatArray(dim) }
            for (i in 0 until n) {
                val l = labels[i]
                for (d in 0 until dim) newCenters[l][d] += data[i][d]
                counts[l]++
            }
            for (c in 0 until k) if (counts[c] > 0) for (d in 0 until dim) newCenters[c][d] /= counts[c]
            for (c in 0 until k) newCenters[c].copyInto(centers[c])
        }
        return labels
    }

    private fun euclidean(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += (a[i] - b[i]) * (a[i] - b[i])
        return sqrt(sum)
    }

    private fun scanMusic(): List<String> {
        val songPaths = mutableListOf<String>()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null,
            "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
        )
        cursor?.use {
            while (it.moveToNext()) {
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                songPaths.add(path)
            }
        }
        return songPaths
    }

    private suspend fun rebuildClusters(): Map<Int, List<SongFeature>> {
        val songs = db.songFeatureDao().getAll()
        if (songs.isEmpty()) return emptyMap()
        val features = songs.map { Gson().fromJson(it.mfcc, FloatArray::class.java) }
        val clusters = kMeans(features, k = 5)
        return clusters.indices.groupBy { clusters[it] }
            .mapValues { (_, idxList) -> idxList.map { songs[it] } }
    }

    private suspend fun extractPCMAndMFCC(path: String, progressCallback: (Float) -> Unit): FloatArray? {
        return withContext(Dispatchers.Default) {
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(path)
                var format: MediaFormat? = null
                var trackIndex = -1

                for (i in 0 until extractor.trackCount) {
                    val f = extractor.getTrackFormat(i)
                    val mime = f.getString(MediaFormat.KEY_MIME)
                    if (mime != null && mime.startsWith("audio/")) {
                        format = f
                        trackIndex = i
                        break
                    }
                }
                if (format == null) return@withContext null

                extractor.selectTrack(trackIndex)
                val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val bufferSize = 1024
                val mfcc = MFCC(bufferSize, sampleRate.toFloat(), 13, 40, 300f, 8000f)
                val accumulated = FloatArray(13) { 0f }
                var frameCount = 0
                val inputBuffer = ByteBuffer.allocate(2 * bufferSize)

                while (true) {
                    inputBuffer.clear()
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) break

                    inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    val floatBuffer = FloatArray(bufferSize) { 0f }
                    val samplesToCopy = minOf(sampleSize / 2, bufferSize)
                    for (i in 0 until samplesToCopy) floatBuffer[i] = inputBuffer.short.toFloat() / Short.MAX_VALUE

                    val audioFormat = be.tarsos.dsp.io.TarsosDSPAudioFormat(
                        sampleRate.toFloat(), 16, 1, true, false
                    )
                    val audioEvent = AudioEvent(audioFormat)
                    audioEvent.setFloatBuffer(floatBuffer)
                    mfcc.process(audioEvent)

                    for (i in accumulated.indices) accumulated[i] += mfcc.mfcc[i]
                    frameCount++
                    progressCallback(frameCount / 1000f)
                    extractor.advance()
                }

                if (frameCount > 0) for (i in accumulated.indices) accumulated[i] /= frameCount
                extractor.release()
                accumulated
            } catch (e: Exception) {
                Log.e(TAG, "Error processing $path", e)
                null
            }
        }
    }

    object ProcessingState {
        var progressState: ((Float) -> Unit)? = null
        var statusState: ((String) -> Unit)? = null
    }
}
