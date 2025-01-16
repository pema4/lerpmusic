package lerpmusic.website.consensus

import io.ktor.server.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import lerpmusic.consensus.*
import lerpmusic.consensus.utils.runningSetDifference
import mu.KotlinLogging
import kotlin.uuid.Uuid

class SessionComposition(
    private val coroutineScope: CoroutineScope,
) : Composition {
    private val activeDevices: MutableStateFlow<List<SessionDevice>> = MutableStateFlow(emptyList())

    private val receiverCoroutine = coroutineScope.launch {
        activeDevices.collectEachAddedDevice { it.receiveMessages() }
    }

    fun addDevice(connection: DeviceConnection) {
        val newDevice = SessionDevice(
            connection = connection,
        )

        activeDevices.update { devices ->
            check(devices.none { it.connection.id == connection.id }) { "Connection $connection already exists" }
            devices + newDevice
        }

        // При отключении удаляемся из списка
        connection.coroutineContext.job.invokeOnCompletion {
            activeDevices.update { devices -> devices - newDevice }
        }
    }

    override val events: Flow<NoteEvent> = emptyFlow()

    override suspend fun play(ev: NoteEvent) {
    }

    private val intensityRequestedCount: Flow<Int> = channelFlow {
        activeDevices.collectEachAddedDevice { device ->
            // Для перехода false -> true возвращаем 1, для true -> false - 0
            // [false, true, false, true] -> [0, 1, -1, 1]
            // [true, false, true] -> [0, 1, -1, 1]
            // TODO: когда девайс уходит, для него надо отправить 0
            device.isIntensityRequested
                .onSubscription { emit(false) }
                .map { if (it) 1 else 0 }
                .runningReduce { prev, next -> next - prev }
                .collect { this@channelFlow.send(it) }
        }
    }

    /**
     * Нужно ли запрашивать [Audience.intensityUpdates].
     */
    override val isIntensityRequested: Flow<Boolean> =
        intensityRequestedCount
            .runningFold(0, Int::plus)
            .map { it > 0 }

    override suspend fun updateIntensity(delta: Double) {
        val jobs = activeDevices.value.map { device ->
            device.connection.launch { device.updateIntensity(delta) }
        }
        currentCoroutineContext().job.invokeOnCompletion { jobs.forEach { it.cancel() } }
        jobs.joinAll()
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

    suspend fun receiveMessages() {
        when (val event = connection.receive()) {
            DeviceRequest.Ping -> connection.send(DeviceResponse.Pong)
//            is DeviceResponse.Pong -> receivedPongs.send(event)
//            is DeviceResponse.PlayNote -> deferredResponses[event.note]?.complete(true)
//            is DeviceResponse.IntensityUpdate -> receivedIntensityUpdates.value?.emit(event.delta)
            is DeviceRequest.AskNote -> {}
            is DeviceRequest.CancelNote -> {}
            DeviceRequest.ReceiveIntensityUpdates -> _isIntensityRequested.value = true
            DeviceRequest.CancelIntensityUpdates -> _isIntensityRequested.value = false
        }
        connection.receive()
    }

    override val events: Flow<NoteEvent>
        get() = emptyFlow()

    override suspend fun play(ev: NoteEvent) {
    }

    override suspend fun updateIntensity(delta: Double) {
        if (isIntensityRequested.value) {
            connection.send(DeviceResponse.IntensityUpdate(delta))
        }
    }
}

class DeviceConnection(
    val id: Uuid,
    private val webSocketSession: WebSocketServerSession,
) : CoroutineScope by webSocketSession {
    suspend fun send(data: DeviceResponse) {
        return webSocketSession.sendSerialized(data).also { log.debug { "Sent $data" } }
    }

    suspend fun receive(): DeviceRequest {
        return webSocketSession.receiveDeserialized<DeviceRequest>().also { log.debug { "Received $it " } }
    }
}

private val log = KotlinLogging.logger {}

