package org.softsuave.bustlespot

import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import org.softsuave.bustlespot.screenshot.ComponentActivityReference
import org.softsuave.bustlespot.ui.MediaProjectionService

class MainActivity : ComponentActivity() {
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("MainActivity Screen capture permission granted. Starting service...")
            ProjectionData.resultCode = result.resultCode
            ProjectionData.data = result.data
        } else {
            println("Screen capture permission denied")
        }
    }
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                requestScreenCapturePermission()
                Toast.makeText(this, "Notification Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window?.statusBarColor = Color.Gray.toArgb()
        setContent {
            App()
        }
        ComponentActivityReference.setActivity(this)
        checkAndRequestNotificationPermission()
    }

    private fun requestScreenCapturePermission() {
        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()

        screenCaptureLauncher.launch(captureIntent)
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            requestScreenCapturePermission()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ComponentActivityReference.clear()
        val serviceIntent = Intent(this, MediaProjectionService::class.java)
        stopService(serviceIntent)
    }

    object ProjectionData{
        var resultCode: Int = 0
        var data: Intent? = null
    }


}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}