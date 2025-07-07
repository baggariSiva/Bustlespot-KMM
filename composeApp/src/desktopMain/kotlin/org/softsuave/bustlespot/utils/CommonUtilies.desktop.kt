package org.softsuave.bustlespot.utils

import androidx.compose.runtime.Composable

actual fun isAndroid(): Boolean = false



@Composable
actual fun handleBackPress(onBack: () -> Unit) {
}

actual fun bitmapToByteArray(bitmap: Any): ByteArray? {
  return null
}