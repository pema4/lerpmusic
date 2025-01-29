package lerpmusic.website.consensus

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.slf4j.MDCContext
import lerpmusic.consensus.SessionId
import lerpmusic.consensus.SessionPin
import mu.KotlinLogging
import org.slf4j.MDC
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
     * Если сессия ещё не запущена или уже завершилась, возвращается `null`.
     * При завершении ошибкой сессия создастся заново.
     *
     * Сессия активна, пока выполняется возвращаемый [kotlinx.coroutines.flow.Flow].
     */
    fun getSession(id: SessionId): Flow<ConsensusSession?> {
        check(id in expectedIds) { "No session with id $id" }

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

            val sessionJob = Job(launcherScope.coroutineContext.job)
            launchSession
                .onEach { log.info("Session started") }
                .retryWhen { ex, attempts ->
                    log.warn(ex) { "Session exited unexpectedly on attempt #$attempts, retrying..." }
                    delay(1.seconds)
                    emit(null)
                    true
                }
                .onCompletion { ex ->
                    when (ex) {
                        null -> {
                            log.info("Session completed normally")
                            emit(null)
                        }

                        is CancellationException -> log.info("Session cancelled")
                        else -> log.warn("Session completed", ex)
                    }

                    sessions -= id
                    sessionJob.cancel()
                }
                .stateIn(
                    scope = launcherScope +
                            sessionJob +
                            MDCContext(MDC.getCopyOfContextMap() + ("call-id" to "session-${id.value}")) +
                            CoroutineName("session-handler"),
                    started = SharingStarted.Companion.WhileSubscribed(
                        stopTimeoutMillis = sessionKeepAlive.inWholeMilliseconds,
                        replayExpirationMillis = 0,
                    ),
                    initialValue = null,
                )
        }

        return session
    }

    fun shutdown() {
        launcherScope.cancel("Application shutdown")
    }
}

private val log = KotlinLogging.logger {}
