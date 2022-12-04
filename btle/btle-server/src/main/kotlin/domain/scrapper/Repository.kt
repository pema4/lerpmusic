package lerpmusic.btle.domain.scrapper

import io.ktor.server.websocket.WebSocketServerSession
import lerpmusic.btle.domain.session.SessionId
import lerpmusic.btle.domain.session.SessionRepository
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

class ScrapperRepository(
    private val sessionRepository: SessionRepository,
) {
    private val activeScrappers =
        ConcurrentHashMap<SessionId, Set<Scrapper>>()
    private val log = KotlinLogging.logger {}

    suspend fun getAndUseScrapper(
        sessionId: SessionId,
        wsSession: WebSocketServerSession,
        block: suspend (Scrapper?) -> Unit,
    ) {
        if (!sessionRepository.exists(sessionId)) {
            return block(null)
        }

        val scrapper = Scrapper(sessionId, wsSession)
        try {
            log.info { "Active scrappers size: ${activeScrappers[sessionId]?.size}" }
            activeScrappers.compute(sessionId) { _, listeners ->
                listeners.orEmpty() + scrapper
            }
            block(scrapper)
        } finally {
            activeScrappers.compute(sessionId) { _, listeners ->
                (listeners.orEmpty() - scrapper)
                    .ifEmpty { null }
            }
        }
    }

    fun getAll(sessionId: SessionId): Set<Scrapper> {
        return activeScrappers[sessionId].orEmpty()
    }
}
