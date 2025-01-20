package lerpmusic.consensus.device

import arrow.continuations.SuspendApp
import arrow.core.raise.nullable
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.*
import io.ktor.http.URLProtocol
import io.ktor.http.buildUrl
import io.ktor.http.path
import io.ktor.serialization.WebsocketDeserializeException
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.Frame
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import lerpmusic.consensus.DeviceRequest
import lerpmusic.consensus.DeviceResponse
import lerpmusic.consensus.launchConsensus
import lerpmusic.consensus.utils.WebSocketConnection
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val max: Max = Max

private val httpClient = HttpClient {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        // Как будто бы не работает в Kotlin/JS
        pingInterval = 15.seconds
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun main() = SuspendApp {
    // inlet() sends null before emitting actual messages
    val serverHost: Flow<String?> = max.inlet("serverHost")
        .map { it?.toString()?.takeIf { it.isNotBlank() } }
        .stateIn(this)

    val sessionId: Flow<String?> = max.inlet("sessionId")
        .map { it?.toString()?.takeIf { it.isNotBlank() } }
        .stateIn(this)

    val sessionPin: Flow<String?> = max.inlet("sessionPin")
        .map { it?.toString()?.takeIf { it.isNotBlank() } }
        .stateIn(this)

    val isIntensityRequested: Flow<Boolean> = max.inlet("intensity")
        .map { it?.toString()?.toInt() ?: 0 }
        .map { it != 0 }
        .stateIn(this)

    val isListenersCountRequested: Flow<Boolean> = max.inlet("listeners")
        .map { it?.toString()?.toInt() ?: 0 }
        .map { it != 0 }
        .stateIn(this)

    val configuration: Flow<ServerConnectionConfig?> =
        combine(serverHost, sessionId, sessionPin) { serverHost, sessionId, sessionPin ->
            nullable {
                ServerConnectionConfig(
                    host = serverHost.bind(),
                    sessionId = sessionId.bind(),
                    sessionPin = sessionPin.bind(),
                )
            }
        }

    configuration
        .onStart { max.outlet("status", "initialized") }
        .collectLatest { configuration ->
            max.post("Restarting with configuration: $configuration")
            if (configuration == null) return@collectLatest

            while (true) {
                withRetries {
                    openServerConnection(configuration) { serverConnection ->
                        launchConsensus(
                            composition = DeviceComposition(max, isIntensityRequested, isListenersCountRequested),
                            audience = DeviceAudience(serverConnection, max)
                        )

                        max.outlet("status", "running")
                    }
                }
                delay(RETRY_TIMEOUT)
            }
        }
}

private suspend fun withRetries(
    retryAfter: Duration = RETRY_TIMEOUT,
    block: suspend CoroutineScope.() -> Unit,
) {
    return flow { emit(coroutineScope { block() }) }
        .retryWhen { ex, attempts ->
            max.outlet("status", "stopped")
            max.post("Got exception $ex on attempt $attempts")
            ex.printStackTrace()
            delay(retryAfter)
            true
        }
        .onCompletion {
            withContext(NonCancellable) {
                max.outlet("status", "stopped")
                max.post("Disconnected from the server")
            }
        }
        .collect()
}

data class ServerConnectionConfig(
    val host: String,
    val sessionId: String,
    val sessionPin: String,
)

class ServerConnection(
    private val webSocketSession: DefaultClientWebSocketSession,
    private val scope: CoroutineScope,
) : WebSocketConnection<DeviceResponse, DeviceRequest>, CoroutineScope by scope {
    override val incoming: Flow<DeviceResponse> = flow {
        while (true) {
            emit(webSocketSession.receiveDeserialized())
        }
    }

    override suspend fun send(data: DeviceRequest): Unit = webSocketSession.sendSerialized(data)
}

private suspend fun openServerConnection(
    config: ServerConnectionConfig,
    block: suspend CoroutineScope.(serverConnection: ServerConnection) -> Unit,
) {
    val url = buildUrl {
        protocol = if ("localhost" in config.host) {
            URLProtocol.WS
        } else {
            URLProtocol.WSS
        }
        host = config.host
        path("consensus", config.sessionId, "device", config.sessionPin)
    }

    max.post("Opening websocket connection to $url")
    try {
        if (url.protocol == URLProtocol.WSS) {
            httpClient.wss(url.toString()) {
                try {
                    coroutineScope { block(ServerConnection(this@wss, this)) }
                } catch (ex: WebsocketDeserializeException) {
                    if (ex.frame !is Frame.Close) throw ex
                }
            }
        } else {
            httpClient.ws(url.toString()) {
                try {
                    coroutineScope { block(ServerConnection(this@ws, this)) }

                } catch (ex: WebsocketDeserializeException) {
                    if (ex.frame !is Frame.Close) throw ex
                }
            }
        }
    } finally {
        max.post("Closing websocket connection to $url")
    }
}

private val RETRY_TIMEOUT = 2.seconds