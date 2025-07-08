package org.softsuave.bustlespot.shared

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.IllegalArgumentException

actual fun toImageBitmap(byteArray: ByteArray): ImageBitmap {
    TODO("Not yet implemented")
}


actual class SharedImage() {
    actual fun toByteArray(): ByteArray? {
        return null
    }
}
