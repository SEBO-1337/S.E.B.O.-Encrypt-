package com.sebo.seboencrypt

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import com.journeyapps.barcodescanner.CaptureActivity

/** Eigene CaptureActivity, die auf Hochformat fixiert wird. */
class PortraitCaptureActivity : CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT < 36) {
            @Suppress("SourceLockedOrientationActivity")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
    }
}

