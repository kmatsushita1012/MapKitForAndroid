package com.mapkit.android.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mapkit.android.api.MKMapKit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!MKMapKit.isInitialized()) {
            MKMapKit.init(BuildConfig.MAPKIT_JS_TOKEN)
        }

        setContent {
            DemoScreen()
        }
    }
}
