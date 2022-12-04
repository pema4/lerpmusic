package lerpmusic.btle.domain.receiver

import io.ktor.server.websocket.WebSocketServerSession
import lerpmusic.btle.domain.session.SessionId
import lerpmusic.btle.domain.session.SessionRepository
import java.util.concurrent.ConcurrentHashMap

class ReceiverRepository(
    private val sessionRepository: SessionRepository,
) {
    private val activeReceivers = ConcurrentHashMap<SessionId, Set<Receiver>>()

    suspend fun getAndUseReceiver(
        sessionId: SessionId,
        bucketsRange: IntRange,
        wsSession: WebSocketServerSession,
        block: suspend (Receiver?) -> Unit,
    ) {
        if (!sessionRepository.exists(sessionId)) {
            return block(null)
        }

        val receiver = Receiver(sessionId, bucketsRange, wsSession)
        try {
            activeReceivers.compute(sessionId) { _, devices ->
                devices.orEmpty() + receiver
            }
            block(receiver)
        } finally {
            activeReceivers.compute(sessionId) { _, devices ->
                (devices.orEmpty() - receiver)
                    .ifEmpty { null }
            }
        }
    }

    fun getAll(sessionId: SessionId): Collection<Receiver> {
        return activeReceivers[sessionId].orEmpty()
    }
}
