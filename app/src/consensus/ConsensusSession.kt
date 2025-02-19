package lerpmusic.website.consensus

import kotlinx.coroutines.CoroutineScope
import lerpmusic.consensus.launchConsensus
import mu.KotlinLogging

@JvmInline
value class SessionId(val value: String)

@JvmInline
value class SessionPin(val value: String) {
    init {
        check(value matches pinRegex) { "Pin $value does not match $pinRegex" }
    }

    companion object {
        val pinRegex = "[a-zA-Z0-9]+".toRegex()
    }
}


/**
 * Session
 */
class ConsensusSession(
    val id: SessionId,
    private val expectedPin: SessionPin,
    private val coroutineScope: CoroutineScope,
) {
    private val composition = SessionComposition(coroutineScope)
    private val audience = SessionAudience(coroutineScope)

    init {
        coroutineScope.launchConsensus(composition, audience)
    }

    suspend fun addDevice(connection: DeviceConnection, pin: SessionPin) {
        check(pin == expectedPin)
        composition.addDevice(connection)
    }

    suspend fun addListener(connection: ListenerConnection) {
        audience.addListener(connection)
    }
}

private val log = KotlinLogging.logger {}