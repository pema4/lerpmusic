package lerpmusic.website.consensus

import io.ktor.server.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import lerpmusic.consensus.Audience
import lerpmusic.consensus.ListenerRequest
import lerpmusic.consensus.ListenerResponse
import lerpmusic.consensus.Note
import lerpmusic.consensus.utils.runningSetDifference
import kotlin.uuid.Uuid

/**
 * Аудитория — несколько слушателей.
 */
class SessionAudience(
    private val coroutineScope: CoroutineScope,
) : Audience {
    private val activeListeners: MutableStateFlow<List<SessionListener>> = MutableStateFlow(emptyList())

    init {
        coroutineScope.launch {
            activeListeners.collectEachAddedListener { it.receiveMessages() }
        }
    }

    /**
     * Добавление слушателя в сессию.
     *
     * Удаление произойдёт автоматически при отключении слушателя.
     */
    fun addListener(connection: ListenerConnection) {
        val newListener = SessionListener(
            connection = connection,
        )

        activeListeners.update { listeners ->
            check(listeners.none { it.connection == connection }) { "Connection $connection already exists" }
            listeners + newListener
        }

        // При отключении слушателя удаляем его из списка
        connection.coroutineContext.job.invokeOnCompletion {
            activeListeners.update { listeners -> listeners - newListener }
        }
    }

    override val intensityUpdates: Flow<Double> = channelFlow {
        activeListeners.collectEachAddedListener { listener ->
            listener.intensityUpdates.collect { send(it) }
        }
    }

    override suspend fun shouldPlayNote(note: Note): Boolean {
        TODO("Not yet implemented")
    }

    override fun cancelNote(note: Note) {
        TODO("Not yet implemented")
    }

    private suspend fun StateFlow<List<SessionListener>>.collectEachAddedListener(
        block: suspend CoroutineScope.(SessionListener) -> Unit,
    ): Nothing {
        // TODO: как лучше всего отменять дочерние корутины при отмене выполнения collectEachAddedListener?
        //  В наивном варианте с invokeOnCompletion будет утекать память:
        //  val newJobs = listeners.added.map { listener ->
        //      listener.connection.launch { block(listener) }
        //  }
        //  currentCoroutineContext().job.invokeOnCompletion { cause ->
        //      newJobs.forEach { it.cancel() }
        //  }
        val launchedJobs = mutableMapOf<SessionListener, Job>()

        runningSetDifference()
            .onEach { listeners ->
                for (listener in listeners.added) {
                    launchedJobs[listener] = listener.connection.launch { block(listener) }
                }

                // эти джобы отменятся сами
                for (listener in listeners.removed) {
                    launchedJobs.remove(listener)
                }
            }
            .onCompletion { cause ->
                launchedJobs.values.forEach {
                    it.cancel("collectEachAddedListener cancelled", cause)
                }
            }
            .collect()

        error("Unreachable, should run forever")
    }
}

/**
 * Один слушатель.
 */
private class SessionListener(
    val connection: ListenerConnection,
) : Audience {
    /**
     * Небольшой абьюз [SharedFlow] и [SharingStarted.WhileSubscribed]:
     * - при первой подписке на [intensityUpdates] будет отправлен запрос слушателю
     * - при последней отписке слушателю отправится уведомление об отмене
     * - если подписки нет, сообщения от слушателя бросаются на пол
     *
     * Эта конструкция заменяет атомарный флаг + дублирование логики подписки/отписки в [intensityUpdates]
     */
    private val receivedIntensityUpdates: StateFlow<MutableSharedFlow<Double>?> = flow {
        // 1. начинаем слушать сообщения от слушателя
        // Это происходит до отправки запроса, чтобы не просыпать никакие ответы
        emit(MutableSharedFlow<Double>())

        // 2. уведомляем слушателя о том, что хотим получать уведомления об интенсивности.
        connection.send(ListenerResponse.ReceiveIntensityUpdates)

        try {
            // 3. слушаем бесконечно — пока кто-то подписан на эти уведомления
            awaitCancellation()
        } finally {
            // 4. Компенсация шага 1 — отменяем уведомление,
            connection.launch {
                connection.send(ListenerResponse.CancelIntensityUpdates)
            }
        }
    }.stateIn(connection, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), null)

    /**
     * Изменения интенсивности
     */
    override val intensityUpdates: Flow<Double> = receivedIntensityUpdates.flatMapLatest { it ?: emptyFlow() }

    suspend fun receiveMessages(): Nothing {
        while (true) {
            when (val event = connection.receive()) {
                ListenerRequest.Action -> TODO()
                ListenerRequest.DecreaseIntensity -> receivedIntensityUpdates.value?.emit(-1.0)
                ListenerRequest.IncreaseIntensity -> receivedIntensityUpdates.value?.emit(1.0)
            }
        }
    }

    override suspend fun shouldPlayNote(note: Note): Boolean {
        TODO("Not yet implemented")
    }

    override fun cancelNote(note: Note) {
        TODO("Not yet implemented")
    }
}

class ListenerConnection(
    val id: Uuid,
    private val webSocketSession: WebSocketServerSession,
    private val coroutineScope: CoroutineScope = webSocketSession,
) : CoroutineScope by coroutineScope {
    suspend fun send(data: ListenerResponse): Unit = webSocketSession.sendSerialized(data)
    suspend fun receive(): ListenerRequest = webSocketSession.receiveDeserialized()
}
