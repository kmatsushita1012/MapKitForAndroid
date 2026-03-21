package com.mapkit.android.webview.internal

import com.mapkit.android.model.MKMapState

internal data class InternalMapState(
    val centerLat: Double,
    val centerLng: Double,
    val latDelta: Double,
    val lngDelta: Double
)

internal object MKBridgeMapper {
    fun toInternal(state: MKMapState): InternalMapState = InternalMapState(
        centerLat = state.region.center.latitude,
        centerLng = state.region.center.longitude,
        latDelta = state.region.span.latitudeDelta,
        lngDelta = state.region.span.longitudeDelta
    )
}
