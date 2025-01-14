package lerpmusic.consensus.device

import arrow.continuations.SuspendApp
import arrow.core.raise.nullable
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import lerpmusic.consensus.Consensus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val max: Max = Max

private val httpClient = HttpClient {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        pingInterval = 15.seconds
    }
}

fun main() = SuspendApp {
    val serverHost: Flow<String?> = max.inlet("serverHost")
        .map { it.toString().takeIf { it.isNotBlank() } }

    val sessionId: Flow<String?> = max.inlet("sessionId")
        .map { it.toString().takeIf { it.isNotBlank() } }

    val sessionPin: Flow<String?> = max.inlet("sessionPin")
        .map { it.toString().takeIf { it.isNotBlank() } }

    val configuration = combine(serverHost, sessionId, sessionPin) { serverHost, sessionId, sessionPin ->
        nullable {
            ServerConnectionConfig(
                host = serverHost.bind(),
                sessionId = sessionId.bind(),
                sessionPin = sessionPin.bind(),
            )
        }
    }

    configuration
        .distinctUntilChanged()
        .onStart { max.outlet("status", "initialized") }
        .collectLatest { configuration ->
            max.post("Restarting with configuration: $configuration")
            if (configuration == null) return@collectLatest
            withRetries {
                openServerConnection(configuration) { serverConnection ->
                    val consensus = Consensus(
                        composition = DeviceComposition(max),
                        audience = DeviceAudience(serverConnection, max)
                    )
                    consensus.run()
                }
            }
        }
}

private suspend fun withRetries(
    retryAfter: Duration = 2.seconds,
    block: suspend CoroutineScope.() -> Unit,
) {
    flow<Nothing> { coroutineScope { block() } }
        .catch { ex ->
            max.outlet("status", "stopped")
            max.post("Got exception $ex")
            ex.printStackTrace()
            throw ex
        }
        .retry { delay(retryAfter); true }
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

private suspend fun openServerConnection(
    config: ServerConnectionConfig,
    block: suspend CoroutineScope.(serverConnection: DefaultClientWebSocketSession) -> Unit,
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
                coroutineScope { block(this@wss) }
            }
        } else {
            httpClient.ws(url.toString()) {
                coroutineScope { block(this@ws) }
            }
        }
    } finally {
        max.post("Closing websocket connection to $url")
    }
}
