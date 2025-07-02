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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window?.statusBarColor = Color.Gray.toArgb()
        setContent {
            App()
        }
        ComponentActivityReference.setActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ComponentActivityReference.clear()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}