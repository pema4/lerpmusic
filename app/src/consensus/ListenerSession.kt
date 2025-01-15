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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.withContext
import lerpmusic.consensus.SessionId
import lerpmusic.website.consensus.listener.ListenerRepository
import lerpmusic.website.consensus.listener.processRequests
import lerpmusic.website.util.withCallIdInMDC
import mu.KotlinLogging
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

private val log = KotlinLogging.logger {}

private val ApplicationCall.sessionId
    get() = SessionId(parameters["sessionId"]!!)

fun Route.listenerSessionRoute(
    listenerRepository: ListenerRepository,
    consensusService: ConsensusService,
) {
    get {
        call
            .resolveResource("static/consensus.html")
            ?.let { call.respond(it) }
    }

    get("/qr") {
        call
            .resolveResource("static/qr.html")
            ?.let { call.respond(it) }
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

    webSocket("/client") {
        val sessionId = call.sessionId

        withCallIdInMDC(call.callId) {
            listenerRepository.getAndUseListener(
                sessionId = sessionId,
                wsSession = this@webSocket,
            ) { listener ->
                if (listener == null) {
                    return@getAndUseListener
                }

                try {
                    listener.processRequests(consensusService)
                } catch (ex: Exception) {
                    when (ex) {
                        is CancellationException,
                        is ClosedSendChannelException,
                        is ClosedReceiveChannelException -> {
                            log.info { "Device session $sessionId is stopped" }
                            throw ex
                        }

                        else -> {
                            log.error(ex) { "Unexpected error in sessionId $sessionId" }
                        }
                    }
                }
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
