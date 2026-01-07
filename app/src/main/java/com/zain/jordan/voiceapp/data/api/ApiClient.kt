package com.zain.jordan.voiceapp.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    
    private const val AGENT_BASE_URL = "https://agenticbuilder.onrender.com/"
    
    // Token server URL (deployed on Render)
    private const val TOKEN_BASE_URL = "https://zain-voice-agent.onrender.com/"
    
    // Agent API config
    const val AGENT_API_KEY = "CVpVXr60LRSVYbnzJYY_TxpULjKJb-5pwyc-U70I"
    const val AGENT_ID = "14e9ebf0-ae34-4b21-8760-b0e3fe87275d"
    
    // LiveKit config
    const val LIVEKIT_URL = "wss://loay-7v7v9o6l.livekit.cloud"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC  // Don't log bodies (contains tokens)
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val agentRetrofit = Retrofit.Builder()
        .baseUrl(AGENT_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val tokenRetrofit = Retrofit.Builder()
        .baseUrl(TOKEN_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val agentApiService: AgentApiService = agentRetrofit.create(AgentApiService::class.java)
    val tokenApiService: TokenApiService = tokenRetrofit.create(TokenApiService::class.java)
}
