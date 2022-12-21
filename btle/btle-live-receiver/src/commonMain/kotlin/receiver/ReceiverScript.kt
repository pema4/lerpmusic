package lerpmusic.btle.receiver

import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import lerpmusic.btle.domain.note.MpeEvent
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ReceiverScript(
    private val max: Max,
) {
    private val serverHost: Flow<String?> =
        max.receiveAsState("serverHost", initial = null) {
            it?.toString()
        }

    private val sessionId: Flow<String?> =
        max.receiveAsState("sessionId", initial = null) {
            it?.toString()
        }

    private val bucketStart: Flow<Int?> =
        max.receiveAsState("bucketStart", initial = null) {
            it?.toString()?.toIntOrNull()
        }

    private val bucketLength: Flow<Int?> =
        max.receiveAsState("bucketLength", initial = null) {
            it?.toString()?.toIntOrNull()
        }

    suspend fun start(): Unit = coroutineScope {
        launchEventLogging()

        val configurations =
            combine(
                serverHost,
                sessionId,
                bucketStart,
                bucketLength
            ) { serverHost, sessionId, bucketStart, bucketLength ->
                ReceiverClient(
                    serverHost = serverHost ?: return@combine null,
                    sessionId = sessionId ?: return@combine null,
                    bucketStart = bucketStart ?: return@combine null,
                    bucketLength = bucketLength?: return@combine null,
                    max = max,
                )
            }

        configurations
            .filterNotNull()
            .debounce(0.2.seconds)
            .mapLatest { client ->
                while (true) {
                    try {
                        coroutineScope {
                            launch { client.start() }
                            max.outlet("status", "running")
                        }
                    } catch (ex: CancellationException) {
                        max.outlet("status", "stopped")
                        ex.printStackTrace()
                        delay(1.seconds)
                    } catch (ex: Throwable) {
                        max.outlet("status", "stopped")
                        ex.printStackTrace()
                        delay(1.seconds)
                    }
                }
            }
            .launchIn(this)

        max.outlet("status", "initialized")
    }

    private fun CoroutineScope.launchEventLogging() {
        serverHost
            .onEach { max.post("Got serverHost of '$it'") }
            .launchIn(this)

        sessionId
            .onEach { max.post("Got sessionId of '$it'") }
            .launchIn(this)

        bucketStart
            .onEach { max.post("Got bucketStart of '$it'") }
            .launchIn(this)

        bucketLength
            .onEach { max.post("Got bucketLength of '$it'") }
            .launchIn(this)
    }
}

fun ReceiverScript.launchIn(scope: CoroutineScope) =
    scope.launch { start() }
