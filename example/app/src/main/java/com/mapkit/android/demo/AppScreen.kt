package com.studiomk.mapkit.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studiomk.mapkit.api.MKMapView
import com.studiomk.mapkit.api.rememberMKMapController
import com.studiomk.mapkit.model.MKAnnotationStyle
import com.studiomk.mapkit.model.MKCoordinate
import com.studiomk.mapkit.model.MKCoordinateRegion
import com.studiomk.mapkit.model.MKMapOptions
import com.studiomk.mapkit.model.MKOverlayStyle

@Composable
internal fun AppScreen() {
    val controller = rememberMKMapController()

    var region by remember {
        mutableStateOf(
            MKCoordinateRegion.fromCenter(
                center = MKCoordinate(35.681236, 139.767125),
                latitudeDelta = 0.05,
                longitudeDelta = 0.05
            )
        )
    }

    var pinCoordinate by remember { mutableStateOf(MKCoordinate(35.681236, 139.767125)) }
    var selectedMessage by remember { mutableStateOf("No selection") }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = selectedMessage,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.height(40.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { controller.selectAnnotation("tokyo-station", animated = true) }) {
                    Text("Select")
                }
                Button(onClick = { controller.deselectAnnotation(animated = false) }) {
                    Text("Deselect")
                }
            }
            Button(
                onClick = {
                    region = MKCoordinateRegion.fromCenter(
                        center = MKCoordinate(35.6895, 139.6917),
                        latitudeDelta = 0.07,
                        longitudeDelta = 0.07
                    )
                }
            ) {
                Text("Move Region to Shinjuku")
            }
        }

        MKMapView(
            region = region,
            controller = controller,
            options = MKMapOptions(showsZoomControl = true),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onRegionDidChange = { changed ->
                region = changed
            }
        ) {
            Annotation(
                id = "tokyo-station",
                coordinate = pinCoordinate,
                title = "Tokyo Station",
                subtitle = "Drag me",
                isDraggable = true,
                style = MKAnnotationStyle.Marker(tintHex = "#0EA5E9"),
                onSelected = {
                    selectedMessage = "AnnotationSelected(id=tokyo-station)"
                },
                onDeselected = {
                    selectedMessage = "AnnotationDeselected(id=tokyo-station)"
                },
                onDrag = { coordinate ->
                    pinCoordinate = coordinate
                    selectedMessage = "Dragging: ${coordinate.latitude}, ${coordinate.longitude}"
                },
                onDragEnd = { coordinate ->
                    pinCoordinate = coordinate
                    selectedMessage = "DragEnd: ${coordinate.latitude}, ${coordinate.longitude}"
                }
            )

            PolylineOverlay(
                id = "sample-route",
                points = listOf(
                    MKCoordinate(35.681236, 139.767125),
                    MKCoordinate(35.6895, 139.6917)
                ),
                style = MKOverlayStyle(strokeColorHex = "#2563EB", strokeWidth = 4.0),
                onTap = {
                    selectedMessage = "OverlayTapped(id=sample-route)"
                }
            )
        }
    }
}
