package com.nueng.translator.ui.qr

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrScreen(
    myUsername: String,
    onNavigateBack: () -> Unit,
    onNavigateToMyQr: (String) -> Unit,
    onQrScanned: (String) -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasCamPerm     by remember { mutableStateOf(false) }
    var statusMsg      by remember { mutableStateOf("Point camera at a NuengChat QR code") }

    // AtomicBoolean so background thread reads the latest value correctly
    val scanned = remember { AtomicBoolean(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamPerm = granted }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) hasCamPerm = true
        else permLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = { Text("Scan QR Code") },
                actions = {
                    IconButton(onClick = { onNavigateToMyQr(myUsername) }) {
                        Icon(Icons.Default.QrCode, "My QR Code",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (!hasCamPerm) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("Camera permission is required to scan QR codes.",
                        textAlign = TextAlign.Center, fontSize = 15.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            } else {
                // ── Camera preview (full screen) ──────────────────────────
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                        val executor = Executors.newSingleThreadExecutor()

                        // ZXing reader with hints for speed
                        val reader = MultiFormatReader().apply {
                            setHints(mapOf(
                                DecodeHintType.POSSIBLE_FORMATS to
                                    listOf(com.google.zxing.BarcodeFormat.QR_CODE),
                                DecodeHintType.TRY_HARDER to true
                            ))
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                .build()

                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                if (!scanned.get()) {
                                    decodeQr(imageProxy, reader) { result ->
                                        if (result != null && scanned.compareAndSet(false, true)) {
                                            // Parse nuengtranslator://user/{username}
                                            val username = when {
                                                result.startsWith("nuengtranslator://user/") ->
                                                    result.removePrefix("nuengtranslator://user/").trim()
                                                else -> ""
                                            }
                                            if (username.isNotBlank()) {
                                                // Must call on main thread
                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    onQrScanned(username)
                                                }
                                            } else {
                                                // Not a NuengChat QR — reset so user can try again
                                                scanned.set(false)
                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    statusMsg = "Not a NuengChat QR code. Try again."
                                                }
                                            }
                                        }
                                    }
                                }
                                imageProxy.close()
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("ScanQr", "Camera bind error: ${e.message}")
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )

                // ── Scan frame overlay ────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                )

                // Corner decorations
                val cornerColor = MaterialTheme.colorScheme.primary
                val cornerSize  = 24.dp
                val cornerWidth = 4.dp
                // Top-left
                Box(Modifier.size(260.dp), contentAlignment = Alignment.TopStart) {
                    Box(Modifier.size(cornerSize).border(width = cornerWidth, color = cornerColor,
                        shape = RoundedCornerShape(topStart = 16.dp)))
                }
                // Top-right
                Box(Modifier.size(260.dp), contentAlignment = Alignment.TopEnd) {
                    Box(Modifier.size(cornerSize).border(width = cornerWidth, color = cornerColor,
                        shape = RoundedCornerShape(topEnd = 16.dp)))
                }
                // Bottom-left
                Box(Modifier.size(260.dp), contentAlignment = Alignment.BottomStart) {
                    Box(Modifier.size(cornerSize).border(width = cornerWidth, color = cornerColor,
                        shape = RoundedCornerShape(bottomStart = 16.dp)))
                }
                // Bottom-right
                Box(Modifier.size(260.dp), contentAlignment = Alignment.BottomEnd) {
                    Box(Modifier.size(cornerSize).border(width = cornerWidth, color = cornerColor,
                        shape = RoundedCornerShape(bottomEnd = 16.dp)))
                }

                // ── Status text ───────────────────────────────────────────
                Text(
                    text      = statusMsg,
                    fontSize  = 13.sp,
                    color     = Color.White,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Tries to decode a QR code from the image proxy.
 * Handles rotation by trying the image as-is (CameraX already handles rotation metadata).
 * Calls [onResult] with the decoded string, or null if nothing found.
 */
private fun decodeQr(
    imageProxy: ImageProxy,
    reader: MultiFormatReader,
    onResult: (String?) -> Unit
) {
    try {
        val buffer = imageProxy.planes[0].buffer.also { it.rewind() }
        val bytes  = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val width  = imageProxy.width
        val height = imageProxy.height

        val source = PlanarYUVLuminanceSource(
            bytes, width, height,
            0, 0, width, height, false
        )
        val bmp    = BinaryBitmap(HybridBinarizer(source))

        try {
            val result = reader.decodeWithState(bmp)
            onResult(result.text)
        } catch (_: NotFoundException) {
            onResult(null)
        } finally {
            reader.reset()
        }
    } catch (e: Exception) {
        Log.w("ScanQr", "Decode error: ${e.message}")
        onResult(null)
    }
}
