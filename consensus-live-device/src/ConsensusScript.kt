package lerpmusic.consensus.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import app.cash.molecule.moleculeFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
@Composable
fun ConsensusScript(max: Max) {
    val serverHost: String? by produceState(initialValue = null) {
        max.inlet("serverHost")
            .debounce(1.seconds)
            .collect { value = it?.toString() }
    }
    LaunchedEffect(serverHost) { max.post("Got serverHost of '$serverHost'") }

    val sessionId: String? by produceState(initialValue = null) {
        max.inlet("sessionId")
            .debounce(1.seconds)
            .collect { value = it?.toString() }
    }
    LaunchedEffect(sessionId) { max.post("Got sessionId of '$sessionId'") }

    val sessionPin: String? by produceState(null) {
        max.inlet("sessionPin")
            .debounce(1.seconds)
            .collect { value = it?.toString() }
    }
    LaunchedEffect(sessionPin) { max.post("Got sessionPin of '$sessionPin'") }

    LaunchedEffect(serverHost, sessionId, sessionPin) {
        if (serverHost != null && sessionId != null && sessionPin != null) {
            val client = ConsensusClient(
                serverHost = serverHost!!,
                sessionId = sessionId!!,
                sessionPin = sessionPin!!,
                max = max,
            )
            client.run()
        }
    }

    LaunchedEffect(Unit) { max.outlet("status", "initialized") }
}
