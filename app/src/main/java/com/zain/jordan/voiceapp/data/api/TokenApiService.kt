package com.zain.jordan.voiceapp.data.api

import com.zain.jordan.voiceapp.data.model.LiveKitToken
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TokenApiService {
    
    @GET("api/token")
    suspend fun getToken(
        @Query("room") roomName: String,
        @Query("identity") identity: String
    ): Response<LiveKitToken>
}
