package com.nueng.translator.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import java.io.File

class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startMs: Long = 0L

    fun startRecording() {
        val file = File(context.cacheDir, "voice_recording_tmp.3gp")
        outputFile = file
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        recorder!!.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setAudioSamplingRate(8000)
            setAudioEncodingBitRate(8000)  // 8kbps — ~1KB/sec raw, ~1.3KB/sec Base64
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        startMs = System.currentTimeMillis()
    }

    /** Stops recording. Returns Pair(base64Data, durationSeconds) or null on error. */
    fun stopRecording(): Pair<String, Int>? {
        return try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            val durationSec = ((System.currentTimeMillis() - startMs) / 1000).toInt().coerceAtLeast(1)
            val bytes = outputFile?.readBytes() ?: return null
            val b64 = Base64.encodeToString(bytes, Base64.DEFAULT)
            outputFile?.delete()
            Pair(b64, durationSec)
        } catch (_: Exception) {
            recorder?.release(); recorder = null
            null
        }
    }

    fun cancelRecording() {
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release(); recorder = null
        outputFile?.delete()
    }
}
