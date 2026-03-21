package com.nueng.translator.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

object TtsHelper {
    private var tts: TextToSpeech? = null
    private var isReady = false

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                tts?.language = Locale.SIMPLIFIED_CHINESE
                // Reset speech rate after any slow utterance finishes
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId?.startsWith("slow_") == true) {
                            tts?.setSpeechRate(1.0f)
                        }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (utteranceId?.startsWith("slow_") == true) {
                            tts?.setSpeechRate(1.0f)
                        }
                    }
                })
                Log.d("TTS", "TTS initialized")
            } else {
                Log.e("TTS", "TTS init failed: $status")
            }
        }
    }

    fun speak(text: String, langCode: String = "zh") {
        if (!isReady || tts == null) return
        val locale = when (langCode) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "en" -> Locale.US
            "th" -> Locale("th", "TH")
            "vi" -> Locale("vi", "VN")
            "id" -> Locale("id", "ID")
            "lo" -> Locale("lo", "LA")
            else -> Locale.SIMPLIFIED_CHINESE
        }
        tts?.language = locale
        tts?.setSpeechRate(1.0f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun speakSlow(text: String, langCode: String = "zh") {
        if (!isReady || tts == null) return
        val locale = when (langCode) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "en" -> Locale.US
            "th" -> Locale("th", "TH")
            "vi" -> Locale("vi", "VN")
            "id" -> Locale("id", "ID")
            "lo" -> Locale("lo", "LA")
            else -> Locale.SIMPLIFIED_CHINESE
        }
        tts?.language = locale
        tts?.setSpeechRate(0.5f)
        // Utterance ID prefixed with "slow_" so the listener knows to reset rate on done
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "slow_${text.hashCode()}")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}