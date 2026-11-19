package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicInteger

enum class RealtimeConnectionState {
    DISCONNECTED,
    CONNECTING,
    SUBSCRIBED,
}

class SupabaseRealtimeClient(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String,
    private val apiKey: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refCounter = AtomicInteger(1)

    private val _state = MutableStateFlow(RealtimeConnectionState.DISCONNECTED)
    val state: StateFlow<RealtimeConnectionState> = _state.asStateFlow()

    private val _tableChangeEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val tableChangeEvents: SharedFlow<String> = _tableChangeEvents.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var isStopped = false

    fun start() {
        if (apiKey.isBlank()) return
        isStopped = false
        connect()
    }

    fun stop() {
        isStopped = true
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Client closed")
        webSocket = null
        _state.value = RealtimeConnectionState.DISCONNECTED
    }

    private fun connect() {
        if (isStopped) return
        webSocket?.cancel()
        _state.value = RealtimeConnectionState.CONNECTING

        val wsBase = baseUrl
            .trimEnd('/')
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
        val encodedKey = URLEncoder.encode(apiKey, "UTF-8")
        val wsUrl = "$wsBase/realtime/v1/websocket?apikey=$encodedKey&vsn=1.0.0"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                sendJoinChannel(webSocket)
                startHeartbeat(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _state.value = RealtimeConnectionState.DISCONNECTED
                heartbeatJob?.cancel()
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Supabase Realtime WebSocket error: ${t.message}")
                _state.value = RealtimeConnectionState.DISCONNECTED
                heartbeatJob?.cancel()
                scheduleReconnect()
            }
        })
    }

    private fun sendJoinChannel(ws: WebSocket) {
        val vehiclesSub = JSONObject()
            .put("event", "*")
            .put("schema", "public")
            .put("table", "vehicles")
        val bookingsSub = JSONObject()
            .put("event", "*")
            .put("schema", "public")
            .put("table", "bookings")

        val postgresChanges = JSONArray()
            .put(vehiclesSub)
            .put(bookingsSub)

        val config = JSONObject()
            .put("broadcast", JSONObject().put("self", false))
            .put("presence", JSONObject().put("key", ""))
            .put("postgres_changes", postgresChanges)

        val joinFrame = JSONObject()
            .put("topic", CHANNEL_TOPIC)
            .put("event", "phx_join")
            .put("payload", JSONObject().put("config", config))
            .put("ref", refCounter.getAndIncrement().toString())

        ws.send(joinFrame.toString())
    }

    private fun startHeartbeat(ws: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && !isStopped) {
                delay(25_000L)
                val hb = JSONObject()
                    .put("topic", "phoenix")
                    .put("event", "heartbeat")
                    .put("payload", JSONObject())
                    .put("ref", refCounter.getAndIncrement().toString())
                ws.send(hb.toString())
            }
        }
    }

    private fun handleIncomingMessage(raw: String) {
        runCatching {
            val json = JSONObject(raw)
            val event = json.optString("event")
            val topic = json.optString("topic")
            when {
                event == "phx_reply" && topic == CHANNEL_TOPIC -> {
                    val status = json.optJSONObject("payload")?.optString("status")
                    if (status == "ok") {
                        _state.value = RealtimeConnectionState.SUBSCRIBED
                    }
                }
                event == "postgres_changes" -> {
                    val table = json.optJSONObject("payload")
                        ?.optJSONObject("data")
                        ?.optString("table")
                        ?: "fleet"
                    _tableChangeEvents.tryEmit(table)
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (isStopped) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(8_000L)
            if (!isStopped) {
                connect()
            }
        }
    }

    companion object {
        private const val TAG = "SupabaseRealtime"
        private const val CHANNEL_TOPIC = "realtime:idle_fleet_sync"
    }
}
