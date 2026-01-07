package com.kunano.wavesynch.ui.utils

import PermissionHandler
import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.kunano.wavesynch.R
import com.kunano.wavesynch.ui.theme.AppDimens
import com.kunano.wavesynch.ui.theme.OverlayColor
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
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.surface,
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
    overlayColor: Color = OverlayColor,
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

            fun drawCornerRounded(
                xA: Float,
                yA: Float,
                xCorner: Float,
                yCorner: Float,
                xB: Float,
                yB: Float,
            ) {
                val path = Path().apply {
                    moveTo(xA, yA)
                    lineTo(xCorner, yCorner)
                    lineTo(xB, yB)
                }

                drawPath(
                    path = path,
                    color = cornerColor,
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // TL (from right -> corner -> down)
            drawCornerRounded(
                xA = left + cl, yA = top,
                xCorner = left, yCorner = top,
                xB = left, yB = top + cl
            )

            // TR (from left -> corner -> down)
            drawCornerRounded(
                xA = right - cl, yA = top,
                xCorner = right, yCorner = top,
                xB = right, yB = top + cl
            )

            // BL (from right -> corner -> up)
            drawCornerRounded(
                xA = left + cl, yA = bottom,
                xCorner = left, yCorner = bottom,
                xB = left, yB = bottom - cl
            )

            // BR (from left -> corner -> up)
            drawCornerRounded(
                xA = right - cl, yA = bottom,
                xCorner = right, yCorner = bottom,
                xB = right, yB = bottom - cl
            )

        }
    }
}


@Composable
fun QrScannerScreen(
    onResult: (String) -> Unit,
    navigateBack: () -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }

    var relaunchPermissionHandler by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showPermissionSettings by remember { mutableStateOf(false) }
    var permissionPermanentlyDenied by remember { mutableStateOf(false) }


    val required: List<String> = buildList {
        add(Manifest.permission.CAMERA)
    } // show UI like "We need this because..."

    val permissionRequiredMessage = stringResource(R.string.permission_required_message)





    if (!hasCameraPermission) {
        Log.d("MainScreen", "Launching permission handler")
        PermissionHandler(
            required = required,
            onAllGranted = {
                Log.d("MainScreen", "Permission granted")
                hasCameraPermission = true
            },
            onNeedUserActionInSettings = {
                Log.d("MainScreen", "User needs to go to settings")
                showPermissionSettings = true
                permissionPermanentlyDenied = true
            },
            onShowRationale = {
                showPermissionRationale = true
                Log.d("MainScreen", "Showing permission rationale")
            }
        )
    }

    if (relaunchPermissionHandler) {
        PermissionHandler(
            required = required,
            onAllGranted = {
                Log.d("MainScreen", "Permission granted")
                hasCameraPermission = true
            },
            onNeedUserActionInSettings = {
                showPermissionSettings = true
                permissionPermanentlyDenied = true
            },
            onShowRationale = { showPermissionRationale = true }
        )
    }


    if (showPermissionRationale) {
        PermissionRationaleDialog(message = permissionRequiredMessage, onRetry = {
            relaunchPermissionHandler = true
            showPermissionRationale = false
        }, onCancel = { showPermissionRationale = false
        navigateBack()})

    } else if (showPermissionSettings) {
        GoToSettingsDialog(onOpenSettings = {
            openAppSettings(context = context)
            showPermissionSettings = false
        }, onCancel = { showPermissionSettings = false })

    }

    // Show something while waiting for permission
    if (!hasCameraPermission) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (permissionPermanentlyDenied) {
                Box(
                    Modifier.padding(horizontal = AppDimens.Padding.horizontal),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.permission_required_message_go_setting),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { openAppSettings(context = context) }) {
                    Text(stringResource(R.string.open_settings))
                }
            }

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


fun generateQrBitmap(
    content: String,
    size: Int = 600,
    bgColor: Color = Color.White,
    fgColor: Color = Color.Black,
): Bitmap {

    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)

    val bmp = createBitmap(size, size, Bitmap.Config.RGB_565)

    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp[x, y] =
                if (bitMatrix[x, y]) fgColor.toArgb() else bgColor.toArgb()
        }
    }

    return bmp
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


@Composable
fun BlockingLoadingOverlay(
    isLoading: Boolean,
    message: String? = null,
    modifier: Modifier = Modifier,
) {
    if (!isLoading) return

    // Full-screen overlay that blocks interactions behind it
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            // clickable with no indication consumes all pointer input
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* consume clicks */ }
            .semantics { contentDescription = "Loading" },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .height(60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center

            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                if (message != null) {
                    Spacer(Modifier.weight(0.3f))
                    Text(
                        textAlign = TextAlign.Center,
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}


@Composable
fun PermissionRationaleDialog(
    title: String = stringResource(R.string.permission_required),
    message: String = stringResource(R.string.permissions_required_message),
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.primary
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        title = { androidx.compose.material3.Text(title) },
        text = { androidx.compose.material3.Text(message) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onRetry) {
                androidx.compose.material3.Text(stringResource(R.string.retry), color = textColor)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onCancel) {
                androidx.compose.material3.Text(stringResource(R.string.cancel), color = textColor)
            }
        }
    )
}


@Composable
fun GoToSettingsDialog(
    title: String = stringResource(R.string.enable_permissions),
    message: String = stringResource(R.string.permission_required_message_go_setting),
    onOpenSettings: () -> Unit,
    onCancel: () -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.primary
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        title = { androidx.compose.material3.Text(title) },
        text = { androidx.compose.material3.Text(message) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onOpenSettings) {
                androidx.compose.material3.Text(
                    stringResource(R.string.open_settings),
                    color = textColor
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onCancel) {
                androidx.compose.material3.Text(stringResource(R.string.cancel), color = textColor)
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun BlockingLoadingOverlayPreview() {
    BlockingLoadingOverlay(isLoading = true, message = "Loading...")
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
