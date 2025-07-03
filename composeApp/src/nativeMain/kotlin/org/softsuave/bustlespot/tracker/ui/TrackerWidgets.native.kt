package org.softsuave.bustlespot.tracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.darwin.NSObject
import kotlin.native.concurrent.freeze


@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapViewMobile(
    centerCoordinate: Coordinate,
    modifier: Modifier,
    onMarkerClick: (Coordinate) -> Unit
) {
    // Hold reference to delegate to prevent GC
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
        }.freeze()
    }

    UIKitView(
        factory = {
            MKMapView().apply {
                // Set region
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

                delegate = delegateRef

                // Add annotation
                val annotation = MKPointAnnotation(
                    coordinate = CLLocationCoordinate2DMake(
                        centerCoordinate.latitude,
                        centerCoordinate.longitude
                    )
                )
                addAnnotation(annotation)
            }
        },
        modifier = modifier,
        update = { view ->
            // Update region and annotation on coordinate change
            view.setRegion(
                region = MKCoordinateRegionMake(
                    centerCoordinate = CLLocationCoordinate2DMake(
                        centerCoordinate.latitude,
                        centerCoordinate.longitude
                            ),
                     span = MKCoordinateSpanMake(
                         latitudeDelta = 0.01,
                         longitudeDelta = 0.01
                     )
                ),
                animated = true
            )

            // Remove existing annotations
            val annotations = view.annotations
            view.removeAnnotations(annotations)

            // Add updated annotation
            val annotation = MKPointAnnotation(
                coordinate = CLLocationCoordinate2DMake(
                    centerCoordinate.latitude,
                    centerCoordinate.longitude
                )
            )
            view.addAnnotation(annotation = annotation)
        }
    )
}
