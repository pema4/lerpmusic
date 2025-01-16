package lerpmusic.website.consensus

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageConfig
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import lerpmusic.consensus.SessionId
import lerpmusic.website.consensus.listener.ListenerRepository
import lerpmusic.website.util.withCallIdInMDC
import mu.KotlinLogging
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.uuid.Uuid

private val log = KotlinLogging.logger {}

private val ApplicationCall.sessionId
    get() = SessionId(parameters["sessionId"]!!)

fun Route.consensusSessionListenerRoute(
    listenerRepository: ListenerRepository,
    consensusService: ConsensusService,
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
                if (session == null) return@collectLatest

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

fun generateQRCodeImage(barcodeText: String): BufferedImage {
    val barcodeWriter = QRCodeWriter()
    val bitMatrix = barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 512, 512)
    val config = MatrixToImageConfig(0xFF000000u.toInt(), 0x00000000u.toInt())
    return MatrixToImageWriter.toBufferedImage(bitMatrix, config)
}
