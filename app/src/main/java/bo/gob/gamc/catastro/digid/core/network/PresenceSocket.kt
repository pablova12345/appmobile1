package bo.gob.gamc.catastro.digid.core.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Mantiene abierto un WebSocket de presencia mientras una pantalla de captura/escaneo
 * está activa, para que el ERP web detecte "celular conectado". No envía nada por el
 * socket: solo lo abre, escucha y reconecta con backoff si se cae.
 */
class PresenceSocket internal constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
    private val baseUrl: String,
    private val wsPath: String
) {
    private var scope: CoroutineScope? = null
    private var webSocket: WebSocket? = null
    private var active = false
    private var retryAttempt = 0

    fun start() {
        if (active) return
        active = true
        retryAttempt = 0
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        connect()
    }

    fun stop() {
        active = false
        webSocket?.close(1000, "screen_closed")
        webSocket = null
        scope?.cancel()
        scope = null
    }

    private fun connect() {
        if (!active) return
        val token = tokenManager.getAccessToken()
        if (token.isNullOrEmpty()) {
            scheduleRetry()
            return
        }

        val request = Request.Builder()
            .url(buildWsUrl(token))
            .header("User-Agent", MOBILE_USER_AGENT)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                retryAttempt = 0
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (active) scheduleRetry()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (active) scheduleRetry()
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "update" -> Log.d(TAG, "Evento 'update' recibido en $wsPath")
                "presence" -> Log.d(
                    TAG,
                    "Evento 'presence' recibido en $wsPath: mobile_connected=${json.optBoolean("mobile_connected")}"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Mensaje WS no reconocido en $wsPath: $text")
        }
    }

    private fun scheduleRetry() {
        val currentScope = scope ?: return
        val delaySeconds = min(MAX_BACKOFF_SECONDS, 2.0.pow(min(retryAttempt, 5)).toLong())
        retryAttempt++
        currentScope.launch {
            delay(delaySeconds * 1000L)
            if (active) connect()
        }
    }

    private fun buildWsUrl(token: String): String {
        val trimmed = baseUrl.trimEnd('/')
        val wsBase = when {
            trimmed.startsWith("https://") -> "wss://" + trimmed.removePrefix("https://")
            trimmed.startsWith("http://") -> "ws://" + trimmed.removePrefix("http://")
            else -> trimmed
        }
        val path = wsPath.trimStart('/')
        return "$wsBase/$path?token=$token"
    }

    companion object {
        private const val TAG = "PresenceSocket"
        private const val MAX_BACKOFF_SECONDS = 30L

        // El backend clasifica "es celular" con una regex sobre el User-Agent del handshake WS.
        private const val MOBILE_USER_AGENT = "IDEC-DigID-Android-App (Mobi; Android)"
    }
}

@Singleton
class PresenceSocketFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager
) {
    fun create(baseUrl: String, wsPath: String): PresenceSocket =
        PresenceSocket(okHttpClient, tokenManager, baseUrl, wsPath)
}
