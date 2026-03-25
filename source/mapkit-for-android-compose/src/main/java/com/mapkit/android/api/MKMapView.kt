package com.studiomk.mapkit.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.studiomk.mapkit.model.MKAnnotation
import com.studiomk.mapkit.model.MKAnnotationStyle
import com.studiomk.mapkit.model.MKCircleOverlay
import com.studiomk.mapkit.model.MKCoordinate
import com.studiomk.mapkit.model.MKCoordinateRegion
import com.studiomk.mapkit.model.MKMapEvent
import com.studiomk.mapkit.model.MKMapOptions
import com.studiomk.mapkit.model.MKMapRenderState
import com.studiomk.mapkit.model.MKMapController
import com.studiomk.mapkit.model.MKOverlay
import com.studiomk.mapkit.model.MKOverlayStyle
import com.studiomk.mapkit.model.MKPolygonOverlay
import com.studiomk.mapkit.model.MKPolylineOverlay
import com.studiomk.mapkit.webview.MKBridgeWebView

@DslMarker
annotation class MKMapDsl

@Stable
@Composable
fun rememberMKMapController(): MKMapController = remember { MKMapController() }

internal data class MKAnnotationCallbacks(
    val onSelected: (() -> Unit)? = null,
    val onDeselected: (() -> Unit)? = null,
    val onDragStart: (() -> Unit)? = null,
    val onDrag: ((MKCoordinate) -> Unit)? = null,
    val onDragEnd: ((MKCoordinate) -> Unit)? = null
)

internal data class MKOverlayCallbacks(
    val onTap: (() -> Unit)? = null
)

internal data class MKCollectedContent(
    val annotations: List<MKAnnotation>,
    val overlays: List<MKOverlay>,
    val annotationCallbacksById: Map<String, MKAnnotationCallbacks>,
    val overlayCallbacksById: Map<String, MKOverlayCallbacks>
)

@MKMapDsl
interface MKMapContentScope {
    fun Annotation(
        id: String,
        coordinate: MKCoordinate,
        title: String? = null,
        subtitle: String? = null,
        isVisible: Boolean = true,
        isDraggable: Boolean = false,
        style: MKAnnotationStyle = MKAnnotationStyle.Marker(),
        onSelected: (() -> Unit)? = null,
        onDeselected: (() -> Unit)? = null,
        onDragStart: (() -> Unit)? = null,
        onDrag: ((MKCoordinate) -> Unit)? = null,
        onDragEnd: ((MKCoordinate) -> Unit)? = null
    )

    fun PolylineOverlay(
        id: String,
        points: List<MKCoordinate>,
        style: MKOverlayStyle = MKOverlayStyle(),
        isVisible: Boolean = true,
        zIndex: Int = 0,
        onTap: (() -> Unit)? = null
    )

    fun PolygonOverlay(
        id: String,
        points: List<MKCoordinate>,
        holes: List<List<MKCoordinate>> = emptyList(),
        style: MKOverlayStyle = MKOverlayStyle(),
        isVisible: Boolean = true,
        zIndex: Int = 0,
        onTap: (() -> Unit)? = null
    )

    fun CircleOverlay(
        id: String,
        center: MKCoordinate,
        radiusMeter: Double,
        style: MKOverlayStyle = MKOverlayStyle(),
        isVisible: Boolean = true,
        zIndex: Int = 0,
        onTap: (() -> Unit)? = null
    )
}

private class MKMapContentCollector : MKMapContentScope {
    private val annotations = mutableListOf<MKAnnotation>()
    private val overlays = mutableListOf<MKOverlay>()
    private val annotationCallbacksById = linkedMapOf<String, MKAnnotationCallbacks>()
    private val overlayCallbacksById = linkedMapOf<String, MKOverlayCallbacks>()

    override fun Annotation(
        id: String,
        coordinate: MKCoordinate,
        title: String?,
        subtitle: String?,
        isVisible: Boolean,
        isDraggable: Boolean,
        style: MKAnnotationStyle,
        onSelected: (() -> Unit)?,
        onDeselected: (() -> Unit)?,
        onDragStart: (() -> Unit)?,
        onDrag: ((MKCoordinate) -> Unit)?,
        onDragEnd: ((MKCoordinate) -> Unit)?
    ) {
        registerUniqueId(id)
        annotations += MKAnnotation(
            id = id,
            coordinate = coordinate,
            title = title,
            subtitle = subtitle,
            isVisible = isVisible,
            isDraggable = isDraggable,
            style = style
        )
        annotationCallbacksById[id] = MKAnnotationCallbacks(
            onSelected = onSelected,
            onDeselected = onDeselected,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd
        )
    }

    override fun PolylineOverlay(
        id: String,
        points: List<MKCoordinate>,
        style: MKOverlayStyle,
        isVisible: Boolean,
        zIndex: Int,
        onTap: (() -> Unit)?
    ) {
        registerUniqueId(id)
        overlays += MKPolylineOverlay(
            id = id,
            points = points,
            style = style,
            isVisible = isVisible,
            zIndex = zIndex
        )
        overlayCallbacksById[id] = MKOverlayCallbacks(onTap = onTap)
    }

    override fun PolygonOverlay(
        id: String,
        points: List<MKCoordinate>,
        holes: List<List<MKCoordinate>>,
        style: MKOverlayStyle,
        isVisible: Boolean,
        zIndex: Int,
        onTap: (() -> Unit)?
    ) {
        registerUniqueId(id)
        overlays += MKPolygonOverlay(
            id = id,
            points = points,
            holes = holes,
            style = style,
            isVisible = isVisible,
            zIndex = zIndex
        )
        overlayCallbacksById[id] = MKOverlayCallbacks(onTap = onTap)
    }

    override fun CircleOverlay(
        id: String,
        center: MKCoordinate,
        radiusMeter: Double,
        style: MKOverlayStyle,
        isVisible: Boolean,
        zIndex: Int,
        onTap: (() -> Unit)?
    ) {
        registerUniqueId(id)
        overlays += MKCircleOverlay(
            id = id,
            center = center,
            radiusMeter = radiusMeter,
            style = style,
            isVisible = isVisible,
            zIndex = zIndex
        )
        overlayCallbacksById[id] = MKOverlayCallbacks(onTap = onTap)
    }

    fun collect(block: MKMapContentScope.() -> Unit): MKCollectedContent {
        block(this)
        return MKCollectedContent(
            annotations = annotations.toList(),
            overlays = overlays.toList(),
            annotationCallbacksById = annotationCallbacksById.toMap(),
            overlayCallbacksById = overlayCallbacksById.toMap()
        )
    }

    private fun registerUniqueId(id: String) {
        require(id.isNotBlank()) { "id must not be blank" }
        require(id !in annotationCallbacksById && id !in overlayCallbacksById) {
            "Duplicated map content id: $id"
        }
    }
}

@Composable
fun MKMapView(
    region: MKCoordinateRegion,
    controller: MKMapController,
    options: MKMapOptions = MKMapOptions(),
    modifier: Modifier = Modifier,
    onRegionDidChange: ((MKCoordinateRegion) -> Unit)? = null,
    onEvent: (MKMapEvent) -> Unit = {},
    content: MKMapContentScope.() -> Unit
) {
    val collected = MKMapContentCollector().collect(content)
    val renderState = MKMapRenderState(
        region = region,
        annotations = collected.annotations,
        overlays = collected.overlays,
        options = options
    )

    val latestAnnotationCallbacks = rememberUpdatedState(collected.annotationCallbacksById)
    val latestOverlayCallbacks = rememberUpdatedState(collected.overlayCallbacksById)
    val latestOnRegionDidChange = rememberUpdatedState(onRegionDidChange)
    val latestOnEvent = rememberUpdatedState(onEvent)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MKBridgeWebView(context).also { webView ->
                webView.setEventListener { event ->
                    when (event) {
                        is MKMapEvent.RegionDidChange -> latestOnRegionDidChange.value?.invoke(event.region)
                        is MKMapEvent.AnnotationSelected -> {
                            latestAnnotationCallbacks.value[event.id]?.onSelected?.invoke()
                        }
                        is MKMapEvent.AnnotationDeselected -> {
                            latestAnnotationCallbacks.value[event.id]?.onDeselected?.invoke()
                        }
                        is MKMapEvent.AnnotationDragStart -> {
                            latestAnnotationCallbacks.value[event.id]?.onDragStart?.invoke()
                        }
                        is MKMapEvent.AnnotationDragging -> {
                            latestAnnotationCallbacks.value[event.id]?.onDrag?.invoke(event.coordinate)
                        }
                        is MKMapEvent.AnnotationDragEnd -> {
                            latestAnnotationCallbacks.value[event.id]?.onDragEnd?.invoke(event.coordinate)
                        }
                        is MKMapEvent.OverlayTapped -> {
                            latestOverlayCallbacks.value[event.id]?.onTap?.invoke()
                        }
                        else -> Unit
                    }
                    latestOnEvent.value(event)
                }
                controller.bindCommandDispatcher { command -> webView.applyCommand(command) }
                val token = MKMapKit.currentTokenOrNull()
                if (token != null) {
                    webView.ensureInitialized(token)
                    webView.applyState(renderState)
                }
            }
        },
        update = { webView ->
            webView.setEventListener { event ->
                when (event) {
                    is MKMapEvent.RegionDidChange -> latestOnRegionDidChange.value?.invoke(event.region)
                    is MKMapEvent.AnnotationSelected -> {
                        latestAnnotationCallbacks.value[event.id]?.onSelected?.invoke()
                    }
                    is MKMapEvent.AnnotationDeselected -> {
                        latestAnnotationCallbacks.value[event.id]?.onDeselected?.invoke()
                    }
                    is MKMapEvent.AnnotationDragStart -> {
                        latestAnnotationCallbacks.value[event.id]?.onDragStart?.invoke()
                    }
                    is MKMapEvent.AnnotationDragging -> {
                        latestAnnotationCallbacks.value[event.id]?.onDrag?.invoke(event.coordinate)
                    }
                    is MKMapEvent.AnnotationDragEnd -> {
                        latestAnnotationCallbacks.value[event.id]?.onDragEnd?.invoke(event.coordinate)
                    }
                    is MKMapEvent.OverlayTapped -> {
                        latestOverlayCallbacks.value[event.id]?.onTap?.invoke()
                    }
                    else -> Unit
                }
                latestOnEvent.value(event)
            }
            controller.bindCommandDispatcher { command -> webView.applyCommand(command) }
            val token = MKMapKit.currentTokenOrNull()
            if (token != null) {
                webView.ensureInitialized(token)
                webView.applyState(renderState)
            }
        }
    )
}
