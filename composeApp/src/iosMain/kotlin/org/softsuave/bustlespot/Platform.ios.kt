package org.softsuave.bustlespot

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val platformType: PlatFormType
        get() = PlatFormType.IOS
}

actual fun getPlatform(): Platform = IOSPlatform()