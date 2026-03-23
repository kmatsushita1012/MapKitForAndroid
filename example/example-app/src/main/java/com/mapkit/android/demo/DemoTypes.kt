package com.mapkit.android.demo

internal enum class DemoTab { Map, Settings }
internal enum class DrawMode { Browse, Annotation, Polyline, Polygon, Circle }
internal enum class PlacementTrigger { Tap, LongPress }
internal enum class AnnotationVisualStyle { Default, CustomImage }
internal enum class MarkerGlyphMode { GlyphText, GlyphImage }
internal enum class ZoomRangePreset {
    none,
    city,
    district
}
internal enum class PoiFilterPreset {
    all,
    none,
    includeCafePark,
    excludeCafePark
}
