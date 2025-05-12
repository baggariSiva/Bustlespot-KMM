package org.softsuave.bustlespot.locationmodule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jordond.compass.Priority
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.GeolocatorResult
import dev.jordond.compass.geolocation.LocationRequest
import dev.jordond.compass.geolocation.TrackingStatus
import dev.jordond.compass.geolocation.mobile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationViewModel() : ViewModel() {
    private val geolocator: Geolocator = Geolocator.mobile()
    private val initalGeocode: Geocode = Geocode(0.0, 0.0, "")
    private var geoFenceThreshold: Double = 0.0
    private var geoFenceManager: GeoFenceManager =
        GeoFenceManager(GeoFenceShape.Circle(initalGeocode, geoFenceThreshold))
    private val _locationInfo = MutableStateFlow("Press the button to get location")
    val locationInfo: StateFlow<String> = _locationInfo

    private val _distanceInfo = MutableStateFlow("Press the button to get location")
    val distanceInfo: StateFlow<String> = _distanceInfo

    private val _geoFenceInfo = MutableStateFlow("IDLE")
    val geoFenceInfo: StateFlow<String> = _geoFenceInfo

    private val _sliderValue = MutableStateFlow(0)
    val sliderValue: StateFlow<Int> = _sliderValue

    private val _geoFenceRunning = MutableStateFlow(false)
    val geoFenceRunning: StateFlow<Boolean> = _geoFenceRunning

    private val trackingStatus = MutableStateFlow<TrackingStatus>(TrackingStatus.Idle)

    init {
        viewModelScope.launch {
            geolocator.trackingStatus.collectLatest { status ->
                trackingStatus.value = status
                when (status) {
                    is TrackingStatus.Tracking -> _locationInfo.value = "Tracking started"
                    is TrackingStatus.Error -> _locationInfo.value = "Error: ${status.cause}"
                    is TrackingStatus.Idle -> _locationInfo.value = "Tracking stopped"
                    is TrackingStatus.Update -> {
                        val location = status.location.coordinates
                        setInitialGeocodeAndUpdateDistance(location.latitude, location.longitude)
                        if (geoFenceRunning.value) {
                            checkGeoFence(Geocode(location.latitude, location.longitude, ""))
                        }
                        _locationInfo.value =
                            "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                    }
                }
            }
        }
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            when (val result = geolocator.current()) {
                is GeolocatorResult.Success -> {
                    val location = result.data.coordinates
//                    setInitialGeocodeAndUpdateDistance(location.latitude, location.longitude)
                    _locationInfo.value =
                        "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                }

                is GeolocatorResult.Error -> {
                    _locationInfo.value = when (result) {
                        is GeolocatorResult.NotSupported -> "Geolocation not supported"
                        is GeolocatorResult.NotFound -> "Location not found"
                        is GeolocatorResult.PermissionError -> "Permission denied"
                        is GeolocatorResult.GeolocationFailed -> "Failed: ${result.message}"
                        else -> "Error"
                    }
                }
            }
        }
    }

    private fun setInitialGeocodeAndUpdateDistance(lat: Double, long: Double) {
        if (initalGeocode.lat == 0.0 && initalGeocode.long == 0.0) {
            initalGeocode.lat = lat
            initalGeocode.long = long
        } else {
            _distanceInfo.update {
                "Distance: ${distanceBetween(initalGeocode, Geocode(lat, long, ""))} meters"
            }
        }
    }

    fun startTracking() {
        viewModelScope.launch {
            geolocator.startTracking(
                LocationRequest(
                    interval = 1000L,
                    priority = Priority.HighAccuracy
                )
            )
        }
    }

    fun stopTracking() {
        viewModelScope.launch {
            geolocator.stopTracking()
        }
    }

    fun startGeoFence(
        shapeEnum: GeoFenceShapeEnum,
        thresholdMeters: Double = 10.0
    ) {
        val shape = when (shapeEnum) {
            GeoFenceShapeEnum.CIRCLE -> GeoFenceShape.Circle(initalGeocode, thresholdMeters)
            GeoFenceShapeEnum.SQUARE -> GeoFenceShape.Square(initalGeocode, thresholdMeters)
            GeoFenceShapeEnum.RECTANGLE -> GeoFenceShape.Rectangle(null, null)
        }
        geoFenceManager = GeoFenceManager(shape, thresholdMeters)
        _geoFenceRunning.value = true
    }
    private var showStatus = "IDLE"
    private fun checkGeoFence(current: Geocode) {
        viewModelScope.launch {
            val currentStatus = geoFenceManager.check(current)
            if(currentStatus == GeoFenceEnums.ENTER){
                showStatus = currentStatus.toString()
            }
            if (currentStatus == GeoFenceEnums.EXIT){
                showStatus = currentStatus.toString()
            }
            _geoFenceInfo.update { showStatus }
        }
    }

    fun stopGeoFence() {
        _geoFenceRunning.value = false
    }

    fun setGeoFenceThreshold(value: Float) {
        geoFenceThreshold = value.toDouble()
    }

    fun resume() {
        viewModelScope.launch {
            if (trackingStatus.value !is TrackingStatus.Tracking) {
                geolocator.startTracking(
                    LocationRequest(
                        interval = 1000L,
                        priority = Priority.HighAccuracy
                    )
                )
            }
        }
    }

}
