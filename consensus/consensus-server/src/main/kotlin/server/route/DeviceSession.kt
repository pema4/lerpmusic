package lerpmusic.consensus.server.route

import io.ktor.server.plugins.callid.callId
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import lerpmusic.consensus.domain.device.DeviceRepository
import lerpmusic.consensus.domain.device.processRequests
import lerpmusic.consensus.domain.session.ConsensusService
import lerpmusic.consensus.domain.session.SessionId
import lerpmusic.consensus.domain.session.SessionPin
import mu.KotlinLogging
import org.slf4j.MDC

fun Route.deviceSessionRoute(
    deviceRepository: DeviceRepository,
    consensusService: ConsensusService,
) {
    webSocket("/device/{sessionPin}") {
        val sessionId = SessionId(call.parameters["sessionId"]!!)
        val sessionPin = SessionPin(call.parameters["sessionPin"]!!)

        withCallIdInMDC(call.callId) {
            deviceRepository.getAndUseDevice(
                sessionId = sessionId,
                sessionPin = sessionPin,
                wsSession = this,
            ) { device ->
                if (device == null) {
                    return@getAndUseDevice
                }

                try {
                    device.processRequests(consensusService)
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
                } finally {
                    consensusService.cancelAllNotes(device)
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
