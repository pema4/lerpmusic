package lerpmusic.consensus.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.sign
import kotlin.time.Duration

typealias Connection = CoroutineScope

/**
 * Отслеживает изменения в [`Flow<List<T>>`][Flow], запуская [action] для каждого входящего соединения.
 *
 * Каждый [action] запускается в дочерней корутине, которая будет отменена при завершении [T] или при удалении [T] из списка.
 */
suspend fun <T : Connection> Flow<List<T>>.collectConnected(
    action: suspend CoroutineScope.(T) -> Unit,
) {
    // Как лучше всего отменять дочерние корутины при отмене выполнения collectEachAddedListener?
    // В наивном варианте с invokeOnCompletion будет утекать память:
    // val launchedJobs = items.added.map { item ->
    //     item.launch { block(item) }
    // }
    // currentCoroutineContext().job.invokeOnCompletion { cause ->
    //     launchedJobs.forEach { it.cancel() }
    // }
    val launchedJobs = mutableMapOf<T, Job>()

    runningSetDifference()
        .onEach { diff ->
            for (connected in diff.added) {
                launchedJobs[connected] = connected.launch { action(connected) }
            }

            for (disconnected in diff.removed) {
                launchedJobs.remove(disconnected)?.cancel()
            }
        }
        .onCompletion { cause ->
            launchedJobs.values.forEach {
                it.cancel("collectEachAddedCoroutine cancelled", cause)
            }
        }
        .collect()
}

/**
 * [Flow], который для каждого входящего соединения выполняет действие [transform] и возвращает элементы полученного [Flow]
 *
 * @see collectConnected
 * @see Flow.flatMapMerge
 */
fun <T : Connection, R> Flow<List<T>>.flatMapConnected(
    transform: suspend (T) -> Flow<R>,
): Flow<R> {
    return channelFlow {
        collectConnected { coroutine ->
            transform(coroutine).collect { send(it) }
        }
    }
}

/**
 * [Flow], возвращающий количество соединений, для которых [predicate] возвращает `true`
 */
fun <T : Connection> Flow<List<T>>.runningCountConnected(predicate: suspend (T) -> Flow<Boolean>): Flow<Int> {
    return channelFlow {
        collectConnected { connection ->
            // Для перехода false -> true возвращаем 1, для true -> false - -1
            // [false, true, false, true] -> [0, 1, -1, 1]
            // [true, false, true] -> [1, -1, 1]
            var previous = false
            predicate(connection)
                .onCompletion { if (previous) send(-1) }
                .collect { requested ->
                    val delta = (requested compareTo previous).sign
                    previous = requested
                    send(delta)
                }
        }
    }.runningFold(0, Int::plus)
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