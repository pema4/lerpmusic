package lerpmusic.consensus.server.config

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import lerpmusic.consensus.domain.device.DeviceRepository
import lerpmusic.consensus.domain.listener.ListenerRepository
import lerpmusic.consensus.domain.note.NoteQueue
import lerpmusic.consensus.domain.session.ConsensusService
import lerpmusic.consensus.domain.session.SessionRepository
import lerpmusic.consensus.server.route.deviceSessionRoute
import lerpmusic.consensus.server.route.listenerSessionRoute

private val sessionRepository = SessionRepository()

private val noteQueue = NoteQueue()

private val deviceRepository = DeviceRepository(
    sessionRepository = sessionRepository,
    noteQueue = noteQueue,
)

private val listenerRepository = ListenerRepository(
    sessionRepository = sessionRepository,
)

private val consensusService = ConsensusService(
    deviceRepository = deviceRepository,
    listenerRepository = listenerRepository,
    noteQueue = noteQueue,
)

fun Application.configureRouting() {
    routing {
        get("/c/{sessionId}") {
            val sessionId = call.parameters["sessionId"]!!
            call.respondRedirect("/consensus/$sessionId")
        }

        route("/consensus/{sessionId}") {
            deviceSessionRoute(
                deviceRepository = deviceRepository,
                consensusService = consensusService,
            )

            listenerSessionRoute(
                listenerRepository = listenerRepository,
                consensusService = consensusService,
            )
        }

        static("/static") {
            resources("static")
        }
    }
}

