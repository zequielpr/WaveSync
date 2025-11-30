package com.kunano.wavesynch.ui.host.join_room

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.kunano.wavesynch.R
import com.kunano.wavesynch.ui.utils.QrScanFrame
import kotlin.math.min





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRoomViewCompose(viewModel: JoinRoomViewModel = hiltViewModel(), onBack: () -> Unit, navigateToCurrentRoom: () -> Unit){
    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Image(
                        painter = painterResource(id = R.drawable.arrow_back_ios_48px),
                        contentDescription = "Back"
                    )
                }
            },
            title = {})
    }) { it ->
        Box(modifier = Modifier.padding(it)){
            QrScannerScreen(onResult = { hostData ->
                viewModel.joinSoundRoom(hostData)
            })
        }
    }
}





@Preview(showBackground = true)
@Composable
fun QrScannerScreenPreview() {

}




