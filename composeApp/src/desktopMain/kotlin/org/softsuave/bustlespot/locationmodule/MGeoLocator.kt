package org.softsuave.bustlespot.locationmodule

import dev.jordond.compass.geolocation.Locator

actual class MGeoLocator {
    actual fun getLocator(): Locator? {
        return null // or Locator.mobile() if browser not supported
    }
}