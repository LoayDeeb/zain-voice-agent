package com.zain.jordan.voiceapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zain.jordan.voiceapp.data.model.CallInfo
import com.zain.jordan.voiceapp.data.model.CallState
import com.zain.jordan.voiceapp.ui.theme.*
import com.zain.jordan.voiceapp.viewmodel.LiveKitViewModel

@Composable
fun LiveKitCallScreen(
    viewModel: LiveKitViewModel,
    onNavigateBack: () -> Unit
) {
    val callInfo by viewModel.callInfo.collectAsState()
    val agentSpeaking by viewModel.agentSpeaking.collectAsState()
    val userSpeaking by viewModel.userSpeaking.collectAsState()
    val transcription by viewModel.transcription.collectAsState()
    
    var showEndCallDialog by remember { mutableStateOf(false) }
    
    BackHandler {
        showEndCallDialog = true
    }
    
    LaunchedEffect(callInfo.state) {
        if (callInfo.state == CallState.IDLE || callInfo.state == CallState.DISCONNECTED) {
            onNavigateBack()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ZainPurple, ZainPurpleDark)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Status badge
            StatusBadge(
                state = callInfo.state,
                isMuted = callInfo.isMuted,
                agentSpeaking = agentSpeaking,
                userSpeaking = userSpeaking
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Animated avatar
            VoiceAvatar(
                agentSpeaking = agentSpeaking,
                isMuted = callInfo.isMuted,
                isConnected = callInfo.state == CallState.CONNECTED
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Agent name
            Text(
                text = callInfo.agentName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = White
            )
            
            // Duration
            Text(
                text = formatDuration(callInfo.duration),
                style = MaterialTheme.typography.titleMedium,
                color = White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Transcription card
            TranscriptionCard(
                transcription = transcription,
                agentSpeaking = agentSpeaking,
                isMuted = callInfo.isMuted,
                userSpeaking = userSpeaking
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Audio visualization
            if (callInfo.state == CallState.CONNECTED) {
                AudioWaveform(isActive = agentSpeaking || userSpeaking)
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Call controls
            CallControls(
                callInfo = callInfo,
                onMuteToggle = { viewModel.toggleMute() },
                onSpeakerToggle = { viewModel.toggleSpeaker() },
                onEndCall = { showEndCallDialog = true }
            )
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
    
    // End call dialog
    if (showEndCallDialog) {
        AlertDialog(
            onDismissRequest = { showEndCallDialog = false },
            title = { Text("End Call?") },
            text = { Text("Are you sure you want to end this call?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndCallDialog = false
                        viewModel.endCall()
                    }
                ) {
                    Text("End Call", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndCallDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusBadge(
    state: CallState,
    isMuted: Boolean,
    agentSpeaking: Boolean,
    userSpeaking: Boolean
) {
    val (text, color) = when {
        isMuted -> "Muted" to WarningOrange
        agentSpeaking -> "Agent Speaking" to InfoBlue
        userSpeaking -> "Listening..." to CallActiveGreen
        state == CallState.CONNECTED -> "Connected" to CallActiveGreen
        state == CallState.CONNECTING -> "Connecting" to CallConnectingYellow
        state == CallState.RECONNECTING -> "Reconnecting" to CallConnectingYellow
        else -> "" to White
    }
    
    if (text.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = color.copy(alpha = 0.2f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun VoiceAvatar(
    agentSpeaking: Boolean,
    isMuted: Boolean,
    isConnected: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (agentSpeaking) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (agentSpeaking) 0.1f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )
    
    Box(contentAlignment = Alignment.Center) {
        // Outer ring
        if (agentSpeaking && isConnected) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(InfoBlue.copy(alpha = ringAlpha))
            )
        }
        
        // Avatar
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(ZainMagenta, ZainPink)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    !isConnected -> Icons.Filled.WifiOff
                    isMuted -> Icons.Filled.MicOff
                    agentSpeaking -> Icons.Filled.VolumeUp
                    else -> Icons.Filled.SupportAgent
                },
                contentDescription = "Status",
                tint = White,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

@Composable
private fun TranscriptionCard(
    transcription: String?,
    agentSpeaking: Boolean,
    isMuted: Boolean,
    userSpeaking: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = White.copy(alpha = 0.15f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                !transcription.isNullOrBlank() -> {
                    Text(
                        text = transcription,
                        style = MaterialTheme.typography.bodyLarge,
                        color = White,
                        textAlign = TextAlign.Center
                    )
                }
                agentSpeaking -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniAudioBars()
                        Text(
                            text = "Agent is speaking...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = White
                        )
                    }
                }
                userSpeaking && !isMuted -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniAudioBars()
                        Text(
                            text = "Listening...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = White
                        )
                    }
                }
                isMuted -> {
                    Text(
                        text = "You are muted",
                        style = MaterialTheme.typography.bodyLarge,
                        color = WarningOrange
                    )
                }
                else -> {
                    Text(
                        text = "Speak to interact with the assistant",
                        style = MaterialTheme.typography.bodyMedium,
                        color = White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniAudioBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "bars")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = 6f,
                targetValue = 18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(200 + index * 50, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(InfoBlue)
            )
        }
    }
}

@Composable
private fun AudioWaveform(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(9) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = if (isActive) 8f else 4f,
                targetValue = if (isActive) 32f else 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 250 + (index * 50),
                        easing = EaseInOut
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave_$index"
            )
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(White.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
private fun CallControls(
    callInfo: CallInfo,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onEndCall: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute
        ControlButton(
            icon = if (callInfo.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            label = if (callInfo.isMuted) "Unmute" else "Mute",
            isActive = callInfo.isMuted,
            activeColor = WarningOrange,
            onClick = onMuteToggle
        )
        
        // End call
        FloatingActionButton(
            onClick = onEndCall,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            containerColor = CallEndRed
        ) {
            Icon(
                imageVector = Icons.Filled.CallEnd,
                contentDescription = "End Call",
                tint = White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Speaker
        ControlButton(
            icon = if (callInfo.isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeDown,
            label = "Speaker",
            isActive = callInfo.isSpeakerOn,
            onClick = onSpeakerToggle
        )
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: androidx.compose.ui.graphics.Color = White,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isActive) activeColor else White.copy(alpha = 0.2f),
                contentColor = if (isActive) ZainPurple else White
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = White.copy(alpha = 0.8f)
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
