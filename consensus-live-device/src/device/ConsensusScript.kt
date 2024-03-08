package lerpmusic.consensus.device

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import lerpmusic.consensus.NoteEvent
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ConsensusScript(
    private val max: Max,
    private val httpClientEngineFactory: HttpClientEngineFactory<*>,
) {
    private val noteEvents: ReceiveChannel<NoteEvent?> =
        max.receive3AsChannel("midiIn") transform@{ a, b, c ->
            NoteEvent.fromRaw(
                channel = a as? Int ?: return@transform null,
                pitch = b as? Int ?: return@transform null,
                velocity = c as? Int ?: return@transform null,
            )
        }

    private val serverHost: Flow<String?> =
        max.receiveAsState("serverHost", initial = null) {
            it?.toString()
        }

    private val sessionId: Flow<String?> =
        max.receiveAsState("sessionId", initial = null) {
            it?.toString()
        }

    private val sessionPin: Flow<String?> =
        max.receiveAsState("sessionPin", initial = null) {
            it?.toString()
        }

    suspend fun start(): Unit = coroutineScope {
        launchEventLogging()

        fun play(ev: NoteEvent) {
            launch {
                when (ev) {
                    is NoteEvent.NoteOn ->
                        max.outlet("midiOut", ev.note.channel, ev.note.pitch, ev.velocity)

                    is NoteEvent.NoteOff ->
                        max.outlet("midiOut", ev.note.channel, ev.note.pitch, 0)
                }
            }
        }

        val configurations =
            combine(serverHost, sessionId, sessionPin) { serverHost, sessionId, sessionPin ->
                ConsensusClient(
                    serverHost = serverHost ?: return@combine null,
                    sessionId = sessionId ?: return@combine null,
                    sessionPin = sessionPin ?: return@combine null,
                    noteEvents = noteEvents,
                    play = ::play,
                    httpClientEngineFactory = httpClientEngineFactory,
                    max = max,
                )
            }

        configurations
            .filterNotNull()
            .mapLatest { client ->
                while (true) {
                    try {
                        coroutineScope {
                            launch { client.start() }
                            max.outlet("status", "running")
                        }
                    } catch (ex: CancellationException) {
                        max.outlet("status", "stopped")
                        throw ex
                    } catch (ex: Throwable) {
                        max.outlet("status", "stopped")
                        ex.printStackTrace()
                        delay(2.seconds)
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

        sessionPin
            .onEach { max.post("Got sessionPin of '$it'") }
            .launchIn(this)
    }
}

fun ConsensusScript.launchIn(scope: CoroutineScope) =
    scope.launch { start() }
