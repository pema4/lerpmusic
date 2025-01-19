package lerpmusic.website.consensus

import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import lerpmusic.consensus.*
import lerpmusic.consensus.utils.collectAddedInChildCoroutine
import mu.KotlinLogging
import kotlin.math.sign

class SessionComposition(
    private val sessionScope: CoroutineScope,
) : Composition {
    private val activeDevices: MutableStateFlow<List<SessionDevice>> = MutableStateFlow(emptyList())
    override val isListenersCountRequested: Flow<Boolean> = flowOf(true)

    init {
        sessionScope.launch {
            activeDevices.collectAddedInChildCoroutine { it.receiveMessages() }
        }
    }

    fun addDevice(connection: DeviceConnection) {
        sessionScope.launch(CoroutineName("DeviceConnectionCompletionHandler")) {
            val newDevice = SessionDevice(
                connection = connection,
            )

            activeDevices.update { devices ->
                check(devices.none { it.connection.id == connection.id }) { "Connection $connection already exists" }
                devices + newDevice
            }
            log.info { "Device ${connection.id} connected" }

            // При отключении удаляемся из списка
            try {
                connection.coroutineContext.job.join()
            } finally {
                activeDevices.update { devices -> devices - newDevice }
                log.info { "Device ${connection.id} disconnected" }
            }
        }
    }

    override suspend fun updateListenersCount(count: Int) {
        activeDevices.collectAddedInChildCoroutine { device ->
            device.isListenersCountRequested.collectLatest { requested ->
                if (requested) {
                    device.updateListenersCount(count)
                }
            }
        }
    }

    override val events: Flow<NoteEvent> = emptyFlow()

    override suspend fun play(ev: NoteEvent) {
    }

    private val intensityRequestedCount: Flow<Int> = channelFlow {
        activeDevices.collectAddedInChildCoroutine { device ->
            // Для перехода false -> true возвращаем 1, для true -> false - -1
            // [false, true, false, true] -> [0, 1, -1, 1]
            // [true, false, true] -> [0, 1, -1, 1]
            var previous = false
            device.isIntensityRequested
                .onCompletion { if (previous) send(-1) }
                .collect { requested ->
                    val delta = (requested compareTo previous).sign
                    previous = requested
                    send(delta)
                }
        }
    }.runningFold(0, Int::plus)

    /**
     * Нужно ли запрашивать [Audience.intensityUpdates].
     */
    override val isIntensityRequested: StateFlow<Boolean> =
        intensityRequestedCount
            .onEach { log.info { "intensityRequestedCount: $it" } }
            .map { it > 0 }
            .stateIn(sessionScope, SharingStarted.Eagerly, initialValue = false)

    override suspend fun updateIntensity(update: IntensityUpdate) {
        val jobs = activeDevices.value
            .filter { it.isListenersCountRequested.value }
            .map { device ->
                device.connection.launch { device.updateIntensity(update) }
            }

        try {
            jobs.joinAll()
        } finally {
            jobs.forEach { it.cancel() }
        }
    }
}

class SessionDevice(
    val connection: DeviceConnection,
) : Composition, CoroutineScope by connection {
    private val _isIntensityRequested = MutableStateFlow(false)
    override val isIntensityRequested: StateFlow<Boolean> = _isIntensityRequested.asStateFlow()

    private val _isListenersCountRequested = MutableStateFlow(false)
    override val isListenersCountRequested: StateFlow<Boolean> = _isIntensityRequested.asStateFlow()

    suspend fun receiveMessages(): Nothing {
        while (true) {
            when (val event = connection.receive()) {
                DeviceRequest.Ping -> connection.send(DeviceResponse.Pong)
                is DeviceRequest.AskNote -> {}
                is DeviceRequest.CancelNote -> {}
                DeviceRequest.ReceiveIntensityUpdates -> _isIntensityRequested.value = true
                DeviceRequest.CancelIntensityUpdates -> _isIntensityRequested.value = false
                is DeviceRequest.ReceiveListenersCount -> _isListenersCountRequested.value = event.receive
            }
        }
    }

    override suspend fun updateListenersCount(count: Int) {
        connection.send(DeviceResponse.ListenersCount(count))
    }

    override val events: Flow<NoteEvent>
        get() = emptyFlow()

    override suspend fun play(ev: NoteEvent) {
    }

    override suspend fun updateIntensity(update: IntensityUpdate) {
        connection.send(DeviceResponse.IntensityUpdate(update.decrease, update.increase))
    }
}

class DeviceConnection(
    val id: String,
    private val webSocketSession: WebSocketServerSession,
    private val coroutineScope: CoroutineScope,
) : CoroutineScope by coroutineScope {
    suspend fun send(data: DeviceResponse) {
        return webSocketSession.sendSerialized(data)
    }

    suspend fun receive(): DeviceRequest {
        return webSocketSession.receiveDeserialized<DeviceRequest>()
    }
}

private val log = KotlinLogging.logger {}

