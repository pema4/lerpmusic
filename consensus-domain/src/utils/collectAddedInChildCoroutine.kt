package lerpmusic.consensus.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

suspend fun <T : CoroutineScope> StateFlow<List<T>>.collectAddedInChildCoroutine(
    block: suspend CoroutineScope.(T) -> Unit,
): Nothing {
    // TODO: как лучше всего отменять дочерние корутины при отмене выполнения collectEachAddedListener?
    //  В наивном варианте с invokeOnCompletion будет утекать память:
    //  val newJobs = items.added.map { item ->
    //      item.launch { block(item) }
    //  }
    //  currentCoroutineContext().job.invokeOnCompletion { cause ->
    //      newJobs.forEach { it.cancel() }
    //  }
    val activeJobs = mutableMapOf<T, Job>()

    runningSetDifference()
        .onEach { diff ->
            for (item in diff.added) {
                activeJobs[item] = item.launch { block(item) }
            }

            for (item in diff.removed) {
                activeJobs.remove(item)?.cancel()
            }
        }
        .onCompletion { cause ->
            activeJobs.values.forEach {
                it.cancel("collectEachAddedCoroutine cancelled", cause)
            }
        }
        .collect()

    error("Unreachable, should run forever because receiver is a StateFlow")
}

fun <T : CoroutineScope, R> StateFlow<List<T>>.flatMapMergeAddedInChildCoroutine(
    transform: suspend CoroutineScope.(T) -> Flow<R>,
): Flow<R> {
    return channelFlow {
        collectAddedInChildCoroutine { coroutine ->
            transform(coroutine).collect { send(it) }
        }
    }
}