package com.mapkit.android.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.mapkit.android.model.MKMapErrorCause
import com.mapkit.android.model.MKMapEvent
import com.mapkit.android.model.MKMapState
import com.mapkit.android.webview.MKBridgeWebView
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MKMapView(
    state: MKMapState,
    onEvent: (MKMapEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnEvent = rememberUpdatedState(onEvent)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MKBridgeWebView(context).also { webView ->
                webView.setEventListener { event -> latestOnEvent.value(event) }
                val token = MKMapKit.currentTokenOrNull()
                if (token != null) {
                    webView.ensureInitialized(token)
                    webView.applyState(state)
                } else {
                    latestOnEvent.value(MKMapEvent.MapError(MKMapErrorCause.TokenUnavailable))
                }
            }
        },
        update = { webView ->
            webView.setEventListener { event -> latestOnEvent.value(event) }
            val token = MKMapKit.currentTokenOrNull()
            if (token != null) {
                webView.ensureInitialized(token)
                webView.applyState(state)
            } else {
                latestOnEvent.value(MKMapEvent.MapError(MKMapErrorCause.TokenUnavailable))
            }
        }
    )
}
