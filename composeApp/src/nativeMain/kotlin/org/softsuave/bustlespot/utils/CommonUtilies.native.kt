package org.softsuave.bustlespot.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import org.jetbrains.skia.Bitmap
import org.jetbrains.skiko.ExperimentalSkikoApi
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation


actual fun isAndroid(): Boolean = false

@Composable
actual fun handleBackPress(onBack: () -> Unit) {
}

actual fun convertImageBitmapToBase64(image: ImageBitmap): String? {
    return null
}

@OptIn(ExperimentalForeignApi::class)
@ExperimentalSkikoApi
actual fun bitmapToByteArray(bitmap: Any): ByteArray? {
    println("bitmapToByteArray called with type: ${bitmap::class.simpleName}")
    return try {
        when (bitmap) {
            is UIImage -> {
                val nsData = UIImageJPEGRepresentation(bitmap, 1.0)
                nsData?.toByteArray()
            }
            is ImageBitmap -> {
                val skiaBitmap = bitmap.asSkiaBitmap()
                print("Skia image bitmap imageinfo ${skiaBitmap.imageInfo})")
                val nsData = skiaBitmap.readPixels()

                println("ImageBitmap size: ${bitmap.width}x${bitmap.height} (${bitmap.config})")

                nsData?.toNSData()?.toByteArray()
            }
            else -> {
                println("Unsupported bitmap type: ${bitmap::class.simpleName}")
                null
            }
        }
    } catch (e: Exception) {
        println("Error in bitmapToByteArray: ${e.message}")
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalForeignApi::class)
fun Bitmap.toUIImage(): UIImage? {
    val nsData = this.readPixels()?.toNSData() ?: return null
    return UIImage(data = nsData)
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData {
    memScoped {
        return NSData.create(
            bytes = allocArrayOf(this@toNSData),
            length = this@toNSData.size.toULong()
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray? {
    val length = this.length.toInt()
    return this.bytes?.readBytes(length)
}
