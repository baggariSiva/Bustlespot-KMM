package org.softsuave.bustlespot.locationmodule

import androidx.lifecycle.viewModelScope
import dev.jordond.compass.Priority
import dev.jordond.compass.geolocation.LocationRequest
import dev.jordond.compass.geolocation.TrackingStatus
import kotlinx.coroutines.launch
import kotlin.math.cos

class GeoFenceManager(
    private val shape: GeoFenceShape,
    private val thresholdMeters: Double = 10.0
) {
    private var previousState: Boolean? = null

    fun check(current: Geocode): GeoFenceEnums {
        val isInside = when (shape) {
            is GeoFenceShape.Circle -> {
                val distance = shape.center?.let { distanceBetween(it, current) }
                distance!! <  thresholdMeters
            }

            is GeoFenceShape.Square -> {
                val halfSide = shape.sideLengthMeters / 2
                val latDelta = metersToLatitudeDelta(halfSide)
                val lonDelta = metersToLongitudeDelta(halfSide, shape.center!!.lat)

                val inLat = current.lat in ((shape.center?.lat ?:0.0 ) - latDelta)..(shape.center!!.lat + latDelta)
                val inLon = current.long in ((shape.center?.long ?: 0.0) - lonDelta)..(shape.center!!.long + lonDelta)
                inLat && inLon
            }

            is GeoFenceShape.Rectangle -> {
                val inLat = current.lat in (shape.bottomRight?.lat ?: 0.0)..(shape.topLeft?.lat ?: 1.0)
                val inLon = current.long in (shape.topLeft?.long ?: 0.0)..shape.bottomRight!!.long
                inLat && inLon
            }
        }

        return when {
            previousState == null -> {
                previousState = isInside
                GeoFenceEnums.IDLE
            }
            previousState == false && isInside -> {
                previousState = true
                GeoFenceEnums.ENTER
            }
            previousState == true && !isInside -> {
                previousState = false
                GeoFenceEnums.EXIT
            }
            else -> GeoFenceEnums.DWELL
        }
    }

    private fun metersToLatitudeDelta(meters: Double): Double {
        val metersPerDegree = 111_000.0
        return meters / metersPerDegree
    }

    private fun metersToLongitudeDelta(meters: Double, latitude: Double): Double {
        val metersPerDegree = 111_000.0 * cos(latitude.toRadians())
        return meters / metersPerDegree
    }
}


