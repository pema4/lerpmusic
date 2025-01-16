package lerpmusic.consensus.device

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.flow.*
import lerpmusic.consensus.Audience
import lerpmusic.consensus.DeviceRequest
import lerpmusic.consensus.DeviceResponse
import lerpmusic.consensus.Note
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class DeviceAudience(
    private val serverConnection: ServerConnection,
    private val max: Max,
) : Audience {
    private val deferredResponses = mutableMapOf<Note, CompletableDeferred<Boolean>>()
    private val receivedPongs = serverConnection.launchPinger()

    /**
     * Небольшой абьюз [SharedFlow] и [SharingStarted.WhileSubscribed]:
     * - при первой подписке на [intensityUpdates] будет отправлен запрос слушателю
     * - при последней отписке слушателю отправится уведомление об отмене
     * - если подписки нет, сообщения от слушателя бросаются на пол
     *
     * Эта конструкция заменяет атомарный флаг + дублирование логики подписки/отписки в [intensityUpdates]
     */
    private val receivedIntensityUpdates: StateFlow<MutableSharedFlow<Double>?> = flow {
        // 1. начинаем слушать сообщения от слушателя
        // Это происходит до отправки запроса, чтобы не просыпать никакие ответы
        emit(MutableSharedFlow<Double>())

        // 2. уведомляем слушателя о том, что хотим получать уведомления об интенсивности.
        serverConnection.send(DeviceRequest.ReceiveIntensityUpdates)

        try {
            // 3. слушаем бесконечно — пока кто-то подписан на эти уведомления
            awaitCancellation()
        } finally {
            // 4. Компенсация шага 1 — отменяем уведомление
            serverConnection.launch {
                serverConnection.send(DeviceRequest.CancelIntensityUpdates)
            }
        }
    }.stateIn(serverConnection, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), null)

    override val intensityUpdates: Flow<Double> = receivedIntensityUpdates.flatMapLatest { it ?: emptyFlow() }

    private val receiverCoroutine = serverConnection.launch { receiveMessages() }

    private suspend fun CoroutineScope.receiveMessages(): Nothing {
        while (true) {
            when (val event = serverConnection.receive()) {
                is DeviceResponse.Pong -> receivedPongs.send(event)
                is DeviceResponse.PlayNote -> deferredResponses[event.note]?.complete(true)
                is DeviceResponse.IntensityUpdate -> receivedIntensityUpdates.value?.emit(event.delta)
            }
        }
    }

    override suspend fun shouldPlayNote(note: Note): Boolean = coroutineScope {
        // 1. Начинаем слушать входящие сообщения, там будет ответ на запрос
        val deferredResponse = CompletableDeferred<Boolean>()
        val previousDeferredResponse = deferredResponses.put(note, deferredResponse)
        // complete(false) вместо cancel(), чтобы не отправлять отмену запроса на сервер
        previousDeferredResponse?.complete(false)

        var requestSent = false
        try {
            // 2. Отправляем запрос
            serverConnection.send(DeviceRequest.AskNote(note))
            requestSent = true

            // 3. Ждём ответ
            deferredResponse.await()
        } catch (ex: RuntimeException) {
            // Результат shouldPlayNote больше не нужен — чистим ресурсы
            // Компенсация шага 2 — сообщаем серверу, что ответ больше не нужен
            if (requestSent) {
                withContext(NonCancellable) {
                    try {
                        serverConnection.send(DeviceRequest.CancelNote(note))
                    } catch (suppressed: Throwable) {
                        ex.addSuppressed(suppressed)
                    }
                }
            }

            when {
                // Если запрос отменили, проигрывать событие не нужно.
                deferredResponse.isCancelled -> false
                else -> throw ex
            }
        } finally {
            // Компенсация шага 1 — сигнализируем серверу, что ответ больше не нужен
            // В Kotlin/JS не хватает перегрузки remove(note, deferredResponse) и функции compute
            val value = deferredResponses[note]
            if (value == deferredResponse) {
                deferredResponses.remove(note)
            }
        }
    }

    override fun cancelNote(note: Note) {
        deferredResponses[note]?.cancel()
    }
}

private fun ServerConnection.launchPinger(
    // TODO: для дебага таймаут небольшой
    pingTimeout: Duration = 30.minutes,
): Channel<DeviceResponse.Pong> {
    val receivedPongs = Channel<DeviceResponse.Pong>(capacity = Channel.CONFLATED)

    launch {
        receivedPongs.consume {
            while (true) {
                send(DeviceRequest.Ping)
                delay(pingTimeout)
                if (receivedPongs.tryReceive().isFailure) {
                    error("Pong was not received within deadline, exiting")
                }
            }
        }
    }

    return receivedPongs
}