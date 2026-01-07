import os
import logging
import aiohttp
import asyncio
import time
from dotenv import load_dotenv

from livekit import agents
from livekit.agents import AgentSession, Agent
from livekit.plugins import elevenlabs, silero

load_dotenv()

# Production logging - use INFO for app, WARNING for libraries
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logging.getLogger("livekit.agents").setLevel(logging.INFO)
logging.getLogger("livekit.plugins.elevenlabs").setLevel(logging.WARNING)
logging.getLogger("livekit.plugins.silero").setLevel(logging.WARNING)

AGENT_API_URL = os.getenv("AGENT_API_URL", "https://agenticbuilder.onrender.com/api/agent/invoke")
AGENT_API_KEY = os.getenv("AGENT_API_KEY", "")
AGENT_ID = os.getenv("AGENT_ID", "14e9ebf0-ae34-4b21-8760-b0e3fe87275d")

# Global aiohttp session for connection reuse (major latency improvement)
_http_session: aiohttp.ClientSession | None = None

async def get_http_session() -> aiohttp.ClientSession:
    """Get or create a reusable HTTP session with connection pooling."""
    global _http_session
    if _http_session is None or _http_session.closed:
        connector = aiohttp.TCPConnector(
            limit=10,
            keepalive_timeout=60,
            enable_cleanup_closed=True,
        )
        timeout = aiohttp.ClientTimeout(total=5, connect=3)  # Fast fail for voice
        _http_session = aiohttp.ClientSession(
            connector=connector,
            timeout=timeout,
        )
    return _http_session


async def keep_render_warm():
    """Ping Render services every 5 minutes to prevent cold starts."""
    while True:
        try:
            session = await get_http_session()
            # Ping the agent API health endpoint (or just the base URL)
            async with session.get(AGENT_API_URL.replace("/api/agent/invoke", "/health"), timeout=aiohttp.ClientTimeout(total=10)) as resp:
                logging.debug(f"Keep-alive ping: {resp.status}")
        except Exception as e:
            logging.debug(f"Keep-alive ping failed (non-critical): {e}")
        await asyncio.sleep(300)  # 5 minutes


class ZainAssistant(Agent):
    def __init__(self):
        super().__init__(
            instructions="""You are the Zain Jordan voice assistant. 
            You help customers with their telecom needs in Arabic and English.
            Be helpful, concise, and professional."""
        )
        self.session_id = None
        self.stt_start_time = None
        self.turn_start_time = None
    
    async def on_enter(self):
        # Greet the user when they join
        await self.session.say(
            "مرحباً بك في زين الأردن. كيف يمكنني مساعدتك اليوم؟",
            allow_interruptions=True
        )
    
    async def on_user_turn_completed(self, turn_ctx: agents.ChatContext, new_message: agents.ChatMessage):
        """Called when the user finishes speaking."""
        turn_start = time.time()
        
        # Calculate STT time (from when user started speaking to now)
        if self.stt_start_time:
            stt_duration = (turn_start - self.stt_start_time) * 1000
            logging.info(f"⏱️ STT completed in {stt_duration:.0f}ms")
        
        user_text = new_message.text_content
        logging.info(f"User said: {user_text}")
        
        if not user_text:
            logging.warning("No user text received")
            return
        
        # Build conversation history from the last 4 messages (reduced for lower latency)
        conversation_history = self._build_conversation_history(turn_ctx, max_messages=4)
        
        # Call your agent API with conversation history
        api_start = time.time()
        logging.info("Calling agent API...")
        response_text = await self._call_agent_api(user_text, conversation_history)
        api_duration = (time.time() - api_start) * 1000
        logging.info(f"⏱️ API call completed in {api_duration:.0f}ms")
        logging.info(f"API response: {response_text[:100]}...")
        
        # Speak the response
        tts_start = time.time()
        logging.info("Speaking response via TTS...")
        await self.session.say(response_text, allow_interruptions=True)
        tts_duration = (time.time() - tts_start) * 1000
        logging.info(f"⏱️ TTS completed in {tts_duration:.0f}ms")
        
        # Total turn time
        total_duration = (time.time() - turn_start) * 1000
        logging.info(f"⏱️ TOTAL turn time: {total_duration:.0f}ms (API: {api_duration:.0f}ms, TTS: {tts_duration:.0f}ms)")
    
    def _build_conversation_history(self, turn_ctx: agents.ChatContext, max_messages: int = 10) -> str:
        """Build a formatted string of the last N messages from conversation history."""
        messages = []
        
        # Get messages from chat context (excluding the current message which is handled separately)
        chat_messages = list(turn_ctx.items)[-max_messages:] if turn_ctx.items else []
        
        for msg in chat_messages:
            role = msg.role if hasattr(msg, 'role') else 'unknown'
            content = msg.text_content if hasattr(msg, 'text_content') else str(msg)
            
            if content:
                if role == 'user':
                    messages.append(f"User: {content}")
                elif role == 'assistant':
                    messages.append(f"Assistant: {content}")
                else:
                    messages.append(f"{role}: {content}")
        
        return "\n".join(messages) if messages else ""
    
    async def _call_agent_api(self, message: str, conversation_history: str = "") -> str:
        """Call the AgenticBuilder API to get a response."""
        try:
            session = await get_http_session()
            headers = {
                "Content-Type": "application/json",
                "X-API-Key": AGENT_API_KEY
            }
            
            # Include conversation history in the message if available
            if conversation_history:
                full_message = f"Conversation History:\n{conversation_history}\n\nCurrent message: {message}"
            else:
                full_message = message
            
            logging.info(f"Sending message with history: {full_message[:200]}...")
            
            payload = {
                "agent_id": AGENT_ID,
                "message": full_message,
                "channel": "api",
                "persist_messages": False,  # Faster for voice - no server-side history overhead
                "max_iterations": 10,
                "max_tool_iterations": 10
            }
            
            if self.session_id:
                payload["session_id"] = self.session_id
            
            async with session.post(AGENT_API_URL, json=payload, headers=headers) as resp:
                if resp.status == 200:
                    data = await resp.json()
                    # Store session ID for conversation continuity
                    if "session_id" in data:
                        self.session_id = data["session_id"]
                    return data.get("response") or data.get("message") or "عذراً، لم أتمكن من فهم طلبك."
                else:
                    error_text = await resp.text()
                    print(f"API Error {resp.status}: {error_text}")
                    return "عذراً، حدث خطأ في الخدمة. يرجى المحاولة مرة أخرى."
        
        except Exception as e:
            import traceback
            logging.error(f"Error calling agent API: {e}")
            logging.error(f"Full traceback: {traceback.format_exc()}")
            return "عذراً، حدث خطأ في الاتصال. يرجى المحاولة لاحقاً."


async def entrypoint(ctx: agents.JobContext):
    """Main entrypoint for the agent."""
    
    # Start keep-alive task to prevent Render cold starts
    asyncio.create_task(keep_render_warm())
    
    # Connect to the room
    await ctx.connect()
    
    # Log participants for debugging
    logging.info("Connected to room: %s", ctx.room.name)
    for p in ctx.room.remote_participants.values():
        logging.info(
            "Participant: sid=%s, identity=%s, audio_tracks=%d",
            p.sid, p.identity,
            len([t for t in p.track_publications.values() if t.kind.name == "KIND_AUDIO"])
        )
    
    # Create the agent session with ElevenLabs for both STT and TTS
    session = AgentSession(
        # Speech-to-Text: ElevenLabs Scribe - FORCE ARABIC to prevent Turkish/other language detection
        stt=elevenlabs.STT(),
        
        # Text-to-Speech: ElevenLabs with low-latency streaming settings
        tts=elevenlabs.TTS(
            voice_id="9enyNIN2oxpPh6N3QDbc",  # Custom Arabic voice
            model="eleven_turbo_v2_5",
            language="ar",
            streaming_latency=1,  # Small buffer for smoother audio on variable networks
        ),
        
        # Voice Activity Detection - balanced for quality and responsiveness
        vad=silero.VAD.load(
            min_speech_duration=0.25,  # 250ms of speech before considering it a turn
            min_silence_duration=0.4,   # 400ms of silence before ending the turn
        ),
    )
    
    # Create the agent instance
    agent = ZainAssistant()
    
    # Register event handlers for timing
    @session.on("user_started_speaking")
    def on_user_started_speaking():
        agent.stt_start_time = time.time()
        logging.info("🎤 User started speaking - STT timer started")
    
    # Start the agent
    await session.start(
        room=ctx.room,
        agent=agent,
    )


if __name__ == "__main__":
    # Run the agent worker
    agents.cli.run_app(
        agents.WorkerOptions(
            entrypoint_fnc=entrypoint,
        )
    )
