package lerpmusic.btle.scrapper

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import lerpmusic.btle.domain.scrapper.Peripheral
import mu.KotlinLogging

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
    fun on(event: String, callback: (dynamic) -> Unit)
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
    val callback: (dynamic) -> Unit = callback@{ ev ->
        val peripheral = json.decodeFromString<Peripheral>(ev.toString())
        trySend(peripheral).onFailure {
            log.error(it) { "Can't emit 'discover' event for $ev" }
        }
    }

    on("discover", callback)
    startScanning(allowDuplicates = true)
    awaitClose {
        stopScanning()
    }
}

private val json = Json { ignoreUnknownKeys = true }

private val log = KotlinLogging.logger {}
