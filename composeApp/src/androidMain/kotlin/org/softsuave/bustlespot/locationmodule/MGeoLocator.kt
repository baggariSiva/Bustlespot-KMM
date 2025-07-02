package org.softsuave.bustlespot.locationmodule

import dev.jordond.compass.geolocation.Locator
import dev.jordond.compass.geolocation.mobile.mobile

actual class MGeoLocator {
    actual fun getLocator(): Locator {
        return Locator.mobile() // or Locator.mobile() if browser not supported
    }
}