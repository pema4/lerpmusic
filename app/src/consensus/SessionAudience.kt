package lerpmusic.website.consensus

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import lerpmusic.consensus.Audience
import lerpmusic.consensus.IntensityUpdate
import lerpmusic.consensus.ListenerRequest
import lerpmusic.consensus.ListenerResponse
import lerpmusic.consensus.utils.collectConnected
import lerpmusic.consensus.utils.flatMapConnected
import lerpmusic.consensus.utils.receiveMessagesWithSubscription
import mu.KotlinLogging

/**
 * Аудитория — несколько слушателей.
 */
class SessionAudience(
    private val sessionScope: CoroutineScope,
) : Audience {
    private val activeListeners: MutableStateFlow<List<SessionListener>> = MutableStateFlow(emptyList())

    init {
        sessionScope.launch {
            activeListeners.collectConnected { it.receiveMessage() }
        }
    }

    override val listenersCount: StateFlow<Int> =
        activeListeners
            .map { it.size }
            .onEach { log.info { "listenersCount: $it" } }
            .stateIn(sessionScope, SharingStarted.Eagerly, initialValue = 0)

    /**
     * Добавление слушателя в сессию.
     *
     * Удаление произойдёт автоматически при отключении слушателя.
     */
    fun addListener(connection: ListenerConnection) {
        sessionScope.launch(CoroutineName("ListenerConnectionCompletionHandler")) {
            val newListener = SessionListener(
                connection = connection,
            )

            activeListeners.update { listeners ->
                check(listeners.none { it.connection.id == connection.id }) { "Connection $connection already exists" }
                listeners + newListener
            }
            log.info { "Listener ${connection.id} connected" }

            // При отключении слушателя удаляем его из списка
            try {
                connection.coroutineContext.job.join()
            } finally {
                activeListeners.update { listeners -> listeners - newListener }
                log.info { "Listener ${connection.id} disconnected" }
            }
        }
    }

    override val intensityUpdates: Flow<IntensityUpdate> =
        activeListeners.flatMapConnected { it.intensityUpdates }
}

/**
 * Один слушатель.
 */
private class SessionListener(
    val connection: ListenerConnection,
) : Audience, CoroutineScope by connection {
    override val listenersCount: Flow<Int> = flowOf(1)

    private val receivedIntensityUpdates = connection.receiveMessagesWithSubscription<IntensityUpdate>(
        onStart = { connection.send(ListenerResponse.ReceiveIntensityUpdates) },
        onCancellation = { connection.send(ListenerResponse.CancelIntensityUpdates) },
    )

    /**
     * Изменения интенсивности
     */
    override val intensityUpdates: Flow<IntensityUpdate> = receivedIntensityUpdates.incoming

    suspend fun receiveMessage() {
        connection.incoming.collect { event ->
            when (event) {
                ListenerRequest.Action -> {}
                ListenerRequest.DecreaseIntensity -> receivedIntensityUpdates.receive(IntensityUpdate(1.0, 0.0))
                ListenerRequest.IncreaseIntensity -> receivedIntensityUpdates.receive(IntensityUpdate(0.0, 1.0))
            }
        }
    }
}

private val log = KotlinLogging.logger {}