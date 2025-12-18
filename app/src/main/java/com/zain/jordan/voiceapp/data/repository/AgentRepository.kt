package com.zain.jordan.voiceapp.data.repository

import com.zain.jordan.voiceapp.data.api.ApiClient
import com.zain.jordan.voiceapp.data.model.AgentRequest
import com.zain.jordan.voiceapp.data.model.AgentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AgentRepository {
    
    private val apiService = ApiClient.agentApiService
    
    suspend fun sendMessage(
        message: String,
        sessionId: String? = null
    ): Result<AgentResponse> = withContext(Dispatchers.IO) {
        try {
            val request = AgentRequest(
                agentId = ApiClient.AGENT_ID,
                message = message,
                sessionId = sessionId
            )
            
            val response = apiService.invokeAgent(
                apiKey = ApiClient.AGENT_API_KEY,
                request = request
            )
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("API Error ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
