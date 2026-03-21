(function () {
  const state = {
    token: null,
    mapReady: false,
    useMock: true,
    map: null,
    annotationsById: {},
    overlaysById: {},
    region: {
      centerLat: 35.681236,
      centerLng: 139.767125,
      latDelta: 0.05,
      lngDelta: 0.05,
    },
    annotations: [],
    overlays: [],
  };

  function emit(payload) {
    if (window.AndroidMKBridge && window.AndroidMKBridge.emitEvent) {
      window.AndroidMKBridge.emitEvent(JSON.stringify(payload));
    }
  }

  function renderStatus() {
    const status = document.getElementById("status");
    status.textContent =
      "mode: " + (state.useMock ? "mock" : "mapkit") + "\n" +
      "center: " + state.region.centerLat.toFixed(6) + ", " + state.region.centerLng.toFixed(6) + "\n" +
      "span: " + state.region.latDelta.toFixed(5) + ", " + state.region.lngDelta.toFixed(5) + "\n" +
      "annotations: " + state.annotations.length + "\n" +
      "overlays: " + state.overlays.length + "\n" +
      "token: " + (state.token ? "set" : "unset");
  }

  function emitBridgeError(message) {
    emit({
      type: "bridgeError",
      message: String(message || "unknown bridge error"),
    });
  }

  function loadMapKitScriptIfNeeded() {
    return new Promise((resolve, reject) => {
      if (window.mapkit) {
        resolve();
        return;
      }
      const existing = document.getElementById("apple-mapkit-script");
      if (existing) {
        existing.addEventListener("load", () => resolve());
        existing.addEventListener("error", () => reject(new Error("failed to load mapkit script")));
        return;
      }
      const script = document.createElement("script");
      script.id = "apple-mapkit-script";
      script.src = "https://cdn.apple-mapkit.com/mk/5.x.x/mapkit.core.js";
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error("failed to load mapkit script"));
      document.head.appendChild(script);
    });
  }

  function attachMapEvents() {
    if (!state.map || !window.mapkit || !window.mapkit.addEventListener) return;
    state.map.addEventListener("region-change-end", function () {
      try {
        const r = state.map.region;
        if (!r || !r.center || !r.span) return;
        state.region = {
          centerLat: r.center.latitude,
          centerLng: r.center.longitude,
          latDelta: r.span.latitudeDelta,
          lngDelta: r.span.longitudeDelta,
        };
        renderStatus();
        emit({ type: "regionDidChange", region: state.region, settled: true });
      } catch (e) {
        emitBridgeError(e && e.message ? e.message : e);
      }
    });
  }

  function clearMapObjects() {
    if (!state.map || !window.mapkit) return;
    Object.keys(state.annotationsById).forEach((id) => {
      const a = state.annotationsById[id];
      try {
        state.map.removeAnnotation(a);
      } catch (_) {}
    });
    Object.keys(state.overlaysById).forEach((id) => {
      const o = state.overlaysById[id];
      try {
        state.map.removeOverlay(o);
      } catch (_) {}
    });
    state.annotationsById = {};
    state.overlaysById = {};
  }

  function applyMapKitRegion(region) {
    if (!state.map || !window.mapkit || !region) return;
    const center = new window.mapkit.Coordinate(region.centerLat, region.centerLng);
    const span = new window.mapkit.CoordinateSpan(region.latDelta, region.lngDelta);
    state.map.region = new window.mapkit.CoordinateRegion(center, span);
  }

  function applyMapKitAnnotations(annotations) {
    if (!state.map || !window.mapkit) return;
    annotations.forEach((item) => {
      if (!item || !item.id) return;
      const coord = new window.mapkit.Coordinate(item.lat, item.lng);
      const marker = new window.mapkit.MarkerAnnotation(coord, {
        title: item.title || item.id,
      });
      marker.data = { id: item.id };
      marker.addEventListener("select", function () {
        emit({ type: "annotationTapped", id: item.id });
      });
      state.annotationsById[item.id] = marker;
      state.map.addAnnotation(marker);
    });
  }

  function applyMapKitOverlays(overlays) {
    if (!state.map || !window.mapkit) return;
    overlays.forEach((item) => {
      if (!item || !item.id || item.type !== "MKPolylineOverlay") return;
      const points = (item.points || []).map((p) => new window.mapkit.Coordinate(p.lat, p.lng));
      if (!points.length) return;
      const polyline = new window.mapkit.PolylineOverlay(points, {
        style: new window.mapkit.Style({
          lineWidth: 4,
          strokeColor: "#0EA5E9",
        }),
      });
      polyline.data = { id: item.id };
      state.overlaysById[item.id] = polyline;
      state.map.addOverlay(polyline);
    });
  }

  function initializeMapKit() {
    return loadMapKitScriptIfNeeded()
      .then(() => {
        if (!window.mapkit) throw new Error("mapkit is unavailable");
        window.mapkit.init({
          authorizationCallback: function (done) {
            done(state.token);
          },
        });
        state.map = new window.mapkit.Map("mapCanvas", {
          isRotationEnabled: true,
          isZoomEnabled: true,
          isScrollEnabled: true,
          showsCompass: window.mapkit.FeatureVisibility && window.mapkit.FeatureVisibility.Adaptive,
        });
        attachMapEvents();
        state.mapReady = true;
        state.useMock = false;
      });
  }

  window.MKBridge = {
    init: function (token) {
      state.token = token;
      initializeMapKit()
        .then(function () {
          renderStatus();
          emit({ type: "mapLoaded" });
        })
        .catch(function (e) {
          state.mapReady = false;
          state.useMock = true;
          renderStatus();
          emitBridgeError(e && e.message ? e.message : e);
          emit({ type: "mapLoaded" });
        });
    },
    applyState: function (payload) {
      if (payload && payload.region) state.region = payload.region;
      if (payload && payload.annotations) state.annotations = payload.annotations;
      if (payload && payload.overlays) state.overlays = payload.overlays;
      if (!state.useMock && state.mapReady) {
        try {
          clearMapObjects();
          applyMapKitRegion(state.region);
          applyMapKitAnnotations(state.annotations);
          applyMapKitOverlays(state.overlays);
        } catch (e) {
          emitBridgeError(e && e.message ? e.message : e);
        }
      }
      renderStatus();
    },
    simulatePan: function () {
      state.region.centerLat = state.region.centerLat + 0.001;
      state.region.centerLng = state.region.centerLng + 0.001;
      renderStatus();
      if (!state.useMock && state.mapReady) {
        try {
          applyMapKitRegion(state.region);
        } catch (e) {
          emitBridgeError(e && e.message ? e.message : e);
        }
      }
      emit({ type: "regionDidChange", region: state.region, settled: true });
    },
    simulateAnnotationTap: function () {
      const id = (state.annotations[0] && state.annotations[0].id) || "sample-annotation";
      emit({ type: "annotationTapped", id: id });
    },
    simulateOverlayTap: function () {
      const id = (state.overlays[0] && state.overlays[0].id) || "sample-overlay";
      emit({ type: "overlayTapped", id: id });
    },
  };

  renderStatus();
})();
