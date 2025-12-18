package com.zain.jordan.voiceapp.ui.screens

import android.Manifest
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.zain.jordan.voiceapp.data.model.CallState
import com.zain.jordan.voiceapp.ui.theme.*
import com.zain.jordan.voiceapp.viewmodel.LiveKitViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LiveKitHomeScreen(
    viewModel: LiveKitViewModel,
    onNavigateToCall: () -> Unit
) {
    val callInfo by viewModel.callInfo.collectAsState()
    
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    val isConnecting = callInfo.state == CallState.CONNECTING
    
    LaunchedEffect(callInfo.state) {
        if (callInfo.state == CallState.CONNECTED) {
            onNavigateToCall()
        }
    }
    
    fun startCall() {
        if (audioPermissionState.status.isGranted) {
            viewModel.startCall()
        } else if (audioPermissionState.status.shouldShowRationale) {
            showPermissionDialog = true
        } else {
            audioPermissionState.launchPermissionRequest()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Z",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ZainPurple
                            )
                        }
                        Text(
                            text = "Zain Jordan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { startCall() },
                        enabled = !isConnecting
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = "Start Call",
                                tint = White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ZainPurple,
                    titleContentColor = White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ZainPurple.copy(alpha = 0.1f),
                            White
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // Main card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(ZainPurple, ZainMagenta)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SupportAgent,
                                contentDescription = "Assistant",
                                tint = White,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Zain Voice Assistant",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = ZainPurple
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "مرحباً بك في خدمة المساعد الصوتي",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Gray600,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Powered by ElevenLabs & LiveKit",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray400,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { startCall() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ZainPurple),
                            enabled = !isConnecting
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Connecting...")
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Call,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Start Voice Call",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Features
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.RecordVoiceOver,
                        text = "ElevenLabs Voice"
                    )
                    FeatureChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Wifi,
                        text = "Real-time"
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Language,
                        text = "Arabic Support"
                    )
                    FeatureChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.SmartToy,
                        text = "AI Powered"
                    )
                }
            }
            
            // Error snackbar
            if (callInfo.state == CallState.ERROR) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.resetError() }) {
                            Text("Dismiss", color = White)
                        }
                    },
                    containerColor = ErrorRed
                ) {
                    Text(callInfo.errorMessage ?: "Connection failed")
                }
            }
        }
    }
    
    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Microphone Permission") },
            text = { 
                Text("Microphone access is required for voice calls with the assistant.")
            },
            confirmButton = {
                TextButton(onClick = { 
                    showPermissionDialog = false
                    audioPermissionState.launchPermissionRequest()
                }) {
                    Text("Grant")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FeatureChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ZainPurple.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ZainPurple,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = ZainPurple,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
