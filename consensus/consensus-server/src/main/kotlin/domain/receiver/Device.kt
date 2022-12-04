package lerpmusic.consensus.domain.device

import io.ktor.server.plugins.callid.callId
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import kotlinx.coroutines.coroutineScope
import lerpmusic.consensus.domain.note.Note
import lerpmusic.consensus.domain.session.ConsensusService
import lerpmusic.consensus.domain.session.SessionId
import mu.KotlinLogging

class Device(
    val sessionId: SessionId,
    private val wsSession: WebSocketServerSession,
) {
    suspend fun receiveRequest(): DeviceRequest =
        wsSession.receiveDeserialized()

    suspend fun playNote(note: Note) =
        respond(DeviceResponse.PlayNote(note))

    private suspend fun respond(response: DeviceResponse) {
        wsSession.sendSerialized(response)
//        log.debug { "Sent ListenerResponse $response to $this" }
    }

    private data class Id(
        val sessionId: SessionId,
        val callId: String?,
    )

    private val id = Id(sessionId, wsSession.call.callId)

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

suspend fun Device.processRequests(
    consensusService: ConsensusService,
) {
    val device = this

    coroutineScope {
        while (true) {
            val request = receiveRequest()
//            log.debug { "Received DeviceRequest $request from ${this@processRequests}" }

            when (request) {
                is DeviceRequest.AskNote -> {
                    consensusService.askNote(device, request.note)
                }

                is DeviceRequest.CancelNote -> {
                    consensusService.cancelNote(device, request.note)
                }
            }
        }
    }
}

private val log = KotlinLogging.logger {}
