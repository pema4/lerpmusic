package lerpmusic.website.consensus

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import lerpmusic.consensus.*
import lerpmusic.consensus.utils.collectConnected
import lerpmusic.consensus.utils.onEachConnected
import lerpmusic.consensus.utils.receiveConnections
import lerpmusic.consensus.utils.runningCountConnected
import mu.KotlinLogging

class SessionComposition(
    private val sessionScope: CoroutineScope,
) : Composition {
    private val deviceConnections = sessionScope.receiveConnections<SessionDevice>()
    private val connectedDevices: StateFlow<List<SessionDevice>> = deviceConnections.connected
        .onEachConnected { it.receiveMessages() }
        .stateIn(sessionScope, started = SharingStarted.Eagerly, emptyList())

    suspend fun addDevice(connection: DeviceConnection) {
        val newDevice = SessionDevice(connection)
        deviceConnections.add(newDevice)
    }

    override val isListenersCountRequested: Flow<Boolean> =
        connectedDevices
            .runningCountConnected { it.isListenersCountRequested }
            .onEach { log.info { "listenersCountRequested: $it" } }
            .map { it > 0 }
            .flowOn(CoroutineName("session-count-listeners-requested"))
            .stateIn(sessionScope, SharingStarted.Eagerly, initialValue = false)

    override suspend fun updateListenersCount(count: Int) {
        connectedDevices.collectConnected { device ->
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

    /**
     * Нужно ли запрашивать [Audience.intensityUpdates].
     */
    override val isIntensityRequested: StateFlow<Boolean> =
        connectedDevices
            .runningCountConnected { it.isIntensityRequested }
            .onEach { log.info { "intensityRequestedCount: $it" } }
            .map { it > 0 }
            .flowOn(CoroutineName("session-count-intensity-requested"))
            .stateIn(sessionScope, SharingStarted.Eagerly, initialValue = false)

    override suspend fun updateIntensity(update: IntensityUpdate) {
        val jobs = connectedDevices.value
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

private class SessionDevice(
    val connection: DeviceConnection,
) : Composition, CoroutineScope by connection {
    private val _isIntensityRequested = MutableStateFlow(false)
    override val isIntensityRequested: StateFlow<Boolean> = _isIntensityRequested.asStateFlow()

    private val _isListenersCountRequested = MutableStateFlow(false)
    override val isListenersCountRequested: StateFlow<Boolean> = _isListenersCountRequested.asStateFlow()

    suspend fun receiveMessages() {
        connection.incoming.collect { event ->
            when (event) {
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

    override fun toString(): String {
        return "SessionDevice(id=${connection.id})"
    }
}

private val log = KotlinLogging.logger {}

