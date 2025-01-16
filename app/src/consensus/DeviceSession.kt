package lerpmusic.website.consensus

import io.ktor.server.plugins.callid.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import lerpmusic.consensus.DeviceRequest
import lerpmusic.consensus.SessionId
import lerpmusic.consensus.SessionPin
import lerpmusic.website.consensus.device.Device
import lerpmusic.website.consensus.device.DeviceRepository
import lerpmusic.website.util.withCallIdInMDC
import mu.KotlinLogging
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

fun Route.consensusSessionDeviceRoute(
    deviceRepository: DeviceRepository,
    consensusService: ConsensusService,
    sessionLauncher: ConsensusSessionLauncher,
) {
    webSocket("/old/device/{sessionPin}") {
        val sessionId = SessionId(call.parameters["sessionId"]!!)
        val sessionPin = SessionPin(call.parameters["sessionPin"]!!)

        withCallIdInMDC(call.callId) {
            deviceRepository.getAndUseDevice(
                sessionId = sessionId,
                sessionPin = sessionPin,
                deviceConnection = this@webSocket,
            ) { device ->
                if (device == null) {
                    return@getAndUseDevice
                }

                try {
                    device.processRequests(consensusService)
                } catch (ex: Exception) {
                    when (ex) {
                        is CancellationException,
//                        is ClosedSendChannelException,
                        is ClosedReceiveChannelException -> {
                            currentCoroutineContext().ensureActive()
                            log.info { "Device session $sessionId is stopped, cause: ${this@webSocket.closeReason}" }
                            throw ex
                        }

                        else -> {
                            log.error(ex) { "Unexpected error in sessionId $sessionId" }
                        }
                    }
                } finally {
                    withContext(NonCancellable) {
                        consensusService.cancelAllNotes(device)
                    }
                }
            }
        }
    }

    webSocket("/device/{sessionPin}") {
        val sessionId = SessionId(call.parameters["sessionId"]!!)
        val sessionPin = SessionPin(call.parameters["sessionPin"]!!)

        flow<Nothing> { awaitCancellation() }
            .onCompletion { println("cancelled") }
            .launchIn(this)

        withCallIdInMDC(call.callId) {
            val session = sessionLauncher.getOrStartSession(sessionId)
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

/**
 * Уйдёт в [SessionComposition.events]
 */
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

            is DeviceRequest.ReceiveIntensityUpdates -> TODO()

            is DeviceRequest.CancelIntensityUpdates -> TODO()

            DeviceRequest.Ping -> TODO()
        }
    }
}

private val log = KotlinLogging.logger {}
