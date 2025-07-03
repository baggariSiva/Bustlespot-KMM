package org.softsuave.bustlespot

class AOSPlatform: Platform {
    override val platformType: PlatFormType
        get() = PlatFormType.ANDROID
}
actual fun getPlatform(): Platform = AOSPlatform()