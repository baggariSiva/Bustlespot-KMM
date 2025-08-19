package org.softsuave.bustlespot
class JVMPlatform: Platform {
    override val platformType: PlatFormType
        get() = PlatFormType.DESKTOP
}
actual fun getPlatform(): Platform = JVMPlatform()