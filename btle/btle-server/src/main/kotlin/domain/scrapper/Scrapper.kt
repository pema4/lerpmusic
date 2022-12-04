package lerpmusic.btle.domain.scrapper

import io.ktor.server.plugins.callid.callId
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import lerpmusic.btle.domain.session.AnnouncementService
import lerpmusic.btle.domain.session.SessionId
import mu.KotlinLogging

class Scrapper(
    val sessionId: SessionId,
    private val wsSession: WebSocketServerSession,
) {
    data class Id(
        val sessionId: SessionId,
        val callId: String?,
    )

    val id = Id(sessionId, wsSession.call.callId)

    suspend fun receiveRequest(): ScrapperRequest =
        wsSession.receiveDeserialized()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Scrapper

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Listener($sessionId, ${id.callId})"
    }
}

suspend fun Scrapper.processRequests(
    announcementService: AnnouncementService,
) {
    coroutineScope {
        while (true) {
            when (val request = receiveRequest()) {
                is ScrapperRequest.Announcement -> {
                    launch {
                        announcementService.announce(sessionId, request.id, request.rssi)
                    }
                }
            }
        }
    }
}

private val log = KotlinLogging.logger {}
