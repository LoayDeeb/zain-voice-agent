package com.zain.jordan.voiceapp.viewmodel

import android.app.Application
import android.media.AudioManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.twilio.audioswitch.AudioDevice
import com.zain.jordan.voiceapp.data.api.ApiClient
import com.zain.jordan.voiceapp.data.model.CallInfo
import com.zain.jordan.voiceapp.data.model.CallState
import io.livekit.android.LiveKit
import io.livekit.android.annotations.Beta
import io.livekit.android.audio.AudioSwitchHandler
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import android.util.Log

private const val TAG = "LiveKitViewModel"

@OptIn(Beta::class)
class LiveKitViewModel(application: Application) : AndroidViewModel(application) {
    
    private var room: Room? = null
    private var durationJob: Job? = null
    private var eventsJob: Job? = null
    private var audioSwitchHandler: AudioSwitchHandler? = null
    
    private val _callInfo = MutableStateFlow(CallInfo())
    val callInfo: StateFlow<CallInfo> = _callInfo.asStateFlow()
    
    private val _agentSpeaking = MutableStateFlow(false)
    val agentSpeaking: StateFlow<Boolean> = _agentSpeaking.asStateFlow()
    
    private val _userSpeaking = MutableStateFlow(false)
    val userSpeaking: StateFlow<Boolean> = _userSpeaking.asStateFlow()
    
    private val _transcription = MutableStateFlow<String?>(null)
    val transcription: StateFlow<String?> = _transcription.asStateFlow()
    
    companion object {
        // Set to false for production
        const val DEMO_MODE = false
    }
    
    fun startCall() {
        if (_callInfo.value.state == CallState.CONNECTED ||
            _callInfo.value.state == CallState.CONNECTING) {
            return
        }
        
        viewModelScope.launch {
            _callInfo.update { it.copy(state = CallState.CONNECTING) }
            
            // Demo mode for testing UI without backend
            if (DEMO_MODE) {
                delay(1500) // Simulate connection delay
                _callInfo.update {
                    it.copy(
                        state = CallState.CONNECTED,
                        roomName = "demo-room"
                    )
                }
                startDurationTimer()
                // Simulate agent speaking
                delay(1000)
                _transcription.value = "مرحباً بك في زين الأردن. كيف يمكنني مساعدتك؟"
                _agentSpeaking.value = true
                delay(3000)
                _agentSpeaking.value = false
                return@launch
            }
            
            try {
                val roomName = "zain-voice-${UUID.randomUUID().toString().take(8)}"
                val identity = "user-${System.currentTimeMillis()}"
                
                // Get token from your server
                val tokenResponse = ApiClient.tokenApiService.getToken(
                    roomName = roomName,
                    identity = identity
                )
                
                if (tokenResponse.isSuccessful && tokenResponse.body() != null) {
                    val token = tokenResponse.body()!!
                    connectToRoom(
                        url = token.url ?: ApiClient.LIVEKIT_URL,
                        token = token.token,
                        roomName = roomName
                    )
                } else {
                    _callInfo.update {
                        it.copy(
                            state = CallState.ERROR,
                            errorMessage = "Failed to get connection token"
                        )
                    }
                }
            } catch (e: Exception) {
                _callInfo.update {
                    it.copy(
                        state = CallState.ERROR,
                        errorMessage = e.message ?: "Connection failed"
                    )
                }
            }
        }
    }
    
    fun startCallWithToken(url: String, token: String, roomName: String = "zain-voice-room") {
        if (_callInfo.value.state == CallState.CONNECTED ||
            _callInfo.value.state == CallState.CONNECTING) {
            return
        }
        
        viewModelScope.launch {
            _callInfo.update { it.copy(state = CallState.CONNECTING) }
            connectToRoom(url, token, roomName)
        }
    }
    
    private suspend fun connectToRoom(url: String, token: String, roomName: String) {
        try {
            room = LiveKit.create(appContext = getApplication())
            
            // Get LiveKit's AudioSwitchHandler and configure for speaker
            audioSwitchHandler = room?.audioHandler as? AudioSwitchHandler
            Log.d(TAG, "AudioHandler type: ${room?.audioHandler?.javaClass?.simpleName}")
            
            audioSwitchHandler?.let { handler ->
                // Start the audio handler first!
                handler.start()
                Log.d(TAG, "AudioSwitchHandler started")
                
                // Set preferred device list with Speakerphone first
                handler.preferredDeviceList = listOf(
                    AudioDevice.Speakerphone::class.java,
                    AudioDevice.WiredHeadset::class.java,
                    AudioDevice.BluetoothHeadset::class.java,
                    AudioDevice.Earpiece::class.java
                )
                Log.d(TAG, "Set preferred device list with Speakerphone first")
            }
            
            // Set audio mode and volume for voice call
            val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
            
            // Boost both VOICE_CALL and MUSIC streams (WebRTC may use either)
            val maxVoice = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVoice, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
            Log.d(TAG, "Audio mode MODE_IN_COMMUNICATION, speaker ON, volumes: voice=$maxVoice music=$maxMusic")
            
            // Listen to room events
            eventsJob = viewModelScope.launch {
                room?.events?.collect { event ->
                    handleRoomEvent(event)
                }
            }
            
            // Connect to the room
            room?.connect(url = url, token = token)
            
            // After connection, select speaker explicitly
            audioSwitchHandler?.let { handler ->
                Log.d(TAG, "Available audio devices: ${handler.availableAudioDevices}")
                val speakerDevice = handler.availableAudioDevices.firstOrNull { it is AudioDevice.Speakerphone }
                if (speakerDevice != null) {
                    handler.selectDevice(speakerDevice)
                    Log.d(TAG, "Selected speaker device: $speakerDevice")
                } else {
                    Log.w(TAG, "No speakerphone device found!")
                }
                Log.d(TAG, "Currently selected: ${handler.selectedAudioDevice}")
            }
            
            // Enable microphone for voice input
            room?.localParticipant?.setMicrophoneEnabled(true)
            Log.d(TAG, "Microphone enabled")
            
            _callInfo.update {
                it.copy(
                    state = CallState.CONNECTED,
                    roomName = roomName,
                    isSpeakerOn = true
                )
            }
            
            startDurationTimer()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to room", e)
            _callInfo.update {
                it.copy(
                    state = CallState.ERROR,
                    errorMessage = e.message ?: "Failed to connect"
                )
            }
        }
    }
    
    private fun handleRoomEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.Disconnected -> {
                _callInfo.update { it.copy(state = CallState.DISCONNECTED) }
                cleanup()
            }
            
            is RoomEvent.Reconnecting -> {
                _callInfo.update { it.copy(state = CallState.RECONNECTING) }
            }
            
            is RoomEvent.Reconnected -> {
                _callInfo.update { it.copy(state = CallState.CONNECTED) }
            }
            
            is RoomEvent.FailedToConnect -> {
                _callInfo.update {
                    it.copy(
                        state = CallState.ERROR,
                        errorMessage = "Failed to connect to room"
                    )
                }
            }
            
            // Track when agent (remote participant) is speaking
            is RoomEvent.TrackSubscribed -> {
                Log.d(TAG, "TrackSubscribed: kind=${event.track.kind}, sid=${event.track.sid}")
                if (event.track.kind == Track.Kind.AUDIO) {
                    Log.d(TAG, "Audio track subscribed - starting playback")
                    _agentSpeaking.value = true
                    // Enable audio playback for remote track with boosted volume
                    (event.track as? io.livekit.android.room.track.RemoteAudioTrack)?.let { audioTrack ->
                        Log.d(TAG, "Starting remote audio track")
                        // Boost track volume (1.0 = 100%; 2.0 = 200% for louder output)
                        audioTrack.setVolume(2.0)
                        Log.d(TAG, "Set remote audio track volume to 2.0")
                        audioTrack.start()
                    }
                }
            }
            
            is RoomEvent.TrackPublished -> {
                Log.d(TAG, "TrackPublished: participant=${event.participant.identity}, track=${event.publication.kind}")
            }
            
            is RoomEvent.TrackUnsubscribed -> {
                Log.d(TAG, "TrackUnsubscribed: kind=${event.track.kind}")
                if (event.track.kind == Track.Kind.AUDIO) {
                    _agentSpeaking.value = false
                }
            }
            
            // Handle active speakers - track both agent and user speaking states
            is RoomEvent.ActiveSpeakersChanged -> {
                Log.d(TAG, "ActiveSpeakersChanged: ${event.speakers.map { it.identity }}")
                val localId = room?.localParticipant?.identity
                val speakers = event.speakers
                
                val agentIsSpeaking = speakers.any { it.identity != localId }
                val userIsSpeaking = speakers.any { it.identity == localId }
                
                _agentSpeaking.value = agentIsSpeaking
                _userSpeaking.value = userIsSpeaking
            }
            
            else -> {}
        }
    }
    
    fun endCall() {
        viewModelScope.launch {
            _callInfo.update { it.copy(state = CallState.DISCONNECTING) }
            
            try {
                room?.disconnect()
            } catch (e: Exception) {
                // Ignore disconnect errors
            }
            cleanup()
            
            // Small delay to ensure UI updates
            delay(100)
            _callInfo.update { CallInfo(state = CallState.DISCONNECTED) }
        }
    }
    
    fun toggleMute() {
        viewModelScope.launch {
            val isMuted = _callInfo.value.isMuted
            room?.localParticipant?.setMicrophoneEnabled(isMuted) // Enable if was muted
            _callInfo.update { it.copy(isMuted = !isMuted) }
        }
    }
    
    fun toggleSpeaker() {
        val newSpeakerState = !_callInfo.value.isSpeakerOn
        _callInfo.update { it.copy(isSpeakerOn = newSpeakerState) }
        
        // Update AudioManager speaker state
        val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = newSpeakerState
        
        audioSwitchHandler?.let { handler ->
            if (newSpeakerState) {
                val speakerDevice = handler.availableAudioDevices.firstOrNull { it is AudioDevice.Speakerphone }
                speakerDevice?.let { handler.selectDevice(it) }
            } else {
                val earpieceDevice = handler.availableAudioDevices.firstOrNull { it is AudioDevice.Earpiece }
                earpieceDevice?.let { handler.selectDevice(it) }
            }
            Log.d(TAG, "Toggled speaker: $newSpeakerState, selected: ${handler.selectedAudioDevice}")
        }
    }
    
    private fun startDurationTimer() {
        durationJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _callInfo.update { it.copy(duration = it.duration + 1) }
            }
        }
    }
    
    private fun cleanup() {
        durationJob?.cancel()
        durationJob = null
        eventsJob?.cancel()
        eventsJob = null
        
        // Reset audio mode
        try {
            val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting audio", e)
        }
        
        audioSwitchHandler?.stop()
        audioSwitchHandler = null
        room = null
        _transcription.value = null
        _agentSpeaking.value = false
        _userSpeaking.value = false
    }
    
    fun resetError() {
        _callInfo.update { it.copy(state = CallState.IDLE, errorMessage = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        room?.disconnect()
        cleanup()
    }
}
