package org.softsuave.bustlespot.tracker.ui

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.MapOptions
import com.tomtom.sdk.map.display.annotation.ExperimentalInitialCameraOptionsApi
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import org.softsuave.bustlespot.R
import org.softsuave.bustlespot.TOM_TOM_MAP_KEY


@OptIn(ExperimentalInitialCameraOptionsApi::class)
@Composable
actual fun MapViewMobile(
    centerCoordinate: Coordinate,
    modifier: Modifier,
    onMarkerClick: (Coordinate) -> Unit
) {
    val context = LocalContext.current
    val fragmentActivity = context as FragmentActivity
    val containerId = remember { View.generateViewId() }

    // Always create the Android view
    AndroidView(
        modifier = modifier,
        factory = {
            FragmentContainerView(it).apply {
                id = containerId
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    )

    // This effect will re-run if centerCoordinate changes
    LaunchedEffect(centerCoordinate) {
        val mapOptions = MapOptions(
            mapKey = TOM_TOM_MAP_KEY,
            cameraOptions = CameraOptions(
                position = GeoPoint(
                    latitude = centerCoordinate.latitude,
                    longitude = centerCoordinate.longitude
                ),
                zoom = 13.0,
            )
        )

        var mapFragment =
            fragmentActivity.supportFragmentManager.findFragmentById(containerId) as? MapFragment

        // Add fragment only once
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance(mapOptions)
            fragmentActivity.supportFragmentManager.beginTransaction()
                .replace(containerId, mapFragment)
                .commit()
        }

        mapFragment.getMapAsync { tomTomMap ->
            // Update camera position every time centerCoordinate changes

            tomTomMap.moveCamera(
                CameraOptions(
                    position = GeoPoint(
                        centerCoordinate.latitude,
                        centerCoordinate.longitude
                    ), zoom = 13.0
                )
            )

            // Optionally clear existing markers if needed
            tomTomMap.clear()

            // Add new marker
            val markerOptions = MarkerOptions(
                coordinate = GeoPoint(centerCoordinate.latitude, centerCoordinate.longitude),
                pinImage = ImageFactory.fromResource(R.drawable.placeholder_removebg_preview)
            )
            tomTomMap.addMarker(markerOptions)

            tomTomMap.addMarkerClickListener { marker ->
                onMarkerClick(Coordinate(marker.coordinate.latitude, marker.coordinate.longitude))
            }
        }
    }
}