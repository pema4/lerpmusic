package lerpmusic.website.consensus

import io.ktor.server.plugins.callid.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import lerpmusic.consensus.DeviceRequest
import lerpmusic.consensus.SessionId
import lerpmusic.consensus.SessionPin
import lerpmusic.website.consensus.device.Device
import lerpmusic.website.consensus.device.DeviceRepository
import mu.KotlinLogging
import kotlin.coroutines.cancellation.CancellationException


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

suspend fun Device.processRequests(
    consensusService: ConsensusService,
): Nothing {
    while (true) {
        val request = receiveRequest()
        log.debug { "Received DeviceRequest $request from ${this@processRequests}" }

        when (request) {
            is DeviceRequest.AskNote -> {
                consensusService.askNote(this, request.note)
            }

            is DeviceRequest.CancelNote -> {
                consensusService.cancelNote(this, request.note)
            }
        }
    }
}

private val log = KotlinLogging.logger {}
