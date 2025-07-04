package org.softsuave.bustlespot.tracker.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.softsuave.bustlespot.Log
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapViewMobile(
    centerCoordinate: Coordinate,
    modifier: Modifier,
    onMarkerClick: (Coordinate) -> Unit
) {
    Log.d("MapViewMobile Updating MapView with centerCoordinate: $centerCoordinate")

    // Delegate must persist across recompositions
    val delegateRef = remember {
        object : NSObject(), MKMapViewDelegateProtocol {
            override fun mapView(
                mapView: MKMapView,
                didSelectAnnotationView: MKAnnotationView
            ) {
                val annotation = didSelectAnnotationView.annotation()
                if (annotation is MKPointAnnotation) {
                    annotation.coordinate().useContents {
                        onMarkerClick(Coordinate(latitude, longitude))
                    }
                }
            }
        }
    }

    // UIKitView with a dynamic update block
    UIKitView(
        factory = {
            MKMapView().apply {
                delegate = delegateRef

                // Set initial region
                setRegion(
                    MKCoordinateRegionMakeWithDistance(
                        centerCoordinate = CLLocationCoordinate2DMake(
                            centerCoordinate.latitude,
                            centerCoordinate.longitude
                        ),
                        latitudinalMeters = 1000.0,
                        longitudinalMeters = 1000.0,
                    ),
                    animated = false
                )

                // Initial marker
                val annotation = MKPointAnnotation(
                    coordinate = CLLocationCoordinate2DMake(
                        centerCoordinate.latitude,
                        centerCoordinate.longitude
                    )
                )
                addAnnotation(annotation)
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { mapView ->
            Log.d("MapViewMobile Updating map region to: $centerCoordinate")

            val newCoordinate = CLLocationCoordinate2DMake(
                centerCoordinate.latitude,
                centerCoordinate.longitude
            )

            // Update map region
            mapView.setRegion(
                MKCoordinateRegionMake(
                    centerCoordinate = newCoordinate,
                    span = MKCoordinateSpanMake(0.01, 0.01)
                ),
                animated = true
            )

            // Clear previous annotations
            mapView.removeAnnotations(mapView.annotations)

            // Add new annotation
            val annotation = MKPointAnnotation(newCoordinate)
            mapView.addAnnotation(annotation)
        }
    )
}
