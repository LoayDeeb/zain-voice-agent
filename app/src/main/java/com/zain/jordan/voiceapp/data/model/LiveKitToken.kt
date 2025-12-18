package com.zain.jordan.voiceapp.data.model

import com.google.gson.annotations.SerializedName

data class LiveKitToken(
    @SerializedName("token")
    val token: String,
    
    @SerializedName("url")
    val url: String? = null
)
