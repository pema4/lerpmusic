package lerpmusic.consensus.device

import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.js.Promise

@Suppress("unused")
@JsModule("max-api")
@JsNonModule
external object MaxApi {
    fun post(vararg args: Any)
    fun addHandler(selector: Any, handler: Function<Unit>)
    fun removeHandler(selector: Any, handler: Function<Unit>)
    fun outlet(vararg values: Any): Promise<Unit>

    val MESSAGE_TYPES: MaxMessageTypes
}

fun interface DisposableHandle {
    fun dispose()
}

object Max {
    private val api = MaxApi

    fun post(vararg args: Any): Unit =
        api.post(*args)

    suspend fun outlet(vararg values: Any): Unit =
        api.outlet(*values).await()

    fun addHandler(selector: Any, handler: (Any) -> Unit): DisposableHandle =
        addHandlerImpl(selector, handler)

    fun addHandler2(selector: Any, handler: (Any, Any) -> Unit): DisposableHandle =
        addHandlerImpl(selector, handler)

    fun addHandler3(selector: Any, handler: (Any, Any, Any) -> Unit): DisposableHandle =
        addHandlerImpl(selector, handler)

    private fun addHandlerImpl(
        selector: Any,
        handler: dynamic,
    ): DisposableHandle {
        api.addHandler(selector, handler)
        return DisposableHandle { api.removeHandler(selector, handler) }
    }
}

object MaxMessageTypes {
    const val ALL = "ALL"
    const val BANG = "BANG"
    const val DICT = "DICT"
    const val NUMBER = "NUMBER"
    const val LIST = "LIST"
}

fun Max.inlet(selector: String, initialValue: Any? = null): Flow<Any?> =
    callbackFlow {
        val handler = addHandler(selector) { a -> trySend(a) }
        trySend(initialValue)
        awaitClose { handler.dispose() }
    }

data class Message2(
    val a: Any,
    val b: Any,
)

fun Max.inlet2(selector: String): Flow<Message2> =
    callbackFlow {
        val handler = addHandler2(selector) { a, b -> trySend(Message2(a, b)) }
        awaitClose { handler.dispose() }
    }

data class Message3(
    val a: Any,
    val b: Any,
    val c: Any,
)

fun Max.inlet3(selector: String): Flow<Message3> =
    callbackFlow {
        val handler = addHandler3(selector) { a, b, c -> trySend(Message3(a, b, c)) }
        awaitClose { handler.dispose() }
    }
