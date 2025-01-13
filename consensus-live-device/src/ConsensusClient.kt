package lerpmusic.consensus.device

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import lerpmusic.consensus.DeviceRequest
import lerpmusic.consensus.DeviceResponse
import lerpmusic.consensus.Note
import lerpmusic.consensus.NoteEvent
import kotlin.time.Duration.Companion.seconds

class ConsensusClient(
    private val serverHost: String,
    private val sessionId: String,
    private val sessionPin: String,
    private val max: Max,
) {
    private val noteEvents: Flow<NoteEvent?> =
        max.inlet3("midiIn")
            .mapNotNull { (a, b, c) ->
                NoteEvent.fromRaw(
                    channel = a as? Int ?: return@mapNotNull null,
                    pitch = b as? Int ?: return@mapNotNull null,
                    velocity = c as? Int ?: return@mapNotNull null,
                )
            }

    private val delayedNoteOns = mutableMapOf<Note, NoteEvent.NoteOn>()

    private val client = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
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
            .onCompletion { max.post("Disconnected from the server") }
            .collect()
    }

    private suspend fun openServerSession() {
        openWebSocketSession {
            launch(start = CoroutineStart.ATOMIC) {
                while (true) {
                    receiveMessage()
                }
            }

            launch(start = CoroutineStart.ATOMIC) {
                for (ev in noteEvents.produceIn(this)) {
                    launch { processNoteEvent(ev) }
                }
            }

            max.outlet("status", "running")
        }
    }

    private suspend fun openWebSocketSession(
        block: suspend DefaultClientWebSocketSession.() -> Unit,
    ) {
        val url = buildUrl {
            protocol = if ("localhost" in serverHost) {
                URLProtocol.WS
            } else {
                URLProtocol.WSS
            }
            host = serverHost
            path("consensus", sessionId, "device", sessionPin)
        }.toString()

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
                    launch { play(delayedNote) }
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
                    launch { play(ev) }
                } else {
                    val msg = DeviceRequest.CancelNote(ev.note)
                    max.post("cancelled note ${ev.note}")
                    sendSerialized<DeviceRequest>(msg)
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
