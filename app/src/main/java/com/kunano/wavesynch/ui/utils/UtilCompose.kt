package com.kunano.wavesynch.ui.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kunano.wavesynch.R
import java.util.concurrent.Executors
import kotlin.math.min

@Composable
fun CustomDialogueCompose(
    title: String,
    text: String? = null,
    acceptButtonText: String = stringResource(R.string.yes),
    dismissButtonText: String = stringResource(R.string.cancel),
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    show: Boolean,
    content: (@Composable () -> Unit)? = null,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val titleStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(color = textColor)
    val textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor)

    val buttonTextStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
    if (show) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = onDismiss,
            title = { Text(title, style = titleStyle) },
            text = content ?: {
                if (text != null) {
                    Text(text, style = textStyle)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm()
                    onDismiss()
                }) {
                    Text(acceptButtonText, style = buttonTextStyle)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(dismissButtonText, style = buttonTextStyle)
                }
            }
        )
    }

}

@Composable
fun CustomToggleCompose(labelName: String, onChange: (state: Boolean) -> Unit) {
    var isOn by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(
            colors = SwitchDefaults.colors(checkedIconColor = MaterialTheme.colorScheme.primary),
            checked = isOn,
            onCheckedChange = {
                isOn = it
                onChange(it)
            }
        )
        Spacer(Modifier.width(8.dp))
        Text(labelName)




    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBottomSheetCompose(
    conetent: @Composable () -> Unit,
    showSheet: Boolean,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Sheet
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            conetent()
        }
    }


}

@Preview(showBackground = true)
@Composable
fun BottomSheetSamplePreview() {
    val textColor = MaterialTheme.colorScheme.onSurface
    val buttonColor = MaterialTheme.colorScheme.primary
    val textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor)
    val titleStyle: TextStyle =
        MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface)

    val contentModifier: Modifier = Modifier.fillMaxWidth()
    CustomBottomSheetCompose(conetent = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(R.string.update_room_name), style = titleStyle)
            Spacer(modifier = Modifier.height(20.dp))
            TextField(
                shape = MaterialTheme.shapes.medium,
                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.secondary),
                textStyle = textStyle,
                modifier = contentModifier,
                onValueChange = {},
                value = "",
                label = { Text(text = stringResource(R.string.new_room_name)) },
            )

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                onClick = { }) {
                Text(text = stringResource(R.string.update), style = textStyle)
            }
            Spacer(modifier = Modifier.height(20.dp))


        }
    }, showSheet = true, onDismiss = {})
}


@Composable
fun QrScanFrame(
    modifier: Modifier = Modifier,
    frameSizeRatio: Float = 0.7f,
    cornerLength: Dp = 24.dp,
    cornerStroke: Dp = 4.dp,
    cornerColor: Color = MaterialTheme.colorScheme.primary,
    overlayColor: Color = Color(0x88000000),
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val frameSize = min(canvasWidth, canvasHeight) * frameSizeRatio

            val left = (canvasWidth - frameSize) / 2f
            val top = (canvasHeight - frameSize) / 2f
            val right = left + frameSize
            val bottom = top + frameSize

            // Dark overlay
            drawRect(color = overlayColor)

            // Clear scan area
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(frameSize, frameSize),
                blendMode = BlendMode.Clear
            )

            val cl = cornerLength.toPx()
            val stroke = cornerStroke.toPx()

            // Corners
            fun drawCorner(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
                drawLine(
                    color = cornerColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = stroke
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(x1, y1),
                    end = Offset(x3, y3),
                    strokeWidth = stroke
                )
            }

            // TL
            drawCorner(left, top, left + cl, top, left, top + cl)
            // TR
            drawCorner(right, top, right - cl, top, right, top + cl)
            // BL
            drawCorner(left, bottom, left + cl, bottom, left, bottom - cl)
            // BR
            drawCorner(right, bottom, right - cl, bottom, right, bottom - cl)
        }
    }
}


@Composable
fun QrScannerScreen(
    onResult: (String) -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Show something while waiting for permission
    if (!hasCameraPermission) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Camera permission is required to scan QR codes")
        }
        return
    }

    var lastScanned by rememberSaveable { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {

        // 1) Camera feed
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onQrCodeScanned = { value ->
                // Avoid spamming: only react when value changes
                if (value != lastScanned) {
                    lastScanned = value
                    onResult(value)
                }
            }
        )

        // 2) Frame overlay
        QrScanFrame()

        // 3) Show last scanned text at bottom (debug)
        lastScanned?.let { text ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text, color = Color.White)
            }
        }
    }
}


@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = androidx.camera.core.Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val scanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                )

                analysis.setAnalyzer(
                    Executors.newSingleThreadExecutor()
                ) { imageProxy ->
                    processImage(scanner, imageProxy, onQrCodeScanned)
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

@SuppressLint("UnsafeOptInUsageError")
private fun processImage(
    scanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onQrCodeScanned: (String) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let {
                        onQrCodeScanned(it)
                    }
                }
            }
            .addOnFailureListener {
                // Handle failure
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}


@Preview(showBackground = true)
@Composable
fun QrScanFramePreview() {
    QrScanFrame()
}


@Preview(showBackground = true)
@Composable
fun CustomDialoguePreview() {
    CustomDialogueCompose(
        "Delete room",
        "Are you sure you want to delete this room?",
        onDismiss = {},
        onConfirm = {},
        show = true
    )
}
