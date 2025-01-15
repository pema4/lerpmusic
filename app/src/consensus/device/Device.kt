package lerpmusic.website.consensus.device

import io.ktor.server.plugins.callid.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lerpmusic.consensus.*
import lerpmusic.website.consensus.device.Device.Id
import mu.KotlinLogging

class Device(
    val sessionId: SessionId,
    private val deviceConnection: WebSocketServerSession,
) {
    suspend fun receiveRequest(): DeviceRequest =
        deviceConnection.receiveDeserialized()

    suspend fun playNote(note: Note) =
        respond(DeviceResponse.PlayNote(note))

    private suspend fun respond(response: DeviceResponse) {
        deviceConnection.sendSerialized(response)
//        log.debug { "Sent ListenerResponse $response to $this" }
    }

    private data class Id(
        val sessionId: SessionId,
        val callId: String?,
    )

    private val id = Id(sessionId, deviceConnection.call.callId)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Device

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Device($sessionId, ${id.callId})"
    }
}

private val log = KotlinLogging.logger {}

class Device2(
    val sessionId: SessionId,
    private val deviceConnection: WebSocketServerSession,
) : Composition {
    private val id = Id(sessionId, deviceConnection.call.callId)

    suspend fun receiveRequest(): DeviceRequest =
        deviceConnection.receiveDeserialized()

    override val events: Flow<NoteEvent> = flow {
        while (true) {
            when (val request = deviceConnection.receiveDeserialized<DeviceRequest>()) {
                is DeviceRequest.AskNote -> emit(NoteEvent.NoteOn(note = request.note, velocity = 0))
                is DeviceRequest.CancelNote -> {}
            }
        }
    }

    override suspend fun play(ev: NoteEvent) {
        deviceConnection.sendSerialized(DeviceResponse.PlayNote(ev.note))
    }
}

