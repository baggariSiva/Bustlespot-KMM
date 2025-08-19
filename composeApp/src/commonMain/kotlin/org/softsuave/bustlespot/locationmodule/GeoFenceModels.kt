package org.softsuave.bustlespot.locationmodule

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class GeoFenceEnums{
    ENTER, EXIT, IDLE,NULL,DWELL
}

data class Geocode(
    var lat:Double,
    var long:Double,
    var address:String?
)
sealed class GeoFenceShape {
    data class Circle(val center: Geocode?, val radiusMeters: Double) : GeoFenceShape()
    data class Square(val center: Geocode?, val sideLengthMeters: Double) : GeoFenceShape()

    data class Rectangle(val topLeft: Geocode?, val bottomRight: Geocode?) : GeoFenceShape()
}
enum class GeoFenceShapeEnum{
    CIRCLE, SQUARE, RECTANGLE
}


fun Double.toRadians(): Double = this * PI / 180.0

fun distanceBetween(a: Geocode, b: Geocode): Double {
    val earthRadius = 6371000.0 // meters

    val dLat = (b.lat - a.lat).toRadians()
    val dLon = (b.long - a.long).toRadians()

    val lat1 = a.lat.toRadians()
    val lat2 = b.lat.toRadians()

    val haversine = sin(dLat / 2).pow(2.0) +
            sin(dLon / 2).pow(2.0) * cos(lat1) * cos(lat2)

    val c = 2 * atan2(sqrt(haversine), sqrt(1 - haversine))

    return earthRadius * c // distance in meters
}