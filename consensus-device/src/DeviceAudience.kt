package lerpmusic.consensus.device

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import lerpmusic.consensus.*
import lerpmusic.consensus.utils.producePings
import lerpmusic.consensus.utils.receiveMessagesWithSubscription

class DeviceAudience(
    private val serverConnection: ServerConnection,
    private val max: Max,
) : Audience {
    private val deferredResponses = mutableMapOf<Note, CompletableDeferred<Boolean>>()
    private val receivedPongs = serverConnection.producePings { DeviceRequest.Ping }

    private val receivedListenersCount = MutableStateFlow<Int>(0)
    override val listenersCount: Flow<Int> = receivedListenersCount.asStateFlow()

    private val receivedIntensityUpdates = serverConnection.receiveMessagesWithSubscription<IntensityUpdate>(
        onStart = { serverConnection.send(DeviceRequest.ReceiveIntensityUpdates) },
        onCancellation = { serverConnection.send(DeviceRequest.CancelIntensityUpdates) },
    )
    override val intensityUpdates: Flow<IntensityUpdate> = receivedIntensityUpdates.incoming

    init {
        serverConnection.launch { receiveMessages() }
    }

    private suspend fun CoroutineScope.receiveMessages() {
        serverConnection.incoming.collect { event ->
            when (event) {
                is DeviceResponse.Pong -> receivedPongs.send(event)
                is DeviceResponse.ListenersCount -> receivedListenersCount.value = event.count
                is DeviceResponse.PlayNote -> deferredResponses[event.note]?.complete(true)
                is DeviceResponse.IntensityUpdate -> receivedIntensityUpdates.receive(
                    IntensityUpdate(
                        event.decrease,
                        event.increase
                    )
                )
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
                deferredResponses -= note
            }
        }
    }

    override fun cancelNote(note: Note) {
        deferredResponses[note]?.cancel()
    }
}
