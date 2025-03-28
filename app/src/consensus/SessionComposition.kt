package lerpmusic.website.consensus

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
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

    init {
        connectedDevices
            .runningCountConnected { it.isIntensityRequested }
            .onEach { log.info { "intensityRequestedCount: $it" } }
            .flowOn(CoroutineName("session-count-intensity-requested"))
            .launchIn(sessionScope)

        connectedDevices
            .runningCountConnected { it.isListenersCountRequested }
            .onEach { log.info { "listenersCountRequested: $it" } }
            .flowOn(CoroutineName("session-count-listeners-requested"))
            .launchIn(sessionScope)
    }

    suspend fun addDevice(connection: DeviceConnection) {
        val newDevice = SessionDevice(connection)
        deviceConnections.add(newDevice)
    }

    override suspend fun updateListenersCount(listenersCount: Flow<Int>) = coroutineScope {
        // Здесь используется stateIn — возвращаем последнее увиденное значение или 0
        val sharedListenersCount = listenersCount.stateIn(
            this,
            initialValue = 0,
            started = SharingStarted.WhileSubscribed(),
        )

        connectedDevices.collectConnected { device ->
            device.updateListenersCount(sharedListenersCount)
        }
    }

    override suspend fun updateIntensity(intensity: Flow<IntensityUpdate>) = coroutineScope {
        // Здесь используется shareIn без буфера — читаем только новые сообщения
        val sharedIntensity = intensity.shareIn(
            this,
            replay = 0,
            started = SharingStarted.WhileSubscribed(),
        )

        connectedDevices.collectConnected {
            it.updateIntensity(sharedIntensity)
        }
    }
}

private class SessionDevice(
    val connection: DeviceConnection,
) : Composition, CoroutineScope by connection {
    private val _isIntensityRequested = MutableStateFlow(false)
    val isIntensityRequested: StateFlow<Boolean> = _isIntensityRequested.asStateFlow()

    private val _isListenersCountRequested = MutableStateFlow(false)
    val isListenersCountRequested: StateFlow<Boolean> = _isListenersCountRequested.asStateFlow()

    suspend fun receiveMessages() {
        connection.incoming.collect { event ->
            when (event) {
                DeviceRequest.Ping -> connection.send(DeviceResponse.Pong)
                DeviceRequest.ReceiveIntensityUpdates -> _isIntensityRequested.value = true
                DeviceRequest.CancelIntensityUpdates -> _isIntensityRequested.value = false
                is DeviceRequest.ReceiveListenersCount -> _isListenersCountRequested.value = event.receive
            }
        }
    }

    override suspend fun updateListenersCount(listenersCount: Flow<Int>) {
        isListenersCountRequested.collectLatest { requested ->
            if (requested) {
                listenersCount.conflate().collect { count ->
                    connection.send(DeviceResponse.ListenersCount(count))
                }
            }
        }
    }

    override suspend fun updateIntensity(intensity: Flow<IntensityUpdate>) {
        isIntensityRequested.collectLatest { requested ->
            if (requested) {
                intensity.conflateDeltas().collect { update ->
                    connection.send(DeviceResponse.IntensityUpdate(update.decrease, update.increase))
                }
            }
        }
    }

    override fun toString(): String {
        return "SessionDevice(id=${connection.id})"
    }
}

private val log = KotlinLogging.logger {}
