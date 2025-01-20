package lerpmusic.consensus.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.sign

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
            for (connection in diff.added) {
                launchedJobs[connection] = connection.launch { action(connection) }
            }

            for (connection in diff.removed) {
                launchedJobs.remove(connection)?.cancel()
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
