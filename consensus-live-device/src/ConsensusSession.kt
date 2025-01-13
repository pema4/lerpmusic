package lerpmusic.consensus.device

import arrow.core.raise.nullable
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import lerpmusic.consensus.DeviceRequest
import lerpmusic.consensus.DeviceResponse
import lerpmusic.consensus.Note
import lerpmusic.consensus.NoteEvent
import kotlin.time.Duration.Companion.seconds

class ConsensusSession(
    private val serverHost: String,
    private val sessionId: String,
    private val sessionPin: String,
    private val max: Max,
) {
    private val noteEvents: Flow<NoteEvent?> =
        max.inlet3("midiin")
            .mapNotNull { (a, b, c) ->
                nullable {
                    NoteEvent.fromRaw(
                        channel = (a as? Int).bind(),
                        pitch = (b as? Int).bind(),
                        velocity = (c as? Int).bind(),
                    )
                }
            }

    private val delayedNoteOns = mutableMapOf<Note, NoteEvent.NoteOn>()

    private val client = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
            pingInterval = 15.seconds
        }
    }

    suspend fun run() {
        flow<Nothing> { openServerSession() }
            .catch { ex ->
                max.outlet("status", "stopped")
                max.post("Got exception $ex")
                ex.printStackTrace()
                throw ex
            }
            .retry { delay(2.seconds); true }
            .onCompletion {
                withContext(NonCancellable) {
                    max.outlet("status", "stopped")
                    max.post("Disconnected from the server")
                }
            }
            .collect()
    }

    private suspend fun openServerSession() {
        openWebSocketSession { ws ->
            launch(start = CoroutineStart.ATOMIC) {
                while (true) {
                    receiveMessage(ws)
                }
            }

            launch(start = CoroutineStart.ATOMIC) {
                for (ev in noteEvents.produceIn(this)) {
                    launch { processNoteEvent(ws, ev) }
                }
            }

            max.outlet("status", "running")
        }
    }

    private suspend fun openWebSocketSession(
        block: suspend CoroutineScope.(ws: DefaultClientWebSocketSession) -> Unit,
    ) {
        val url = buildUrl {
            protocol = if ("localhost" in serverHost) {
                URLProtocol.WS
            } else {
                URLProtocol.WSS
            }
            host = serverHost
            path("consensus", sessionId, "device", sessionPin)
        }

        max.post("Opening websocket connection to $url")
        try {
            if (url.protocol == URLProtocol.WSS) {
                client.wss(url.toString()) { coroutineScope { block(this@wss) } }
            } else {
                client.ws(url.toString()) { coroutineScope { block(this@ws) } }
            }
        } finally {
            max.post("Closing websocket connection to $url")
        }
    }

    private suspend fun CoroutineScope.receiveMessage(ws: DefaultClientWebSocketSession) {
        when (val event = ws.receiveDeserialized<DeviceResponse>()) {
            is DeviceResponse.PlayNote -> {
                val delayedNote = delayedNoteOns.remove(event.note)
                if (delayedNote != null) {
                    launch { play(delayedNote) }
                }
            }
        }
    }

    private suspend fun CoroutineScope.processNoteEvent(
        ws: DefaultClientWebSocketSession,
        event: NoteEvent?,
    ) {
        max.post("Got midi event $event")

        when (event) {
            null -> return

            is NoteEvent.NoteOn -> {
                if (delayedNoteOns.put(event.note, event) == null) {
                    val msg = DeviceRequest.AskNote(event.note)
                    ws.sendSerialized<DeviceRequest>(msg)
                }
            }

            is NoteEvent.NoteOff -> {
                // note on для этой ноты не проигрывался
                if (delayedNoteOns.remove(event.note) == null) {
                    launch { play(event) }
                } else {
                    val msg = DeviceRequest.CancelNote(event.note)
                    max.post("cancelled note ${event.note}")
                    ws.sendSerialized<DeviceRequest>(msg)
                }
            }
        }
    }

    private suspend fun play(ev: NoteEvent) {
        when (ev) {
            is NoteEvent.NoteOn ->
                max.outlet("midiOut", ev.note.channel, ev.note.pitch, ev.velocity)

            is NoteEvent.NoteOff ->
                max.outlet("midiOut", ev.note.channel, ev.note.pitch, 0)
        }
    }
}
