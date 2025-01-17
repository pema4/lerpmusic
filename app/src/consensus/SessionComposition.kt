package lerpmusic.website.consensus

import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import lerpmusic.consensus.*
import lerpmusic.consensus.utils.runningSetDifference
import mu.KotlinLogging
import kotlin.math.sign
import kotlin.uuid.Uuid

class SessionComposition(
    private val coroutineScope: CoroutineScope,
) : Composition {
    private val activeDevices: MutableStateFlow<List<SessionDevice>> = MutableStateFlow(emptyList())

    private val receiverCoroutine = coroutineScope.launch {
        activeDevices.collectEachAddedDevice { it.receiveMessages() }
    }

    fun addDevice(connection: DeviceConnection) {
        coroutineScope.launch(CoroutineName("DeviceConnectionCompletionHandler")) {
            val newDevice = SessionDevice(
                connection = connection,
            )

            activeDevices.update { devices ->
                check(devices.none { it.connection.id == connection.id }) { "Connection $connection already exists" }
                devices + newDevice
            }

            // При отключении удаляемся из списка
            connection.coroutineContext.job.join()
            activeDevices.update { devices -> devices - newDevice }
        }
    }

    override val events: Flow<NoteEvent> = emptyFlow()

    override suspend fun play(ev: NoteEvent) {
    }

    private val intensityRequestedCount: Flow<Int> = channelFlow {
        activeDevices.collectEachAddedDevice { device ->
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
        .onEach { log.info { "intensityRequestedCount: $it" } }

    /**
     * Нужно ли запрашивать [Audience.intensityUpdates].
     */
    override val isIntensityRequested: Flow<Boolean> =
        intensityRequestedCount
            .map { it > 0 }
            .distinctUntilChanged()

    override suspend fun updateIntensity(update: IntensityUpdate) {
        val jobs = activeDevices.value.map { device ->
            device.connection.launch { device.updateIntensity(update) }
        }
        val handler = currentCoroutineContext().job.invokeOnCompletion { jobs.forEach { it.cancel() } }
        jobs.joinAll()
        handler.dispose()
    }

    private suspend fun StateFlow<List<SessionDevice>>.collectEachAddedDevice(
        block: suspend CoroutineScope.(SessionDevice) -> Unit,
    ) {
        // TODO: как лучше всего отменять дочерние корутины при отмене выполнения collectEachAddedDevice?
        //  В наивном варианте с invokeOnCompletion будет утекать память:
        //  val newJobs = devices.added.map { device ->
        //      device.connection.launch { block(device) }
        //  }
        //  currentCoroutineContext().job.invokeOnCompletion { cause ->
        //      newJobs.forEach { it.cancel() }
        //  }
        val launchedJobs = mutableMapOf<SessionDevice, Job>()

        runningSetDifference()
            .onEach { devices ->
                for (device in devices.added) {
                    launchedJobs[device] = device.connection.launch { block(device) }
                }

                // эти джобы отменятся сами
                for (device in devices.removed) {
                    launchedJobs.remove(device)
                }
            }
            .onCompletion { cause ->
                launchedJobs.values.forEach {
                    it.cancel("collectEachAddedDevice cancelled", cause)
                }
            }
            .collect()

        error("Unreachable, should run forever")
    }
}

class SessionDevice(
    val connection: DeviceConnection,
) : Composition {
    private val _isIntensityRequested = MutableStateFlow(false)
    override val isIntensityRequested: StateFlow<Boolean> = _isIntensityRequested.asStateFlow()

    suspend fun receiveMessages(): Nothing {
        while (true) {
            when (val event = connection.receive()) {
                DeviceRequest.Ping -> connection.send(DeviceResponse.Pong)
                is DeviceRequest.AskNote -> {}
                is DeviceRequest.CancelNote -> {}
                DeviceRequest.ReceiveIntensityUpdates -> _isIntensityRequested.value = true
                DeviceRequest.CancelIntensityUpdates -> _isIntensityRequested.value = false
            }
        }
    }

    override val events: Flow<NoteEvent>
        get() = emptyFlow()

    override suspend fun play(ev: NoteEvent) {
    }

    override suspend fun updateIntensity(update: IntensityUpdate) {
        if (isIntensityRequested.value) {
            connection.send(DeviceResponse.IntensityUpdate(update.decrease, update.increase))
        }
    }
}

class DeviceConnection(
    val id: Uuid,
    private val webSocketSession: WebSocketServerSession,
    private val coroutineScope: CoroutineScope,
) : CoroutineScope by coroutineScope {
    suspend fun send(data: DeviceResponse) {
        return webSocketSession.sendSerialized(data).also { log.debug { "Sent $data" } }
    }

    suspend fun receive(): DeviceRequest {
        return webSocketSession.receiveDeserialized<DeviceRequest>().also { log.debug { "Received $it " } }
    }
}

private val log = KotlinLogging.logger {}

