# Zain Voice Agent (LiveKit + ElevenLabs)

Server-side voice agent that handles STT, your API integration, and ElevenLabs TTS.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Android App   │────▶│   LiveKit Room  │◀────│   Zain Agent    │
│  (LiveKit SDK)  │     │   (WebRTC)      │     │  (Python)       │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                        │
                                                        ▼
                              ┌──────────────────────────────────────┐
                              │                                      │
                         ┌────┴────┐    ┌────────┐    ┌────────────┐ │
                         │ Deepgram│    │  Your  │    │ ElevenLabs │ │
                         │  (STT)  │    │  API   │    │   (TTS)    │ │
                         └─────────┘    └────────┘    └────────────┘ │
                              │                                      │
                              └──────────────────────────────────────┘
```

## Setup

### 1. Install dependencies

```bash
cd agent
pip install -r requirements.txt
```

### 2. Configure environment

Copy `.env.example` to `.env` and fill in your credentials:

```bash
cp .env.example .env
```

Required credentials:
- **LiveKit**: Get from [LiveKit Cloud](https://cloud.livekit.io)
- **ElevenLabs**: Get from [ElevenLabs](https://elevenlabs.io/app/settings/api-keys)
- **Deepgram**: Get from [Deepgram Console](https://console.deepgram.com)
- **Agent API**: Your AgenticBuilder API key

### 3. Run the token server

```bash
python token_server.py
```

This runs on `http://localhost:5000` and provides tokens for mobile clients.

### 4. Run the agent

```bash
python zain_agent.py dev
```

The agent will connect to LiveKit and wait for users to join.

## Files

| File | Description |
|------|-------------|
| `zain_agent.py` | Main agent - handles voice interaction |
| `token_server.py` | Generates LiveKit tokens for mobile app |
| `.env.example` | Environment variables template |

## Voice Configuration

### ElevenLabs Voice

The agent uses ElevenLabs for high-quality Arabic TTS. To change the voice:

```python
tts=elevenlabs.TTS(
    voice_id="YOUR_VOICE_ID",  # Change this
    model="eleven_multilingual_v2",
    language="ar",
)
```

Find voice IDs at [ElevenLabs Voice Library](https://elevenlabs.io/voice-library).

### STT Language

For Arabic recognition, Deepgram is configured with:

```python
stt=deepgram.STT(
    model="nova-2",
    language="ar",
)
```

## Deployment

### LiveKit Cloud

1. Go to [LiveKit Cloud](https://cloud.livekit.io)
2. Create a project
3. Deploy your agent using the LiveKit CLI:

```bash
lk cloud deploy
```

### Self-hosted

Run the agent on any server with Python 3.10+:

```bash
python zain_agent.py start
```

## Testing

1. Start the token server and agent
2. Update the Android app's `ApiClient.kt`:
   - Set `TOKEN_BASE_URL` to your token server
   - Set `LIVEKIT_URL` to your LiveKit URL
3. Run the Android app and start a call

## Troubleshooting

### Agent not responding
- Check that the agent is connected to LiveKit
- Verify your API key is correct
- Check Deepgram/ElevenLabs API keys

### Poor audio quality
- Ensure good network connection
- Try different ElevenLabs voices/models

### Arabic not recognized
- Deepgram's Arabic model may need clear speech
- Consider adding language detection for mixed Arabic/English
