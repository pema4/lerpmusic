package lerpmusic.website.consensus

import com.typesafe.config.ConfigFactory
import io.ktor.server.response.*
import io.ktor.server.routing.*
import lerpmusic.consensus.SessionPin
import lerpmusic.website.consensus.device.DeviceRepository
import lerpmusic.website.consensus.listener.ListenerRepository
import lerpmusic.website.consensus.session.SessionRepository

private val config = ConfigFactory.load("lerpmusic.conf")
private val sessionRepository = SessionRepository(
    sessionPin = SessionPin(config.getString("lerpmusic.consensus.sessionPin")),
)

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
