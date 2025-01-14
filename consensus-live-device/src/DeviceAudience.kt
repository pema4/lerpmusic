package lerpmusic.consensus.device

import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.*
import lerpmusic.consensus.Audience
import lerpmusic.consensus.DeviceRequest
import lerpmusic.consensus.DeviceResponse
import lerpmusic.consensus.Note

class DeviceAudience(
    private val serverConnection: DefaultClientWebSocketSession,
    private val max: Max,
) : Audience {
    private val deferredResponses = mutableMapOf<Note, CompletableDeferred<Boolean>>()

    init {
        serverConnection.launch { receiveMessages() }
    }

    private suspend fun CoroutineScope.receiveMessages(): Nothing {
        while (true) {
            when (val event = serverConnection.receiveDeserialized<DeviceResponse>()) {
                is DeviceResponse.PlayNote -> deferredResponses[event.note]?.complete(true)
            }
        }
    }

    override suspend fun shouldPlayNote(note: Note): Boolean = coroutineScope {
        // 1. Начинаем слушать входящие сообщения, там будет ответ на запрос
        val deferredResponse = CompletableDeferred<Boolean>()
        val oldDeferredResponse = deferredResponses.put(note, deferredResponse)
        oldDeferredResponse?.cancel()

        var requestSent = false
        try {
            // 2. Отправляем запрос
            serverConnection.sendSerialized<DeviceRequest>(DeviceRequest.AskNote(note))
            requestSent = true

            // 3. Ждём ответ
            deferredResponse.await()
        } catch (ex: RuntimeException) {
            max.post("Cancelled note $note")

            // Результат shouldPlayNote больше не нужен — чистим ресурсы
            // Компенсация шага 2 — сообщаем серверу, что ответ больше не нужен
            if (requestSent) {
                withContext(NonCancellable) {
                    try {
                        serverConnection.sendSerialized<DeviceRequest>(DeviceRequest.CancelNote(note))
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

    override suspend fun cancelNote(note: Note) {
        deferredResponses[note]?.cancel()
    }
}