package lerpmusic.btle.receiver

import kotlinx.coroutines.await
import kotlin.js.Promise

@JsModule("max-api")
@JsNonModule
actual external object Max {
    actual fun post(msg: Any?)

    actual fun addHandler(msg: Any?, handler: (Any?) -> Unit)

    @JsName("addHandler")
    actual fun addHandler2(msg: Any?, handler: (Any?, Any?) -> Unit)

    @JsName("addHandler")
    actual fun addHandler3(msg: Any?, handler: (Any?, Any?, Any?) -> Unit)

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

actual suspend fun Max.outlet(vararg values: Any?): Any? =
    outletAsync(values).await()
