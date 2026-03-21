package com.mapkit.android.api

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.mapkit.android.model.MKMapErrorCause
import com.mapkit.android.model.MKMapEvent
import com.mapkit.android.model.MKMapState

@Composable
fun MKMapView(
    state: MKMapState,
    onEvent: (MKMapEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state) {
        if (!MKMapKit.isInitialized()) {
            onEvent(MKMapEvent.MapError(MKMapErrorCause.TokenUnavailable))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Text("MKMapView placeholder: region=${state.region.center.latitude},${state.region.center.longitude}")
    }
}
