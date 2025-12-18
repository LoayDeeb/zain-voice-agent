"""
Simple token server for LiveKit.
Run this alongside your agent to generate tokens for mobile clients.
"""

import os
import time
import jwt
from flask import Flask, request, jsonify
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)

LIVEKIT_API_KEY = os.getenv("LIVEKIT_API_KEY")
LIVEKIT_API_SECRET = os.getenv("LIVEKIT_API_SECRET")
LIVEKIT_URL = os.getenv("LIVEKIT_URL")


@app.route("/api/token", methods=["GET"])
def get_token():
    room_name = request.args.get("room", "zain-voice-room")
    identity = request.args.get("identity", "user")
    
    if not LIVEKIT_API_KEY or not LIVEKIT_API_SECRET:
        return jsonify({"error": "LiveKit credentials not configured"}), 500
    
    now = int(time.time())
    exp = now + 86400  # 24 hours
    
    claims = {
        "iss": LIVEKIT_API_KEY,
        "sub": identity,
        "name": identity,
        "iat": now,
        "nbf": now,
        "exp": exp,
        "video": {
            "roomJoin": True,
            "room": room_name,
            "canPublish": True,
            "canSubscribe": True,
            "canPublishData": True,
        }
    }
    
    jwt_token = jwt.encode(claims, LIVEKIT_API_SECRET, algorithm="HS256")
    
    return jsonify({
        "token": jwt_token,
        "url": LIVEKIT_URL
    })


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    print(f"Token server starting...")
    print(f"LiveKit URL: {LIVEKIT_URL}")
    app.run(host="0.0.0.0", port=5000)
