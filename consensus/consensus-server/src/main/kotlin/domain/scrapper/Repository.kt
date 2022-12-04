package lerpmusic.consensus.domain.listener

import io.ktor.server.websocket.WebSocketServerSession
import lerpmusic.consensus.domain.session.SessionId
import lerpmusic.consensus.domain.session.SessionRepository
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

class ListenerRepository(
    private val sessionRepository: SessionRepository,
) {
    private val activeListeners =
        ConcurrentHashMap<SessionId, Set<Listener>>()
    private val log = KotlinLogging.logger {}

    suspend fun getAndUseListener(
        sessionId: SessionId,
        wsSession: WebSocketServerSession,
        block: suspend (Listener?) -> Unit,
    ) {
        if (!sessionRepository.exists(sessionId)) {
            return block(null)
        }

        val listener = Listener(sessionId, wsSession)
        try {
            log.info { "Active listeners size: ${activeListeners[sessionId]?.size}" }
            activeListeners.compute(sessionId) { _, listeners ->
                listeners.orEmpty() + listener
            }
            block(listener)
        } finally {
            activeListeners.compute(sessionId) { _, listeners ->
                (listeners.orEmpty() - listener)
                    .ifEmpty { null }
            }
        }
    }

    fun getAll(sessionId: SessionId): Set<Listener> {
        return activeListeners[sessionId].orEmpty()
    }
}
