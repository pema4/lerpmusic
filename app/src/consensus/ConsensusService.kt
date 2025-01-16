package lerpmusic.website.consensus

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import lerpmusic.consensus.DeviceRequest
import lerpmusic.consensus.Note
import lerpmusic.consensus.SessionId
import lerpmusic.website.consensus.device.Device
import lerpmusic.website.consensus.device.DeviceRepository
import lerpmusic.website.consensus.listener.ListenerRepository
import mu.KotlinLogging

class ConsensusService(
    private val deviceRepository: DeviceRepository,
    private val listenerRepository: ListenerRepository,
    private val noteQueue: NoteQueue,
) {
    /**
     * [SupervisorJob] для отправки запросов клиентам.
     * Если по какой-то причине запрос не отправился -
     * нет смысла пробрасывать исключения по стеку вверх.
     */
    private val listenerRequestScope = SupervisorJob() +
            CoroutineExceptionHandler { _, ex ->
                log.error(ex) { "Can't send response in background" }
            }

    private val log = KotlinLogging.logger {}

    /**
     * Устройство отправляет сообщения "хочу проиграть такую-то ноту".
     * Сервер отправляет запрос всем слушателям с вопросом "Проиграть ноту?".
     * Если слушатель нажал "Да", проигрывается какая-то нота из очереди.
     * Если после проигрывания в очереди не осталось нот, слушателям отправляется отмена запроса.
     */
    suspend fun askNote(
        device: Device,
        note: Note,
    ) {
        val enqueueResult = noteQueue.enqueueNote(device, note)

        if (enqueueResult.queueSize == 1) {
            val sessionListeners = listenerRepository.getAll(device.sessionId)
            coroutineScope {
                for (listener in sessionListeners) {
                    launch(listenerRequestScope) { listener.askForAction() }
                }
            }
        }
    }

    suspend fun playOldestNote(sessionId: SessionId) {
        val (oldestNote, device, remainingNotes) = noteQueue
            .dequeueOldestNote(sessionId)
            ?: return

        coroutineScope {
            launch { device.playNote(oldestNote) }

            if (remainingNotes == 0) {
                val sessionListeners = listenerRepository.getAll(device.sessionId)
                for (listener in sessionListeners) {
                    launch(listenerRequestScope) { listener.cancelAction() }
                }
            }
        }
    }

    suspend fun cancelNote(
        device: Device,
        note: Note,
    ) {
        val (_, _, remainingNotes) =
            noteQueue.dequeueNote(device, note) ?: return

        if (remainingNotes == 0) {
            val sessionListeners = listenerRepository.getAll(device.sessionId)
            coroutineScope {
                for (listener in sessionListeners) {
                    launch(listenerRequestScope) { listener.cancelAction() }
                }
            }
        }
    }

    suspend fun cancelAllNotes(
        device: Device,
    ) {
        noteQueue.dequeueAllNotes(device)
        val sessionListeners = listenerRepository.getAll(device.sessionId)
        coroutineScope {
            for (listener in sessionListeners) {
                launch(listenerRequestScope) { listener.cancelAction() }
            }
        }
    }

    fun sessionHasEnqueuedNotes(
        sessionId: SessionId,
    ): Boolean {
        return noteQueue.hasEnqueuedNotes(sessionId)
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
//        log.debug { "Received DeviceRequest $request from ${this@processRequests}" }

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