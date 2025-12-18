package com.zain.jordan.voiceapp

import android.app.Application
import io.livekit.android.LiveKit
import io.livekit.android.util.LoggingLevel

class ZainVoiceApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        LiveKit.loggingLevel = LoggingLevel.DEBUG
    }
}
