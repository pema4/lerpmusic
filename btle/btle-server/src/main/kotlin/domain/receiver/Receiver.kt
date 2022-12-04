package lerpmusic.btle.domain.receiver

import io.ktor.server.plugins.callid.callId
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import lerpmusic.btle.domain.session.SessionId
import mu.KotlinLogging

class Receiver(
    val sessionId: SessionId,
    val bucketRange: IntRange,
    private val wsSession: WebSocketServerSession,
) {
    suspend fun foundPeripheral(bucket: Int, rssi: Int) =
        respond(ReceiverResponse.FoundPeripheral(bucket, rssi))

    suspend fun lostPeripheral(bucket: Int) =
        respond(ReceiverResponse.LostPeripheral(bucket))

    private suspend fun respond(response: ReceiverResponse) {
        wsSession.sendSerialized(response)
        log.debug { "Sent ReceiverRequest $response to $this" }
    }

    private data class Id(
        val sessionId: SessionId,
        val callId: String?,
    )

    private val id = Id(sessionId, wsSession.call.callId)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Receiver

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
