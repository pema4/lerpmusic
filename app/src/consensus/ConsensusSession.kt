package lerpmusic.website.consensus

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import lerpmusic.consensus.Consensus
import lerpmusic.consensus.SessionId
import lerpmusic.consensus.SessionPin
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
        val consensus = Consensus(composition, audience)
        coroutineScope.launch { consensus.filterCompositionEvents() }
        coroutineScope.launch { consensus.receiveIntensityUpdates() }
    }

    fun addDevice(connection: DeviceConnection, pin: SessionPin) {
        check(pin == expectedPin)
        composition.addDevice(connection)
    }

    fun addListener(connection: ListenerConnection) {
        audience.addListener(connection)
    }
}

class ConsensusSessionLauncher(
    private val expectedSessionPin: SessionPin,
    private val sessionKeepAlive: Duration = 15.seconds
) {
    private val coroutineScope: CoroutineScope = CoroutineScope(Job())
    private val expectedIds = (10..14).map { SessionId(it.toString()) }

    private val sessions = ConcurrentHashMap<SessionId, SharedFlow<ConsensusSession>>()

    /**
     * Возвращает [ConsensusSession], когда она стартует, если сессии нет — создаёт её.
     *
     * Сессия активна, пока выполняется возвращаемый [Flow].
     */
    fun getSession(id: SessionId): Flow<ConsensusSession> {
        if (id !in expectedIds) return emptyFlow()

        val deferredSession = sessions.getOrPut(id) {
            val launchSession: Flow<ConsensusSession> = flow {
                coroutineScope {
                    val session = ConsensusSession(
                        id = id,
                        expectedPin = expectedSessionPin,
                        coroutineScope = this,
                    )
                    emit(session)

                    log.info("Session $id started: $session")
                    awaitCancellation()
                }
            }

            launchSession
                .retryWhen { ex, attempts ->
                    log.error(ex) { "Session $id exited unexpectedly, retry attempt #${attempts + 1}" }
                    true
                }
                .onCompletion {
                    // TODO: надо завершать сессию, когда отключатся все девайсы
                    log.info("Session $id completed successfully")
                    sessions.remove(id)
                }
                .shareIn(
                    scope = coroutineScope,
                    started = SharingStarted.WhileSubscribed(
                        stopTimeoutMillis = sessionKeepAlive.inWholeMilliseconds,
                        replayExpirationMillis = 0,
                    ),
                    replay = 1,
                )
        }

        return deferredSession
    }
}

private val log = KotlinLogging.logger {}