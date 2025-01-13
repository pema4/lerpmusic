package lerpmusic.consensus.device

import arrow.core.raise.nullable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

class ConsensusScript(
    private val max: Max,
) {
    @OptIn(FlowPreview::class)
    suspend fun mainLoop(): Nothing = coroutineScope {
        val serverHost: StateFlow<String?> = max.inlet("serverHost")
            .map { it.toString() }
            .stateIn(this, started = SharingStarted.Eagerly, initialValue = null)
        serverHost.onEach { max.post("Got serverHost of '$it'") }.launchIn(this)

        val sessionId: StateFlow<String?> = max.inlet("sessionId")
            .map { it.toString() }
            .stateIn(this, started = SharingStarted.Eagerly, initialValue = null)
        sessionId.onEach { max.post("Got sessionId of '$it'") }.launchIn(this)

        val sessionPin: StateFlow<String?> = max.inlet("sessionPin")
            .map { it.toString() }
            .stateIn(this, started = SharingStarted.Eagerly, initialValue = null)
        sessionPin.onEach { max.post("Got sessionPin of '$it'") }.launchIn(this)

        val session: Flow<ConsensusSession?> =
            combine(serverHost, sessionId, sessionPin) { serverHost, sessionId, sessionPin ->
                nullable {
                    ConsensusSession(
                        serverHost = serverHost.bind(),
                        sessionId = sessionId.bind(),
                        sessionPin = sessionPin.bind(),
                        max = max,
                    )
                }
            }

        session
            .filterNotNull()
            .onStart { max.outlet("status", "initialized") }
            .collectLatest { it.run() }

        error("Main loop should run indefinitely (until cancellation)")
    }
}
