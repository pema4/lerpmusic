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

    private sealed class SessionLifecycleEvent {
        data class CanBeStarted(val start: () -> Unit) : SessionLifecycleEvent()
        data class Changed(val session: ConsensusSession?) : SessionLifecycleEvent()
    }

    private val sessions = ConcurrentHashMap<SessionId, StateFlow<SessionLifecycleEvent>>()

    private fun createSessionCoroutineScope() = CoroutineScope(SupervisorJob()) +
            CoroutineExceptionHandler { _, ex ->
                log.error(ex) { "Can't send response in background" }
            }

    /**
     * Возвращает [ConsensusSession], если сессия активна, или null, если нет.
     */
    fun getSession(id: SessionId): Flow<ConsensusSession?> =
        getOrStartSessionImpl(id)
            .filterIsInstance<SessionLifecycleEvent.Changed>()
            .map { it.session }

    /**
     * Возвращает [ConsensusSession], когда она стартует, если сессии нет — создаёт её.
     *
     * Сессия активна, пока выполняется возвращаемый [Flow].
     */
    fun getOrStartSession(id: SessionId): Flow<ConsensusSession?> =
        getOrStartSessionImpl(id)
            .onEach { if (it is SessionLifecycleEvent.CanBeStarted) it.start() }
            .filterIsInstance<SessionLifecycleEvent.Changed>()
            .map { it.session }

    private fun getOrStartSessionImpl(
        id: SessionId,
    ): Flow<SessionLifecycleEvent> {
        if (id !in expectedIds) return emptyFlow()

        val deferredSession = sessions.getOrPut(id) {
            val launchSession: Flow<SessionLifecycleEvent> = flow {
                val started = CompletableDeferred<Unit>()
                emit(SessionLifecycleEvent.CanBeStarted(start = { started.complete(Unit) }))
                started.await()

                coroutineScope {
                    val session = ConsensusSession(
                        id = id,
                        expectedPin = expectedSessionPin,
                        coroutineScope = this,
                    )
                    emit(SessionLifecycleEvent.Changed(session))

                    log.info("Session $id started: $session")
                    awaitCancellation()
                }
            }

            launchSession
                .retryWhen { ex, attempts ->
                    log.error(ex) { "Session $id exited unexpectedly, retry attempt #${attempts + 1}" }
                    emit(SessionLifecycleEvent.Changed(null))
                    true
                }
                .onCompletion {
                    // TODO: надо завершать сессию, когда отключатся все девайсы
                    log.info("Session $id completed successfully")
                    sessions.remove(id)
                }
                .stateIn(
                    scope = coroutineScope,
                    started = SharingStarted.WhileSubscribed(
                        stopTimeoutMillis = sessionKeepAlive.inWholeMilliseconds,
                        replayExpirationMillis = 0,
                    ),
                    initialValue = SessionLifecycleEvent.Changed(null),
                )
        }

        return deferredSession
    }
}

private val log = KotlinLogging.logger {}