package lerpmusic.website.consensus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import lerpmusic.consensus.Audience
import lerpmusic.consensus.IntensityUpdate
import lerpmusic.consensus.ListenerRequest
import lerpmusic.consensus.ListenerResponse
import lerpmusic.consensus.utils.flatMapConnected
import lerpmusic.consensus.utils.onEachConnected
import lerpmusic.consensus.utils.receiveConnections
import lerpmusic.consensus.utils.receiveMessagesWithSubscription
import mu.KotlinLogging

/**
 * Аудитория — несколько слушателей.
 */
class SessionAudience(
    private val sessionScope: CoroutineScope,
) : Audience {
    private val listenerConnections = sessionScope.receiveConnections<SessionListener>()
    private val connectedListeners: StateFlow<List<SessionListener>> = listenerConnections.connected
        .onEachConnected { it.receiveMessages() }
        .stateIn(sessionScope, started = SharingStarted.Eagerly, emptyList())

    override val listenersCount: StateFlow<Int> =
        connectedListeners
            .map { it.size }
            .onEach { log.info { "listenersCount: $it" } }
            .stateIn(sessionScope, SharingStarted.Eagerly, initialValue = 0)

    /**
     * Добавление слушателя в сессию.
     *
     * Удаление произойдёт автоматически при отключении слушателя.
     */
    suspend fun addListener(connection: ListenerConnection) {
        val newListener = SessionListener(connection)
        listenerConnections.add(newListener)
    }

    override val intensityUpdates: Flow<IntensityUpdate> =
        connectedListeners.flatMapConnected { it.intensityUpdates }
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

    suspend fun receiveMessages() {
        connection.incoming.collect { event ->
            when (event) {
                ListenerRequest.Action -> {}
                ListenerRequest.DecreaseIntensity -> receivedIntensityUpdates.receive(IntensityUpdate(1.0, 0.0))
                ListenerRequest.IncreaseIntensity -> receivedIntensityUpdates.receive(IntensityUpdate(0.0, 1.0))
            }
        }
    }

    override fun toString(): String {
        return "SessionListener(id=${connection.id})"
    }
}

private val log = KotlinLogging.logger {}