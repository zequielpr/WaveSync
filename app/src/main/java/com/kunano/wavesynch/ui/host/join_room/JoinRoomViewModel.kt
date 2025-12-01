package com.kunano.wavesynch.ui.host.join_room

import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.kunano.wavesynch.domain.usecase.room_use_cases.ProcessImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class JoinRoomViewModel @Inject constructor(
    private val processImageUseCase: ProcessImageUseCase
) : ViewModel() {

    fun processImage(
        scanner: BarcodeScanner,
        imageProxy: ImageProxy,
        onQrCodeScanned: (String) -> Unit
    ) {
        processImageUseCase(scanner, imageProxy, onQrCodeScanned)
    }

    fun joinSoundRoom(hostData: String){
        Log.d("JoinRoomViewModel", "joinSoundRoom with data: $hostData")
    }

}





