package lerpmusic.website.consensus

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageConfig
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.ApplicationCall
import io.ktor.server.http.content.resolveResource
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.host
import io.ktor.server.request.port
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import lerpmusic.consensus.SessionId
import lerpmusic.consensus.SessionPin
import lerpmusic.website.consensus.device.DeviceRepository
import lerpmusic.website.consensus.listener.ListenerRepository
import lerpmusic.website.consensus.session.SessionRepository
import lerpmusic.website.util.withCallIdInMDC
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

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
            sessionLauncher = sessionLauncher,
        )

        consensusSessionListenerRoute(
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

private fun Route.consensusSessionDeviceRoute(
    sessionLauncher: ConsensusSessionLauncher,
) {
    webSocket("/device/{sessionPin}") {
        val sessionId = SessionId(call.parameters["sessionId"]!!)
        val sessionPin = SessionPin(call.parameters["sessionPin"]!!)

        flow<Nothing> { awaitCancellation() }
            .onCompletion { println("cancelled") }
            .launchIn(this)

        withCallIdInMDC(call.callId) {
            val session = sessionLauncher.getSession(sessionId)
            session.collectLatest { session ->
                if (session == null) return@collectLatest

                val connection = DeviceConnection(
                    id = Uuid.random(),
                    webSocketSession = this@webSocket,
                )
                session.addDevice(connection, sessionPin)

                // Отключение от сессии произойдёт при отмене текущей корутины, поэтому просто ждём
                awaitCancellation()
            }
        }
    }
}

private fun Route.consensusSessionListenerRoute(
    sessionLauncher: ConsensusSessionLauncher,
) {
    get {
        val resource = call.resolveResource("static/consensus.html")!!
        call.respond(resource)
    }

    get("/qr") {
        val resource = call.resolveResource("static/qr.html")!!
        call.respond(resource)
    }

    get("/qr-image.png") {
        val qrText = buildString {
            with(call.request) {
                append(host())
                if (port() != 80 && port() != 443) {
                    append(":${port()}")
                }
            }

            append("/c/")
            append(call.sessionId.value)
        }

        val qr = withContext(Dispatchers.IO) {
            generateQRCodeImage(qrText)
        }

        call.respondOutputStream {
            ImageIO.write(qr, "png", this)
        }
    }

    webSocket("/listener") {
        val sessionId = call.sessionId

        withCallIdInMDC(call.callId) {
            val session = sessionLauncher.getSession(sessionId)
            session.collectLatest { session ->
                val connection = ListenerConnection(
                    id = Uuid.random(),
                    webSocketSession = this@webSocket,
                )
                session.addListener(connection)

                // Отключение от сессии произойдёт при отмене текущей корутины, поэтому просто ждём
                awaitCancellation()
            }
        }
    }
}

private fun generateQRCodeImage(barcodeText: String): BufferedImage {
    val barcodeWriter = QRCodeWriter()
    val bitMatrix = barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 512, 512)
    val config = MatrixToImageConfig(0xFF000000u.toInt(), 0x00000000u.toInt())
    return MatrixToImageWriter.toBufferedImage(bitMatrix, config)
}

private val ApplicationCall.sessionId
    get() = SessionId(parameters["sessionId"]!!)
