package lerpmusic.website.consensus

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import lerpmusic.website.consensus.device.DeviceRepository
import lerpmusic.website.consensus.listener.ListenerRepository
import lerpmusic.website.consensus.session.SessionRepository

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

fun Route.consensusRoutes() {
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

    get("/c/{sessionId}") {
        val sessionId = call.parameters["sessionId"]!!
        call.respondRedirect("/consensus/$sessionId")
    }

    get("/c/{sessionId}/qr") {
        val sessionId = call.parameters["sessionId"]!!
        call.respondRedirect("/consensus/$sessionId/qr")
    }
}
