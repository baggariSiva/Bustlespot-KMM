package org.softsuave.bustlespot.shared

import androidx.compose.runtime.Composable

@Composable
actual fun createPermissionsManager(callback: PermissionCallback): PermissionsManager {
    TODO("Not yet implemented")
}


actual class PermissionsManager actual constructor(private val callback: PermissionCallback) :
    PermissionHandler {
    @Composable
    override fun askPermission(permission: PermissionType) {
        when (permission) {
            PermissionType.CAMERA -> {

            }

            PermissionType.GALLERY -> {

            }

        }
    }

    private fun askCameraPermission(
     permission: PermissionType, callback: PermissionCallback
    ) {

    }

    private fun askGalleryPermission(
      permission: PermissionType, callback: PermissionCallback
    ) {

    }

    @Composable
    override fun isPermissionGranted(permission: PermissionType): Boolean {
        return when (permission) {
            PermissionType.CAMERA -> {false}
            PermissionType.GALLERY -> {false}
        }
    }

    @Composable
    override fun launchSettings() {

    }

}