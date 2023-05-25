package com.phazei.dynamicgptchat

import android.app.Application
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply dynamic color
        // DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
