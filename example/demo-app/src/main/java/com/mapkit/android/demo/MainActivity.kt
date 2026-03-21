package com.mapkit.android.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mapkit.android.api.MKMapKit
import com.mapkit.android.api.MKMapView
import com.mapkit.android.model.MKCoordinate
import com.mapkit.android.model.MKCoordinateRegion
import com.mapkit.android.model.MKMapEvent
import com.mapkit.android.model.MKMapState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!MKMapKit.isInitialized()) {
            MKMapKit.init(BuildConfig.MAPKIT_JS_TOKEN)
        }

        setContent {
            var state by remember {
                mutableStateOf(
                    MKMapState(
                        region = MKCoordinateRegion.fromCenter(
                            center = MKCoordinate(35.681236, 139.767125),
                            latitudeDelta = 0.05,
                            longitudeDelta = 0.05
                        )
                    )
                )
            }

            MKMapView(
                state = state,
                onEvent = { event ->
                    if (event is MKMapEvent.RegionDidChange && event.settled) {
                        state = state.copy(region = event.region)
                    }
                }
            )
        }
    }
}
