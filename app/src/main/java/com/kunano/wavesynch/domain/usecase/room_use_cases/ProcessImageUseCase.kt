package com.kunano.wavesynch.domain.usecase.room_use_cases

import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

@SuppressLint("UnsafeOptInUsageError")
class ProcessImageUseCase @Inject constructor() {
    operator fun invoke(barcodeScanner: BarcodeScanner,
               imageProxy: ImageProxy,
               onQrCodeScanned: (String) -> Unit){
            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return
            }

            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes
                        .firstOrNull { it.format == Barcode.FORMAT_QR_CODE && it.rawValue != null }
                        ?.rawValue
                        ?.let { onQrCodeScanned(it) }
                }
                .addOnFailureListener {
                    // Ignore or log
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }

}