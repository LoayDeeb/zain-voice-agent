import os
import logging
import aiohttp
import asyncio
import time
from dotenv import load_dotenv

from livekit import agents
from livekit.agents import AgentSession, Agent, room_io
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

AGENT_API_URL = os.getenv("AGENT_API_URL", "https://agenticbuilder.onrender.com/api/agent/invoke/stream")
AGENT_API_KEY = os.getenv("AGENT_API_KEY", "")
AGENT_ID = os.getenv("AGENT_ID", "9285dd53-6d2e-4c43-aaf3-a6ac2ce21e50")

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
        timeout = aiohttp.ClientTimeout(total=60, connect=10)
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
            async with session.get(AGENT_API_URL.replace("/api/agent/invoke/stream", "/health"), timeout=aiohttp.ClientTimeout(total=10)) as resp:
                logging.debug(f"Keep-alive ping: {resp.status}")
        except Exception as e:
            logging.debug(f"Keep-alive ping failed (non-critical): {e}")
        await asyncio.sleep(300)


class ZainAssistant(Agent):
    def __init__(self):
        super().__init__(
            instructions="""You are the Zain Jordan voice assistant. 
            You help customers with their telecom needs in Arabic and English.
            Be helpful, concise, and professional."""
        )
        self.api_session_id = None
    
    async def on_enter(self):
        await self.session.say(
            "مرحباً بك في زين الأردن. كيف يمكنني مساعدتك اليوم؟",
            allow_interruptions=True
        )
    
    async def on_user_turn_completed(self, turn_ctx: agents.ChatContext, new_message: agents.ChatMessage):
        """Called when the user finishes speaking - we handle the response manually."""
        import json as json_module
        
        turn_start = time.time()
        user_text = new_message.text_content
        
        logging.info(f"User said: {user_text}")
        
        if not user_text:
            logging.warning("No user text received")
            return
        
        conversation_history = self._build_conversation_history(turn_ctx)
        
        logging.info("Starting streaming API call...")
        
        async def stream_response():
            """Async generator that streams text from the API."""
            try:
                session = await get_http_session()
                headers = {
                    "Content-Type": "application/json",
                    "X-API-Key": AGENT_API_KEY,
                    "Accept": "text/event-stream"
                }
                
                if conversation_history:
                    full_message = f"Conversation History:\n{conversation_history}\n\nCurrent message: {user_text}"
                else:
                    full_message = user_text
                
                logging.info(f"Streaming request: {full_message[:200]}...")
                
                payload = {
                    "agent_id": AGENT_ID,
                    "message": full_message,
                    "channel": "api",
                    "persist_messages": False,
                    "max_iterations": 10,
                    "max_tool_iterations": 10
                }
                
                if self.api_session_id:
                    payload["session_id"] = self.api_session_id
                
                first_chunk = True
                current_event = None
                async with session.post(AGENT_API_URL, json=payload, headers=headers) as resp:
                    if resp.status == 200:
                        async for line in resp.content:
                            line = line.decode('utf-8').strip()
                            if not line:
                                continue
                            
                            if line.startswith("event: "):
                                current_event = line[7:]
                                continue
                            
                            if line.startswith("data: "):
                                data_str = line[6:]
                                try:
                                    data = json_module.loads(data_str)
                                    
                                    if current_event == "init":
                                        if "conversation_id" in data:
                                            self.api_session_id = data["conversation_id"]
                                    elif current_event == "text_delta":
                                        if "text" in data:
                                            if first_chunk:
                                                logging.info("⏱️ First chunk received - starting TTS")
                                                first_chunk = False
                                            yield data["text"]
                                    elif current_event == "done":
                                        logging.info("Stream complete")
                                        return
                                except json_module.JSONDecodeError:
                                    continue
                    else:
                        error_text = await resp.text()
                        logging.error(f"API Error {resp.status}: {error_text}")
                        yield "عذراً، حدث خطأ في الخدمة."
            
            except Exception as e:
                import traceback
                logging.error(f"Error streaming agent API: {e}")
                logging.error(f"Full traceback: {traceback.format_exc()}")
                yield "عذراً، حدث خطأ في الاتصال."
        
        try:
            await self.session.say(stream_response(), allow_interruptions=True)
        except Exception as e:
            logging.warning(f"TTS interrupted or failed: {e}")
        
        total_duration = (time.time() - turn_start) * 1000
        logging.info(f"⏱️ TOTAL turn time: {total_duration:.0f}ms")
    
    def _build_conversation_history(self, turn_ctx: agents.ChatContext, max_messages: int = 4) -> str:
        """Build a formatted string of the last N messages from conversation history."""
        messages = []
        chat_messages = list(turn_ctx.items)[-max_messages:] if turn_ctx.items else []
        
        for msg in chat_messages:
            role = msg.role if hasattr(msg, 'role') else 'unknown'
            content = msg.text_content if hasattr(msg, 'text_content') else str(msg)
            
            if content:
                if role == 'user':
                    messages.append(f"User: {content}")
                elif role == 'assistant':
                    messages.append(f"Assistant: {content}")
        
        return "\n".join(messages) if messages else ""


async def entrypoint(ctx: agents.JobContext):
    """Main entrypoint for the agent."""
    
    asyncio.create_task(keep_render_warm())
    
    await ctx.connect()
    
    logging.info("Connected to room: %s", ctx.room.name)
    for p in ctx.room.remote_participants.values():
        logging.info(
            "Participant: sid=%s, identity=%s, audio_tracks=%d",
            p.sid, p.identity,
            len([t for t in p.track_publications.values() if t.kind.name == "KIND_AUDIO"])
        )
    
    session = AgentSession(
        stt=elevenlabs.STT(
            language_code="ar",
        ),
        tts=elevenlabs.TTS(
            voice_id="9enyNIN2oxpPh6N3QDbc",
            model="eleven_turbo_v2_5",
            language="ar",
            inactivity_timeout=180,
        ),
        vad=silero.VAD.load(
            min_speech_duration=0.25,
            min_silence_duration=0.4,
        ),
        # No LLM - we handle responses in on_user_turn_completed
        llm=None,
    )
    
    agent = ZainAssistant()
    
    await session.start(
        room=ctx.room,
        agent=agent,
        room_options=room_io.RoomOptions(
            close_on_disconnect=False,
        ),
    )


if __name__ == "__main__":
    agents.cli.run_app(
        agents.WorkerOptions(
            entrypoint_fnc=entrypoint,
        )
    )
