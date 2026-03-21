(function () {
  const state = {
    token: null,
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
      "center: " + state.region.centerLat.toFixed(6) + ", " + state.region.centerLng.toFixed(6) + "\n" +
      "span: " + state.region.latDelta.toFixed(5) + ", " + state.region.lngDelta.toFixed(5) + "\n" +
      "annotations: " + state.annotations.length + "\n" +
      "overlays: " + state.overlays.length + "\n" +
      "token: " + (state.token ? "set" : "unset");
  }

  window.MKBridge = {
    init: function (token) {
      state.token = token;
      renderStatus();
      emit({ type: "mapLoaded" });
    },
    applyState: function (payload) {
      if (payload && payload.region) state.region = payload.region;
      if (payload && payload.annotations) state.annotations = payload.annotations;
      if (payload && payload.overlays) state.overlays = payload.overlays;
      renderStatus();
    },
    simulatePan: function () {
      state.region.centerLat = state.region.centerLat + 0.001;
      state.region.centerLng = state.region.centerLng + 0.001;
      renderStatus();
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
