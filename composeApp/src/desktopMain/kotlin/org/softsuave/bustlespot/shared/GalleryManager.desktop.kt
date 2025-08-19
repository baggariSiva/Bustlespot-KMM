package org.softsuave.bustlespot.shared

import androidx.compose.runtime.Composable

@Composable
actual fun rememberGalleryManager(onResult: (SharedImage?) -> Unit): GalleryManager {
    TODO("Not yet implemented")
}
actual class GalleryManager actual constructor(private val onLaunch: () -> Unit) {
    actual fun launch() {
        onLaunch()
    }
}