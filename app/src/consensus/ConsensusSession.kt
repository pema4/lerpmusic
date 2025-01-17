package lerpmusic.website.consensus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.slf4j.MDCContext
import lerpmusic.consensus.SessionId
import lerpmusic.consensus.SessionPin
import lerpmusic.consensus.launchConsensus
import mu.KotlinLogging
import org.slf4j.MDC
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
        coroutineScope.launchConsensus(composition, audience)
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
    private val launcherScope: CoroutineScope = CoroutineScope(Job())
    private val expectedIds = (10..14).map { SessionId(it.toString()) }

    private val sessions = ConcurrentHashMap<SessionId, StateFlow<ConsensusSession?>>()

    /**
     * Возвращает [ConsensusSession], когда она стартует, если сессии нет — создаёт её.
     *
     * Если сессия не запущена или завершилась ошибкой, возвращается `null`
     *
     * Сессия активна, пока выполняется возвращаемый [Flow].
     */
    fun getSession(id: SessionId): Flow<ConsensusSession?> {
        if (id !in expectedIds) return emptyFlow()

        val session = sessions.computeIfAbsent(id) {
            val launchSession: Flow<ConsensusSession?> = flow {
                coroutineScope {
                    val session = ConsensusSession(
                        id = id,
                        expectedPin = expectedSessionPin,
                        coroutineScope = this,
                    )
                    emit(session)
                }
            }

            launchSession
                .onEach { log.info("Session started") }
                .retryWhen { ex, attempts ->
                    log.warn(ex) { "Session exited unexpectedly on attempt #$attempts, retrying..." }
                    emit(null)
                    true
                }
                .onCompletion {
                    log.info("Session completed successfully")
                    sessions -= id
                }
                .flowOn(MDCContext(MDC.getCopyOfContextMap() + ("call-id" to "session-${id.value}")))
                .stateIn(
                    scope = launcherScope,
                    started = SharingStarted.WhileSubscribed(
                        stopTimeoutMillis = sessionKeepAlive.inWholeMilliseconds,
                        replayExpirationMillis = 0,
                    ),
                    initialValue = null,
                )
        }

        return session
    }
}

private val log = KotlinLogging.logger {}