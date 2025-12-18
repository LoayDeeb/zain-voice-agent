package com.zain.jordan.voiceapp.data.model

enum class CallState {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTING,
    DISCONNECTED,
    ERROR
}

data class CallInfo(
    val state: CallState = CallState.IDLE,
    val duration: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val errorMessage: String? = null,
    val agentName: String = "Zain Assistant",
    val roomName: String? = null
)
