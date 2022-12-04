package lerpmusic.btle.server.route

import io.ktor.server.plugins.callid.callId
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import lerpmusic.btle.domain.receiver.ReceiverRepository
import lerpmusic.btle.domain.session.AnnouncementService
import lerpmusic.btle.domain.session.SessionId
import mu.KotlinLogging
import org.slf4j.MDC

fun Route.receiverSessionRoute(
    receiverRepository: ReceiverRepository,
    announcementService: AnnouncementService,
) {
    webSocket("/receiver/{bucketStart}/{bucketLength}") {
        val sessionId = SessionId(call.parameters["sessionId"]!!)
        val bucketStart = call.parameters["bucketStart"]!!.toInt()
        val bucketLength = call.parameters["bucketLength"]!!.toInt()
        val bucketsRange = bucketStart until (bucketStart + bucketLength)

        withCallIdInMDC(call.callId) {
            receiverRepository.getAndUseReceiver(
                sessionId = sessionId,
                bucketsRange = bucketsRange,
                wsSession = this,
            ) { receiver ->
                if (receiver == null) {
                    return@getAndUseReceiver
                }

                try {
                    awaitCancellation()
                } catch (ex: Exception) {
                    when (ex) {
                        is CancellationException,
                        is ClosedSendChannelException,
                        is ClosedReceiveChannelException -> {
                            log.info { "Receiver session $sessionId is stopped" }
                            throw ex
                        }

                        else -> {
                            log.error(ex) { "Unexpected error in receiver session $sessionId" }
                        }
                    }
                } finally {
                    announcementService.freeReceiverBuckets(receiver)
                }
            }
        }
    }
}

suspend fun <T> withCallIdInMDC(callId: String?, block: suspend () -> T) {
    MDC.put("call-id", callId)
    withContext(MDCContext()) {
        block()
    }
}

private val log = KotlinLogging.logger {}
