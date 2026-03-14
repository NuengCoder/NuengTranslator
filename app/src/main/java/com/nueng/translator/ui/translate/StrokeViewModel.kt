package com.nueng.translator.ui.translate

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StrokeUiState(
    val isModelReady: Boolean = false,
    val isDownloading: Boolean = false,
    val recognizedCandidates: List<String> = emptyList(),
    val selectedText: String = "",
    val currentStrokes: List<List<Offset>> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class StrokeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(StrokeUiState())
    val uiState: StateFlow<StrokeUiState> = _uiState.asStateFlow()

    private var inkBuilder = Ink.builder()
    private var strokeBuilder: Ink.Stroke.Builder? = null
    private var model: DigitalInkRecognitionModel? = null
    private val modelManager = RemoteModelManager.getInstance()

    // Current language for recognition (default: Chinese simplified)
    private var currentLangTag = "zh-Hans-CN"

    init {
        downloadModel(currentLangTag)
    }

    fun setLanguage(langCode: String) {
        val tag = when (langCode) {
            "zh" -> "zh-Hans-CN"
            "th" -> "th-TH"
            "lo" -> "lo-LA"
            "vi" -> "vi-VN"
            "en" -> "en-US"
            "id" -> "id-ID"
            else -> "zh-Hans-CN"
        }
        if (tag != currentLangTag) {
            currentLangTag = tag
            downloadModel(tag)
        }
    }

    private fun downloadModel(langTag: String) {
        _uiState.value = _uiState.value.copy(isDownloading = true, isModelReady = false)

        val modelIdentifier: DigitalInkRecognitionModelIdentifier?
        try {
            modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(langTag)
        } catch (e: MlKitException) {
            _uiState.value = _uiState.value.copy(
                isDownloading = false,
                errorMessage = "Language not supported: $langTag"
            )
            return
        }

        if (modelIdentifier == null) {
            _uiState.value = _uiState.value.copy(
                isDownloading = false,
                errorMessage = "No model for: $langTag"
            )
            return
        }

        model = DigitalInkRecognitionModel.builder(modelIdentifier).build()

        modelManager.download(model!!, DownloadConditions.Builder().build())
            .addOnSuccessListener {
                Log.d("StrokeVM", "Model downloaded: $langTag")
                _uiState.value = _uiState.value.copy(
                    isModelReady = true,
                    isDownloading = false
                )
            }
            .addOnFailureListener { e ->
                Log.e("StrokeVM", "Model download failed", e)
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    errorMessage = "Download failed: ${e.message}"
                )
            }
    }

    // --- Touch events from canvas ---
    fun onStrokeStart(point: Offset) {
        strokeBuilder = Ink.Stroke.builder()
        strokeBuilder?.addPoint(Ink.Point.create(point.x, point.y))

        val strokes = _uiState.value.currentStrokes.toMutableList()
        strokes.add(listOf(point))
        _uiState.value = _uiState.value.copy(currentStrokes = strokes)
    }

    fun onStrokeMove(point: Offset) {
        strokeBuilder?.addPoint(Ink.Point.create(point.x, point.y))

        val strokes = _uiState.value.currentStrokes.toMutableList()
        if (strokes.isNotEmpty()) {
            val lastStroke = strokes.last().toMutableList()
            lastStroke.add(point)
            strokes[strokes.lastIndex] = lastStroke
            _uiState.value = _uiState.value.copy(currentStrokes = strokes)
        }
    }

    fun onStrokeEnd() {
        strokeBuilder?.let { builder ->
            inkBuilder.addStroke(builder.build())
        }
        strokeBuilder = null

        // Auto-recognize after each stroke
        recognize()
    }

    fun clearCanvas() {
        inkBuilder = Ink.builder()
        strokeBuilder = null
        _uiState.value = _uiState.value.copy(
            currentStrokes = emptyList(),
            recognizedCandidates = emptyList(),
            selectedText = ""
        )
    }

    fun selectCandidate(text: String) {
        _uiState.value = _uiState.value.copy(selectedText = text)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun recognize() {
        val currentModel = model ?: return
        if (!_uiState.value.isModelReady) return

        val ink = inkBuilder.build()
        if (ink.strokes.isEmpty()) return

        val recognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(currentModel).build()
        )

        recognizer.recognize(ink)
            .addOnSuccessListener { result ->
                val candidates = result.candidates.map { it.text }
                _uiState.value = _uiState.value.copy(
                    recognizedCandidates = candidates.take(10)
                )
                recognizer.close()
            }
            .addOnFailureListener { e ->
                Log.e("StrokeVM", "Recognition failed", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Recognition failed: ${e.message}"
                )
                recognizer.close()
            }
    }
}
