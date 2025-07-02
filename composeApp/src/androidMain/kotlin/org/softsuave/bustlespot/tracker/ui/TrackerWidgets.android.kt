package org.softsuave.bustlespot.tracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.MapOptions
import com.tomtom.sdk.map.display.annotation.ExperimentalInitialCameraOptionsApi
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.camera.InitialCameraOptions
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import org.softsuave.bustlespot.R


@OptIn(ExperimentalInitialCameraOptionsApi::class)
@Composable
actual fun TomTomMap(
    centerCoordinate: Coordinate,
    modifier: Modifier,
    onMarkerClick: (Coordinate) -> Unit
) {
    val context = LocalContext.current
    val mapOptions = MapOptions(
        mapKey = "8KZB8zTu2dRMVPN08mwXYNo7JztLRa6N",
        initialCameraOptions = InitialCameraOptions(
            position = GeoPoint(centerCoordinate.latitude, centerCoordinate.longitude),
            zoom = 10.0
        )
    )
    val mapFragment = remember {
        MapFragment.newInstance(mapOptions).apply {
            getMapAsync { tomTomMap ->
                val markerOptions = MarkerOptions(
                    coordinate = GeoPoint(centerCoordinate.latitude, centerCoordinate.longitude),
                    pinImage = ImageFactory.fromResource(R.drawable.ic_launcher_foreground) // Replace with actual resource
                )
                tomTomMap.addMarker(markerOptions)

                tomTomMap.addMarkerClickListener { marker ->
                    onMarkerClick(
                        Coordinate(
                            latitude = marker.coordinate.latitude,
                            longitude = marker.coordinate.longitude
                        )
                    )
                }
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            (context as FragmentActivity).supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, mapFragment)
                .commitNow()
            mapFragment.requireView()
        }
    )

    // Cleanup fragment on dispose
    DisposableEffect(Unit) {
        onDispose {
            (context as? FragmentActivity)?.supportFragmentManager?.beginTransaction()
                ?.remove(mapFragment)
                ?.commitAllowingStateLoss()
        }
    }
}
