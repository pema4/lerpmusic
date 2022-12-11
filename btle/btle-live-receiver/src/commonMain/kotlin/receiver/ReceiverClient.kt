package lerpmusic.btle.receiver

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.plugins.websocket.wss
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlinx.serialization.json.Json
import lerpmusic.btle.domain.receiver.ReceiverResponse
import kotlin.coroutines.cancellation.CancellationException

class ReceiverClient(
    private val serverHost: String,
    private val sessionId: String,
    private val bucketStart: Int,
    private val bucketLength: Int,
    private val max: Max,
) {
    private val activeChannels = mutableSetOf<Int>()

    suspend fun start() {
        try {
            connectToServer()
        } catch (ex: CancellationException) {
            Max.post("Disconnected from the server")
            throw ex
        } catch (ex: Throwable) {
            ex.printStackTrace()
            Max.post("Got exception $ex")
            throw ex
        }
    }

    private suspend fun connectToServer() {
        openWebSocketSession {
            while (true) {
                val ev = receiveDeserialized<ReceiverResponse>()

                val channel = ev.bucket - bucketStart + 2

                when (ev) {
                    is ReceiverResponse.FoundPeripheral -> {
                        max.outlet("mpe", "pressure", channel, ev.rssi)
                        if (channel !in activeChannels) {
                            max.outlet("mpe", "on", channel)
                            activeChannels += channel
                        }
                    }

                    is ReceiverResponse.LostPeripheral -> {
                        max.outlet("mpe", "off", channel)
                        activeChannels.remove(channel)
                    }
                }
            }
        }
    }

    private suspend fun openWebSocketSession(
        block: suspend DefaultClientWebSocketSession.() -> Unit,
    ) {
        val url = URLBuilder()
            .apply {
                protocol = if ("localhost" in serverHost) {
                    URLProtocol.WS
                } else {
                    URLProtocol.WSS
                }
                host = serverHost
                path("btle", sessionId, "receiver", "$bucketStart", "$bucketLength")
            }
            .buildString()

        max.post("Opening websocket connection to $url")
        try {
            if ("localhost" in serverHost) {
                client.ws(url) { block() }
            } else {
                client.wss(url) { block() }
            }
        } finally {
            max.post("Closing websocket connection to $url")
        }
    }

}

private val client = HttpClient {
    install(WebSockets) {
        maxFrameSize = Long.MAX_VALUE
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
}
