package lerpmusic.btle.scrapper

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@JsModule("@abandonware/noble")
@JsNonModule
external object Noble {
    fun startScanning(
        xs: List<Any> = definedExternally,
        allowDuplicates: Boolean = definedExternally,
        errorCallback: (Any) -> Unit = definedExternally,
    )

    fun stopScanning()
    fun reset()
    fun on(event: String, callback: (Peripheral) -> Unit)
}

//fun Noble.startScanning(
//    xs: List<Any>,
//    allowDuplicates: Boolean,
//    errorCallback: (Any) -> Unit,
//) {
//
//}

data class NobleSettings(
    val extended: Boolean = false,
)

fun Noble(settings: NobleSettings? = null): Noble =
    if (settings == null) {
        Noble
    } else {
        error("why?")
//        NobleConstructor.asDynamic()(settings) as Noble
    }

fun Noble.discover(): Flow<Peripheral> = callbackFlow {
    val callback: (Peripheral) -> Unit = { ev -> println(Json.encodeToString(ev)) }
    try {
        on("discover", callback)
        startScanning(allowDuplicates = true)
        awaitCancellation()
    } finally {
        stopScanning()
    }
}
