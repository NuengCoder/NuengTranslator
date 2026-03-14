package com.nueng.translator.ui.translate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale

@Composable
fun VoiceInputButton(
    langCode: String,
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Speech recognizer launcher
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                onResult(spokenText)
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            launchSpeechRecognizer(langCode, speechLauncher)
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    IconButton(
        onClick = {
            if (hasPermission) {
                launchSpeechRecognizer(langCode, speechLauncher)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        modifier = modifier
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = "Voice input",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun launchSpeechRecognizer(
    langCode: String,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val locale = when (langCode) {
        "zh" -> Locale.SIMPLIFIED_CHINESE
        "th" -> Locale.Builder().setLanguage("th").setRegion("TH").build()
        "lo" -> Locale.Builder().setLanguage("lo").setRegion("LA").build()
        "vi" -> Locale.Builder().setLanguage("vi").setRegion("VN").build()
        "id" -> Locale.Builder().setLanguage("id").setRegion("ID").build()
        else -> Locale.ENGLISH
    }

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    try {
        launcher.launch(intent)
    } catch (e: Exception) {
        // Speech recognizer not available on this device
    }
}
