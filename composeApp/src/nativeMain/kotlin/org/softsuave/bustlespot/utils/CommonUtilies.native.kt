package org.softsuave.bustlespot.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.Foundation.NSData

actual fun isAndroid(): Boolean = false

@Composable
actual fun handleBackPress(onBack: () -> Unit) {
}

actual fun bitmapToByteArray(bitmap: Any): ByteArray? {
    return try {
        if (bitmap is UIImage) {
            val data = UIImagePNGRepresentation(bitmap)
            data?.toByteArray()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

// Helper to convert NSData to ByteArray
fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val bytes = ByteArray(length)
    this.getBytes(bytes.refTo(0), length.toULong())
    return bytes
}