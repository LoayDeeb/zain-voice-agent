package com.zain.jordan.voiceapp.data.model

import com.google.gson.annotations.SerializedName

data class TokenResponse(
    @SerializedName("token")
    val token: String,
    @SerializedName("room_name")
    val roomName: String,
    @SerializedName("participant_name")
    val participantName: String,
    @SerializedName("server_url")
    val serverUrl: String
)

data class AgentMessage(
    @SerializedName("id")
    val id: String,
    @SerializedName("text")
    val text: String,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("is_user")
    val isUser: Boolean,
    @SerializedName("audio_url")
    val audioUrl: String? = null
)

data class ConversationHistory(
    @SerializedName("messages")
    val messages: List<AgentMessage>,
    @SerializedName("session_id")
    val sessionId: String
)
