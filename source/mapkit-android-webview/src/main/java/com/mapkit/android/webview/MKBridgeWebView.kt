package com.mapkit.android.webview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mapkit.android.model.MKCoordinate
import com.mapkit.android.model.MKCoordinateRegion
import com.mapkit.android.model.MKMapErrorCause
import com.mapkit.android.model.MKMapEvent
import com.mapkit.android.model.MKMapState
import com.mapkit.android.webview.internal.InternalMapState
import com.mapkit.android.webview.internal.MKBridgeMapper
import com.mapkit.android.webview.internal.approximatelyEquals
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
class MKBridgeWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : WebView(context, attrs) {

    private var onEvent: ((MKMapEvent) -> Unit)? = null
    private var isPageReady = false
    private var hasInitCalled = false
    private var lastAppliedState: InternalMapState? = null
    private var pendingState: MKMapState? = null

    private val androidBridge = object {
        @JavascriptInterface
        fun emitEvent(payload: String) {
            try {
                val event = parseEvent(JSONObject(payload))
                post { onEvent?.invoke(event) }
            } catch (t: Throwable) {
                post {
                    onEvent?.invoke(
                        MKMapEvent.MapError(
                            MKMapErrorCause.BridgeFailure("Failed to parse JS event: ${t.message}")
                        )
                    )
                }
            }
        }
    }

    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                isPageReady = true
                if (hasInitCalled) {
                    flushPendingState()
                }
            }
        }
        addJavascriptInterface(androidBridge, "AndroidMKBridge")
        loadDataWithBaseURL(
            "https://mapkit.android.local/",
            bridgeHtml(),
            "text/html",
            "utf-8",
            null
        )
    }

    fun setEventListener(listener: (MKMapEvent) -> Unit) {
        this.onEvent = listener
    }

    fun ensureInitialized(token: String) {
        if (hasInitCalled) return
        hasInitCalled = true
        if (!isPageReady) return
        val escaped = JSONObject.quote(token)
        evaluateJavascriptSafe("window.MKBridge && window.MKBridge.init($escaped);")
    }

    fun applyState(state: MKMapState) {
        pendingState = state
        if (!isPageReady || !hasInitCalled) return
        flushPendingState()
    }

    private fun flushPendingState() {
        val latest = pendingState ?: return
        val latestInternal = MKBridgeMapper.toInternal(latest)
        if (lastAppliedState?.approximatelyEquals(latestInternal) == true) return

        val payload = serializeState(latest)
        evaluateJavascriptSafe("window.MKBridge && window.MKBridge.applyState($payload);")
        lastAppliedState = latestInternal
    }

    private fun evaluateJavascriptSafe(script: String) {
        evaluateJavascript(script, ValueCallback { })
    }

    private fun serializeState(state: MKMapState): String {
        val regionJson = JSONObject()
            .put("centerLat", state.region.center.latitude)
            .put("centerLng", state.region.center.longitude)
            .put("latDelta", state.region.span.latitudeDelta)
            .put("lngDelta", state.region.span.longitudeDelta)

        val annotations = JSONArray().apply {
            state.annotations.forEach { annotation ->
                put(
                    JSONObject()
                        .put("id", annotation.id)
                        .put("lat", annotation.coordinate.latitude)
                        .put("lng", annotation.coordinate.longitude)
                        .put("title", annotation.title)
                )
            }
        }

        val overlays = JSONArray().apply {
            state.overlays.forEach { overlay ->
                put(JSONObject().put("id", overlay.id).put("type", overlay::class.simpleName))
            }
        }

        return JSONObject()
            .put("region", regionJson)
            .put("annotations", annotations)
            .put("overlays", overlays)
            .put("showsTraffic", state.options.showsTraffic)
            .toString()
    }

    private fun parseEvent(json: JSONObject): MKMapEvent {
        return when (json.optString("type")) {
            "mapLoaded" -> MKMapEvent.MapLoaded
            "regionDidChange" -> {
                val region = json.getJSONObject("region")
                val internal = InternalMapState(
                    centerLat = region.getDouble("centerLat"),
                    centerLng = region.getDouble("centerLng"),
                    latDelta = region.getDouble("latDelta"),
                    lngDelta = region.getDouble("lngDelta")
                )
                val settled = json.optBoolean("settled", true)
                MKMapEvent.RegionDidChange(MKBridgeMapper.toRegion(internal), settled = settled)
            }

            "annotationTapped" -> MKMapEvent.AnnotationTapped(json.getString("id"))
            "overlayTapped" -> MKMapEvent.OverlayTapped(json.getString("id"))
            "userLocationUpdated" -> MKMapEvent.UserLocationUpdated(
                coordinate = MKCoordinate(
                    latitude = json.getDouble("lat"),
                    longitude = json.getDouble("lng")
                )
            )

            else -> MKMapEvent.MapError(
                MKMapErrorCause.BridgeFailure("Unknown event type: ${json.optString("type")}")
            )
        }
    }

    private fun bridgeHtml(): String = """
        <!doctype html>
        <html>
          <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <style>
              html, body { margin: 0; padding: 0; height: 100%; font-family: sans-serif; background: #f4f7fb; }
              #map { height: 100%; display: grid; grid-template-rows: auto 1fr auto; }
              #header { padding: 10px 12px; background: #0f172a; color: #fff; font-size: 13px; }
              #canvas { margin: 12px; border-radius: 12px; border: 1px solid #cbd5e1; background: #e2e8f0; padding: 10px; }
              #footer { padding: 10px 12px; display: flex; gap: 8px; }
              button { border: none; border-radius: 8px; padding: 8px 10px; background: #0ea5e9; color: #fff; }
              #status { font-size: 12px; color: #334155; white-space: pre-wrap; }
            </style>
          </head>
          <body>
            <div id="map">
              <div id="header">MK Bridge WebView</div>
              <div id="canvas">
                <div id="status">loading...</div>
              </div>
              <div id="footer">
                <button onclick="window.MKBridge.simulateAnnotationTap()">Annotation Tap</button>
                <button onclick="window.MKBridge.simulateOverlayTap()">Overlay Tap</button>
                <button onclick="window.MKBridge.simulatePan()">Simulate Pan</button>
              </div>
            </div>
            <script>
              (function() {
                const state = {
                  token: null,
                  region: {
                    centerLat: 35.681236,
                    centerLng: 139.767125,
                    latDelta: 0.05,
                    lngDelta: 0.05
                  },
                  annotations: [],
                  overlays: []
                };

                function emit(payload) {
                  if (window.AndroidMKBridge && window.AndroidMKBridge.emitEvent) {
                    window.AndroidMKBridge.emitEvent(JSON.stringify(payload));
                  }
                }

                function renderStatus() {
                  const status = document.getElementById("status");
                  status.textContent =
                    "center: " + state.region.centerLat.toFixed(6) + ", " + state.region.centerLng.toFixed(6) + "\n" +
                    "span: " + state.region.latDelta.toFixed(5) + ", " + state.region.lngDelta.toFixed(5) + "\n" +
                    "annotations: " + state.annotations.length + "\n" +
                    "overlays: " + state.overlays.length + "\n" +
                    "token: " + (state.token ? "set" : "unset");
                }

                window.MKBridge = {
                  init: function(token) {
                    state.token = token;
                    renderStatus();
                    emit({ type: "mapLoaded" });
                  },
                  applyState: function(payload) {
                    if (payload && payload.region) state.region = payload.region;
                    if (payload && payload.annotations) state.annotations = payload.annotations;
                    if (payload && payload.overlays) state.overlays = payload.overlays;
                    renderStatus();
                  },
                  simulatePan: function() {
                    state.region.centerLat = state.region.centerLat + 0.001;
                    state.region.centerLng = state.region.centerLng + 0.001;
                    renderStatus();
                    emit({ type: "regionDidChange", region: state.region, settled: true });
                  },
                  simulateAnnotationTap: function() {
                    const id = (state.annotations[0] && state.annotations[0].id) || "sample-annotation";
                    emit({ type: "annotationTapped", id: id });
                  },
                  simulateOverlayTap: function() {
                    const id = (state.overlays[0] && state.overlays[0].id) || "sample-overlay";
                    emit({ type: "overlayTapped", id: id });
                  }
                };

                renderStatus();
              })();
            </script>
          </body>
        </html>
    """.trimIndent()
}
