package lerpmusic.consensus.device

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.plugins.websocket.wss
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import io.ktor.http.path
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import lerpmusic.consensus.DeviceRequest
import lerpmusic.consensus.DeviceResponse
import lerpmusic.consensus.Note
import lerpmusic.consensus.NoteEvent
import kotlin.coroutines.cancellation.CancellationException

class ConsensusClient(
    private val serverHost: String,
    private val sessionId: String,
    private val sessionPin: String,
    private val noteEvents: ReceiveChannel<NoteEvent?>,
    private val play: (NoteEvent) -> Unit,
    httpClientEngineFactory: HttpClientEngineFactory<*>,
    private val max: Max,
) {
    private val delayedNoteOns = mutableMapOf<Note, NoteEvent.NoteOn>()

    private val client = HttpClient(httpClientEngineFactory) {
        install(WebSockets) {
            maxFrameSize = Long.MAX_VALUE
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }

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
//        testGetRequest()
        openWebSocketSession {
            coroutineScope {
                launch {
                    while (true) {
                        receiveMessage()
                    }
                }

                for (ev in noteEvents) {
                    launch { processNoteEvent(ev) }
                }
            }
        }
    }

    private suspend fun testGetRequest() {
        val url = URLBuilder()
            .apply {
                protocol = URLProtocol.HTTP
                host = serverHost
                encodedPath = "/test"
            }
            .buildString()

        max.post("Test url is $url")

        try {
            val responseText = client.get(url).bodyAsText()
            max.post("got test response $responseText")
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Throwable) {
            ex.printStackTrace()
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
                path("consensus", sessionId, "device", sessionPin)
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

    private suspend fun DefaultClientWebSocketSession.receiveMessage() {
        when (val ev = receiveDeserialized<DeviceResponse>()) {
            is DeviceResponse.PlayNote -> {
                val delayedNote = delayedNoteOns.remove(ev.note)
                if (delayedNote != null) {
                    play(delayedNote)
                }
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.processNoteEvent(
        ev: NoteEvent?,
    ) {
        max.post("Got midi event $ev")

        when (ev) {
            null -> return

            is NoteEvent.NoteOn -> {
                if (delayedNoteOns.put(ev.note, ev) == null) {
                    val msg = DeviceRequest.AskNote(ev.note)
                    sendSerialized<DeviceRequest>(msg)
                }
            }

            is NoteEvent.NoteOff -> {
                // note on для этой ноты не проигрывался
                if (delayedNoteOns.remove(ev.note) == null) {
                    play(ev)
                } else {
                    val msg = DeviceRequest.CancelNote(ev.note)
                    max.post("cancelled note ${ev.note}")
                    sendSerialized<DeviceRequest>(msg)
                }
            }
        }
    }
}
