import asyncio
import aiohttp
import json
import time
import os
import sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

from dotenv import load_dotenv
load_dotenv()

AGENT_API_URL = "https://agenticbuilder.onrender.com/api/agent/invoke/stream"
AGENT_API_KEY = os.getenv("AGENT_API_KEY", "")
AGENT_ID = os.getenv("AGENT_ID", "14e9ebf0-ae34-4b21-8760-b0e3fe87275d")

async def test_streaming(message: str):
    timeout = aiohttp.ClientTimeout(total=120)
    async with aiohttp.ClientSession(timeout=timeout) as session:
        headers = {
            "Content-Type": "application/json",
            "X-API-Key": AGENT_API_KEY,
            "Accept": "text/event-stream"
        }
        
        payload = {
            "agent_id": AGENT_ID,
            "message": message,
            "channel": "api",
            "persist_messages": False,
            "max_iterations": 10,
            "max_tool_iterations": 10
        }
        
        print(f"Testing: {message}")
        print(f"API: {AGENT_API_URL}")
        print("-" * 60)
        
        start_time = time.time()
        first_chunk_time = None
        full_response = ""
        all_events = []
        
        async with session.post(AGENT_API_URL, json=payload, headers=headers) as resp:
            print(f"Status: {resp.status}")
            
            if resp.status == 200:
                current_event = None
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
                            data = json.loads(data_str)
                            all_events.append({"event": current_event, "data": data})
                            
                            elapsed = (time.time() - start_time) * 1000
                            
                            if current_event == "init":
                                print(f"[{elapsed:.0f}ms] INIT: conversation_id={data.get('conversation_id', 'N/A')}")
                            elif current_event == "text_delta":
                                if first_chunk_time is None:
                                    first_chunk_time = time.time()
                                    print(f"\n[{elapsed:.0f}ms] ⏱️ FIRST TEXT CHUNK!")
                                text = data.get("text", "")
                                full_response += text
                                print(text, end="", flush=True)
                            elif current_event == "tool_call":
                                print(f"\n[{elapsed:.0f}ms] 🔧 TOOL CALL: {data}")
                            elif current_event == "tool_result":
                                print(f"\n[{elapsed:.0f}ms] 🔧 TOOL RESULT: {str(data)[:200]}...")
                            elif current_event == "done":
                                print(f"\n[{elapsed:.0f}ms] ✅ DONE: {data}")
                                break
                            else:
                                print(f"\n[{elapsed:.0f}ms] {current_event}: {str(data)[:100]}")
                        except json.JSONDecodeError as e:
                            print(f"JSON Error: {e}")
            else:
                error = await resp.text()
                print(f"Error: {error}")
        
        end_time = time.time()
        print("\n" + "=" * 60)
        print(f"FULL RESPONSE (what TTS would say):")
        print(full_response)
        print("=" * 60)
        print(f"⏱️ Total time: {(end_time - start_time)*1000:.0f}ms")
        if first_chunk_time:
            print(f"⏱️ Time to first text: {(first_chunk_time - start_time)*1000:.0f}ms")
        
        # Show all event types received
        event_types = set(e["event"] for e in all_events)
        print(f"Event types received: {event_types}")

if __name__ == "__main__":
    test_message = sys.argv[1] if len(sys.argv) > 1 else "بدي استفسر عن رصيدي 0791748785"
    asyncio.run(test_streaming(test_message))
