package org.softsuave.bustlespot.locationmodule

import dev.jordond.compass.Coordinates
import dev.jordond.compass.Location
import dev.jordond.compass.Priority
import dev.jordond.compass.geolocation.LocationRequest
import dev.jordond.compass.geolocation.Locator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class MGeoLocator {
    actual fun getLocator(): Locator? {
        return DLocater()
    }
}

class DLocater(
    override val locationUpdates: Flow<Location> = flow {
    }
) : Locator {
    override fun isAvailable(): Boolean {
        return false
    }

    override suspend fun current(priority: Priority): Location {
        return Location(
            coordinates = Coordinates(0.0, 0.0),
            accuracy = 0.0, timestampMillis = 0L,
            azimuth = null,
            speed = null,
            altitude = null
        )
    }

    override suspend fun track(request: LocationRequest): Flow<Location> {
        return locationUpdates
    }

    override fun stopTracking() {

    }
}