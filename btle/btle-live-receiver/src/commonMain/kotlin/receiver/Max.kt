package lerpmusic.btle.receiver

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

expect object Max {
    fun post(msg: Any?)
    fun addHandler(msg: Any?, handler: (Any?) -> Unit)
    fun addHandler2(msg: Any?, handler: (Any?, Any?) -> Unit)
    fun addHandler3(msg: Any?, handler: (Any?, Any?, Any?) -> Unit)
}

fun <T> Max.receiveAsState(
    msg: String,
    initial: T,
    transform: (Any?) -> T,
): StateFlow<T> {
    return MutableStateFlow(initial)
        .apply { addHandler(msg) { value = transform(it) } }
        .asStateFlow()
}

fun <T> Max.receive2AsState(
    msg: String,
    initial: T,
    transform: (Any?, Any?) -> T,
): StateFlow<T> {
    return MutableStateFlow(initial)
        .apply { addHandler2(msg) { a, b -> value = transform(a, b) } }
        .asStateFlow()
}

fun <T> Max.receive2AsChannel(
    msg: String,
    transform: (Any?, Any?) -> T,
): ReceiveChannel<T> {
    return Channel<T>(Channel.UNLIMITED)
        .apply { addHandler2(msg) { a, b -> trySend(transform(a, b)) } }
}

fun <T> Max.receive3AsChannel(
    msg: String,
    transform: (Any?, Any?, Any?) -> T,
): ReceiveChannel<T> {
    return Channel<T>(Channel.UNLIMITED)
        .apply { addHandler3(msg) { a, b, c -> trySend(transform(a, b, c)) } }
}

expect suspend fun Max.outlet(vararg values: Any?): Any?
