package lerpmusic.btle.server.route

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.callid.callId
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import lerpmusic.btle.domain.scrapper.ScrapperRepository
import lerpmusic.btle.domain.scrapper.processRequests
import lerpmusic.btle.domain.session.AnnouncementService
import lerpmusic.btle.domain.session.SessionId
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

private val ApplicationCall.sessionId
    get() = SessionId(parameters["sessionId"]!!)

fun Route.scrapperSessionRoute(
    scrapperRepository: ScrapperRepository,
    announcementService: AnnouncementService,
) {
    webSocket("/scrapper") {
        val sessionId = call.sessionId

        withCallIdInMDC(call.callId) {
            scrapperRepository.getAndUseScrapper(
                sessionId = sessionId,
                wsSession = this,
            ) { scrapper ->
                if (scrapper == null) {
                    return@getAndUseScrapper
                }

                try {
                    scrapper.processRequests(announcementService)
                } catch (ex: Exception) {
                    when (ex) {
                        is CancellationException,
                        is ClosedSendChannelException,
                        is ClosedReceiveChannelException -> {
                            log.info { "Scrapper session $sessionId is stopped" }
                            throw ex
                        }

                        else -> {
                            log.error(ex) { "Unexpected error in scrapper session $sessionId" }
                        }
                    }
                }
            }
        }
    }
}
