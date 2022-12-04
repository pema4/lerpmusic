package lerpmusic.btle.server.config

import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import lerpmusic.btle.domain.receiver.ReceiverRepository
import lerpmusic.btle.domain.scrapper.ScrapperRepository
import lerpmusic.btle.domain.session.AnnouncementService
import lerpmusic.btle.domain.session.SessionRepository
import lerpmusic.btle.server.route.receiverSessionRoute
import lerpmusic.btle.server.route.scrapperSessionRoute

private val sessionRepository = SessionRepository()

private val receiverRepository = ReceiverRepository(
    sessionRepository = sessionRepository,
)

private val scrapperRepository = ScrapperRepository(
    sessionRepository = sessionRepository,
)

private val announcementService = AnnouncementService(
    receiverRepository = receiverRepository,
)

fun Application.configureRouting() {
    routing {
        route("/btle/{sessionId}") {
            receiverSessionRoute(
                receiverRepository = receiverRepository,
                announcementService = announcementService,
            )

            scrapperSessionRoute(
                scrapperRepository = scrapperRepository,
                announcementService = announcementService,
            )
        }
    }
}

