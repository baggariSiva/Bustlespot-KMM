package org.softsuave.bustlespot.shared

import androidx.compose.ui.graphics.ImageBitmap

expect class SharedImage {
    fun toByteArray(): ByteArray?
}

expect fun toImageBitmap(byteArray: ByteArray): ImageBitmap