package org.softsuave.bustlespot.utils

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import coil3.BitmapImage
import org.softsuave.bustlespot.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Base64


actual fun isAndroid(): Boolean = true

@Composable
actual fun handleBackPress(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}

actual fun convertImageBitmapToBase64(image: ImageBitmap): String? {
    val bitmap: Bitmap = image.asAndroidBitmap()

    val tempFile = File.createTempFile("sampleFile", ".png")

    FileOutputStream(tempFile).use { fileOutputStream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 30, fileOutputStream)
    }

    ByteArrayOutputStream().use { byteArrayOutputStream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 30, byteArrayOutputStream)
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
    }
}