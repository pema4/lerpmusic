package lerpmusic.consensus.device

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import lerpmusic.consensus.NoteEvent
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ConsensusScript(
    private val max: Max,
    private val httpClientEngineFactory: HttpClientEngineFactory<*>,
    private val coroutineScope: CoroutineScope,
) {
    private val noteEvents: ReceiveChannel<NoteEvent?> =
        max.inlet3("midiIn")
            .mapNotNull { (a, b, c) ->
                NoteEvent.fromRaw(
                    channel = a as? Int ?: return@mapNotNull null,
                    pitch = b as? Int ?: return@mapNotNull null,
                    velocity = c as? Int ?: return@mapNotNull null,
                )
            }
            .buffer(Channel.UNLIMITED)
            .produceIn(coroutineScope)

    private val serverHost: Flow<String> =
        max.inlet("serverHost")
            .onEach { max.post("Got serverHost of '$it'") }
            .stateIn(coroutineScope, SharingStarted.Eagerly, initialValue = null)
            .mapNotNull { it?.toString() }

    private val sessionId: Flow<String> =
        max.inlet("sessionId")
            .onEach { max.post("Got sessionId of '$it'") }
            .stateIn(coroutineScope, SharingStarted.Eagerly, initialValue = null)
            .mapNotNull { it?.toString() }

    private val sessionPin: Flow<String> =
        max.inlet("sessionPin")
            .onEach { max.post("Got sessionPin of '$it'") }
            .stateIn(coroutineScope, SharingStarted.Eagerly, initialValue = null)
            .mapNotNull { it?.toString() }

    suspend fun start(): Nothing = coroutineScope {
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
                    serverHost = serverHost,
                    sessionId = sessionId,
                    sessionPin = sessionPin,
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

        awaitCancellation()
    }
}

fun ConsensusScript.launchIn(scope: CoroutineScope) =
    scope.launch { start() }
