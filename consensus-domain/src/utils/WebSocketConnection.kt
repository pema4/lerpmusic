package lerpmusic.consensus.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.flow.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Серверное/клиентское соединение по вебсокету.
 */
interface WebSocketConnection<out T, in R> : CoroutineScope {
    val incoming: Flow<T>
    suspend fun send(response: R)
}

/**
 * Запускает корутину, которая отправляет [ping] с интервалом [pingTimeout],
 * и при отсутствии ответа `pong` завершает [WebSocketConnection].
 */
fun <Ping, Pong> WebSocketConnection<Pong, Ping>.producePings(
    pingTimeout: Duration = 10.seconds,
    ping: () -> Ping,
): SendChannel<Pong> {
    val receivedPongs = Channel<Pong>(capacity = Channel.CONFLATED)

    val job = launch {
        receivedPongs.consume {
            while (true) {
                send(ping())
                delay(pingTimeout)
                if (receivedPongs.tryReceive().isFailure) {
                    error("Pong was not received within deadline, exiting")
                }
            }
        }
    }
    receivedPongs.invokeOnClose { job.cancel() }

    return receivedPongs
}

data class ReceivedMessages<T>(
    val incoming: Flow<T>,
    val receive: suspend (T) -> Unit,
)

/**
 * Утилита для подписки/отписки на получение сообщений по вебсокету.
 * Алгоритм:
 * 1. Получатель подписывается на [ReceivedMessages.incoming], если это первая подписка — выполняется [onStart].
 * 2. Сообщения, полученные по сокету, через [ReceivedMessages.receive] перенаправляются в [ReceivedMessages.incoming].
 * 3. Получатель отписывается от [ReceivedMessages.incoming], если это была последняя отписка — после таймаута [stopTimeout] выполняется [onCancellation].
 *
 * Под капотом работает через [SharedFlow] и [SharingStarted.WhileSubscribed].
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> WebSocketConnection<*, *>.receiveMessages(
    stopTimeout: Duration = Duration.ZERO,
    onStart: suspend () -> Unit = {},
    onCancellation: suspend CoroutineScope.() -> Unit = {},
): ReceivedMessages<T> {
    val messages: StateFlow<MutableSharedFlow<T>?> = flow {
        // 1. начинаем слушать сообщения
        // Это происходит до вызова onStart, чтобы не просыпать никакие ответы
        emit(MutableSharedFlow<T>())

        // 2. уведомляем отправителя о том, что хотим получать от него сообщения
        onStart()

        try {
            // 3. ждём, пока кто-то подписан на сообщения
            awaitCancellation()
        } finally {
            // 4. Компенсация шага 2 — уведомляем отправителя о том, что сообщения больше не нужны
            launch { onCancellation() }
        }
    }.stateIn(
        scope = this,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = stopTimeout.inWholeMilliseconds,
            replayExpirationMillis = 0,
        ),
        initialValue = null
    )

    return ReceivedMessages(
        incoming = messages.flatMapLatest { it ?: emptyFlow() },
        // Непрошенные сообщения игнорируются
        receive = { messages.value?.emit(it) }
    )
}