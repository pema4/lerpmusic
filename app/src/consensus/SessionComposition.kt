package lerpmusic.website.consensus

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import lerpmusic.consensus.*
import lerpmusic.consensus.utils.collectConnected
import lerpmusic.consensus.utils.runningCountConnected
import mu.KotlinLogging

class SessionComposition(
    private val sessionScope: CoroutineScope,
) : Composition {
    private val activeDevices: MutableStateFlow<List<SessionDevice>> = MutableStateFlow(emptyList())
    override val isListenersCountRequested: Flow<Boolean> = flowOf(true)

    init {
        sessionScope.launch {
            activeDevices.collectConnected { it.receiveMessages() }
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
        activeDevices.collectConnected { device ->
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
        activeDevices
            .runningCountConnected { it.isIntensityRequested }
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
}

private val log = KotlinLogging.logger {}

