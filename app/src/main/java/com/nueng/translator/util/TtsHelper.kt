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

    private fun localeFor(langCode: String): Locale = when (langCode) {
        "zh" -> Locale.SIMPLIFIED_CHINESE
        "en" -> Locale.US
        "th" -> Locale.Builder().setLanguage("th").setRegion("TH").build()
        "vi" -> Locale.Builder().setLanguage("vi").setRegion("VN").build()
        "id" -> Locale.Builder().setLanguage("id").setRegion("ID").build()
        "lo" -> Locale.Builder().setLanguage("lo").setRegion("LA").build()
        else -> Locale.SIMPLIFIED_CHINESE
    }

    fun speak(text: String, langCode: String = "zh") {
        if (!isReady || tts == null) return
        tts?.language = localeFor(langCode)
        tts?.setSpeechRate(1.0f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun speakSlow(text: String, langCode: String = "zh") {
        if (!isReady || tts == null) return
        tts?.language = localeFor(langCode)
        tts?.setSpeechRate(0.5f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "slow_" + text.hashCode())
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
