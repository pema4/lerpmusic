package lerpmusic.consensus.device

import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.js.Promise

interface Max {
    fun post(vararg args: Any)
    suspend fun outlet(vararg values: Any)
    fun addHandler(selector: Any, handler: (Any) -> Unit): DisposableHandle
    fun addHandler2(selector: Any, handler: (Any, Any) -> Unit): DisposableHandle
    fun addHandler3(selector: Any, handler: (Any, Any, Any) -> Unit): DisposableHandle

    companion object Default : Max {
        private val api by lazy { js("require('max-api')") }

        override fun post(vararg args: Any): Unit =
            api.post(args)

        override suspend fun outlet(vararg values: Any): Unit =
            api.outlet(values).unsafeCast<Promise<Unit>>().await()

        override fun addHandler(selector: Any, handler: (Any) -> Unit): DisposableHandle =
            addHandlerImpl(selector, handler)

        override fun addHandler2(selector: Any, handler: (Any, Any) -> Unit): DisposableHandle =
            addHandlerImpl(selector, handler)

        override fun addHandler3(selector: Any, handler: (Any, Any, Any) -> Unit): DisposableHandle =
            addHandlerImpl(selector, handler)

        private fun addHandlerImpl(
            selector: Any,
            handler: Function<Unit>,
        ): DisposableHandle {
            api.addHandler(selector, handler)
            return DisposableHandle { api.removeHandler(selector, handler) }
        }
    }
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

fun Max.inlet2(selector: String, initialValue: Message2? = null): Flow<Message2?> =
    callbackFlow {
        val handler = addHandler2(selector) { a, b -> trySend(Message2(a, b)) }
        trySend(initialValue)
        awaitClose { handler.dispose() }
    }

data class Message3(
    val a: Any,
    val b: Any,
    val c: Any,
)

fun Max.inlet3(selector: String, initialValue: Message3? = null): Flow<Message3?> =
    callbackFlow {
        val handler = addHandler3(selector) { a, b, c -> trySend(Message3(a, b, c)) }
        trySend(initialValue)
        awaitClose { handler.dispose() }
    }
