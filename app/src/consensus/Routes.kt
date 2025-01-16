package lerpmusic.website.consensus

import com.typesafe.config.ConfigFactory
import io.ktor.server.response.*
import io.ktor.server.routing.*
import lerpmusic.consensus.SessionPin
import lerpmusic.website.consensus.device.DeviceRepository
import lerpmusic.website.consensus.listener.ListenerRepository
import lerpmusic.website.consensus.session.SessionRepository
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

private val config = ConfigFactory.load("lerpmusic.conf")
private val sessionRepository = SessionRepository(
    sessionPin = SessionPin(config.getString("lerpmusic.consensus.sessionPin")),
)

private val sessionLauncher = ConsensusSessionLauncher(
    expectedSessionPin = SessionPin(config.getString("lerpmusic.consensus.sessionPin")),
    sessionKeepAlive = config.getDuration("lerpmusic.consensus.sessionKeepAliveSeconds", TimeUnit.SECONDS).seconds
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

fun Route.consensusSessionRoutes() {
    route("/consensus/{sessionId}") {
        consensusSessionDeviceRoute(
            deviceRepository = deviceRepository,
            consensusService = consensusService,
            sessionLauncher = sessionLauncher,
        )

        consensusSessionListenerRoute(
            listenerRepository = listenerRepository,
            consensusService = consensusService,
            sessionLauncher = sessionLauncher,
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
