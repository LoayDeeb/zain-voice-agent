# Zain Jordan Voice App

A voice assistant Android application for Zain Jordan using **LiveKit WebRTC** + **ElevenLabs TTS** + your AI agent.

## Features

- 🎙️ Real-time voice with LiveKit WebRTC
- 🗣️ High-quality Arabic TTS via ElevenLabs
- 🤖 AI Agent integration via AgenticBuilder API
- 🎨 Zain-branded purple/magenta theme
- 🌐 Bilingual support (Arabic & English)
- 📱 Modern Jetpack Compose UI
- ⚡ Low-latency voice interaction

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Android App   │────▶│   LiveKit Room  │◀────│   Zain Agent    │
│  (LiveKit SDK)  │     │   (WebRTC)      │     │  (Python)       │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                        │
                              ┌──────────────────────────────────────┐
                              │ Deepgram │  Your API  │  ElevenLabs  │
                              │  (STT)   │            │    (TTS)     │
                              └──────────────────────────────────────┘
```

### Project Structure

```
ZainApp/
├── app/                      # Android app
│   └── src/main/java/.../
│       ├── data/api/         # API clients
│       ├── ui/screens/       # Compose UI
│       ├── viewmodel/        # LiveKitViewModel
│       └── ...
└── agent/                    # Python server
    ├── zain_agent.py         # LiveKit Agent (ElevenLabs + your API)
    ├── token_server.py       # Token generator for mobile
    └── requirements.txt
```

## Quick Start

### 1. Setup the Agent Server

```bash
cd agent
pip install -r requirements.txt
cp .env.example .env
# Edit .env with your credentials
```

### 2. Run the Agent

```bash
# Terminal 1: Token server
python token_server.py

# Terminal 2: Voice agent
python zain_agent.py dev
```

### 3. Configure Android App

Edit `app/.../data/api/ApiClient.kt`:

```kotlin
const val LIVEKIT_URL = "wss://your-app.livekit.cloud"
private const val TOKEN_BASE_URL = "http://your-server:5000/"
```

### 4. Build & Run

Open in Android Studio and run on a device with API 24+.

## Required Credentials

| Service | Get from | Used for |
|---------|----------|----------|
| LiveKit | [cloud.livekit.io](https://cloud.livekit.io) | WebRTC infrastructure |
| ElevenLabs | [elevenlabs.io](https://elevenlabs.io/app/settings/api-keys) | Arabic TTS |
| Deepgram | [console.deepgram.com](https://console.deepgram.com) | Speech-to-Text |
| Your Agent API | Already configured | AI responses |

## Voice Flow

1. **User speaks** → LiveKit captures audio
2. **Deepgram STT** → Transcribes to text
3. **Your API** → `POST /api/agent/invoke` → AI response
4. **ElevenLabs TTS** → High-quality Arabic voice
5. **LiveKit** → Streams audio back to user

## Agent API Integration

Your AgenticBuilder API is called by the server agent:

```python
# agent/zain_agent.py
payload = {
    "agent_id": "14e9ebf0-ae34-4b21-8760-b0e3fe87275d",
    "message": user_text,
    "channel": "api",
    "persist_messages": True,
    "max_iterations": 30,
    "max_tool_iterations": 10
}
```

## Permissions (Android)

- `INTERNET` - Network access
- `RECORD_AUDIO` - Microphone for LiveKit

## License

Copyright © 2024 Zain Jordan. All rights reserved.
