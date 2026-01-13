import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun PermissionHandler(
    onAllGranted: () -> Unit,
    onNeedUserActionInSettings: () -> Unit, // show UI like "Go to settings"
    onShowRationale: () -> Unit,
    required: List<String> = buildList {
        // Camera
        add(Manifest.permission.ACCESS_FINE_LOCATION)

        // Notifications only needed on 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            add(Manifest.permission.RECORD_AUDIO)

        }
    } // show UI like "We need this because..."
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    fun hasAllPermissions(): Boolean =
        required.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = required.all { results[it] == true }
        if (allGranted) {
            onAllGranted()
        } else {
            // Decide whether we can ask again or must send to Settings
            val canAskAgain = activity != null && required.any { perm ->
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
            }

            if (canAskAgain) onShowRationale() else onNeedUserActionInSettings()
        }
    }

    LaunchedEffect(Unit) {
        if (hasAllPermissions()) onAllGranted()
        else launcher.launch(required.toTypedArray())
    }
}
