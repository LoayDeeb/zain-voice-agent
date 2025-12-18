package com.zain.jordan.voiceapp.data.model

import com.google.gson.annotations.SerializedName

data class AgentRequest(
    @SerializedName("agent_id")
    val agentId: String = "14e9ebf0-ae34-4b21-8760-b0e3fe87275d",
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("channel")
    val channel: String = "api",
    
    @SerializedName("persist_messages")
    val persistMessages: Boolean = true,
    
    @SerializedName("max_iterations")
    val maxIterations: Int = 30,
    
    @SerializedName("max_tool_iterations")
    val maxToolIterations: Int = 10,
    
    @SerializedName("session_id")
    val sessionId: String? = null
)

data class AgentResponse(
    @SerializedName("response")
    val response: String? = null,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("session_id")
    val sessionId: String? = null,
    
    @SerializedName("success")
    val success: Boolean? = null,
    
    @SerializedName("error")
    val error: String? = null
)
