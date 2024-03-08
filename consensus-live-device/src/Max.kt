package lerpmusic.consensus.device

import kotlinx.coroutines.await
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlin.js.Promise

@JsModule("max-api")
@JsNonModule
external object Max {
    fun post(msg: Any?)

    fun addHandler(msg: Any?, handler: (Any?) -> Unit)

    @JsName("addHandler")
    fun addHandler2(msg: Any?, handler: (Any?, Any?) -> Unit)

    @JsName("addHandler")
    fun addHandler3(msg: Any?, handler: (Any?, Any?, Any?) -> Unit)

    @JsName("outlet")
    fun outletAsync(vararg values: Any?): Promise<Any?>

    val MESSAGE_TYPES: MaxMessageTypes
}

object MaxMessageTypes {
    const val ALL = "ALL"
    const val BANG = "BANG"
    const val DICT = "DICT"
    const val NUMBER = "NUMBER"
    const val LIST = "LIST"
}

suspend fun Max.outlet(vararg values: Any?): Any? =
    outletAsync(values).await()

fun Max.inlet(selector: String): Flow<Any?> =
    callbackFlow {
        addHandler(selector) { a -> trySend(a) }
        awaitClose()
    }

data class Message2(
    val a: Any?,
    val b: Any?,
)

fun Max.inlet2(selector: String): Flow<Message2> =
    callbackFlow {
        addHandler2(selector) { a, b -> trySend(Message2(a, b)) }
        awaitClose()
    }

data class Message3(
    val a: Any?,
    val b: Any?,
    val c: Any?,
)

fun Max.inlet3(selector: String): Flow<Message3> =
    callbackFlow {
        addHandler3(selector) { a, b, c -> trySend(Message3(a, b, c)) }
        awaitClose()
    }
