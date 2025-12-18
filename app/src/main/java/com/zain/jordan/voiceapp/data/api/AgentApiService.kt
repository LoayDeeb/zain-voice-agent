package com.zain.jordan.voiceapp.data.api

import com.zain.jordan.voiceapp.data.model.AgentRequest
import com.zain.jordan.voiceapp.data.model.AgentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AgentApiService {
    
    @POST("api/agent/invoke")
    suspend fun invokeAgent(
        @Header("X-API-Key") apiKey: String,
        @Body request: AgentRequest
    ): Response<AgentResponse>
}
