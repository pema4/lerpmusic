package lerpmusic.consensus.device

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import lerpmusic.consensus.Audience
import lerpmusic.consensus.DeviceRequest
import lerpmusic.consensus.DeviceResponse
import lerpmusic.consensus.IntensityUpdate
import lerpmusic.consensus.utils.producePings
import lerpmusic.consensus.utils.receiveMessagesWithSubscription

class DeviceAudience(
    private val serverConnection: ServerConnection,
) : Audience {
    private val receivedPongs = serverConnection.producePings { DeviceRequest.Ping }

    init {
        serverConnection.launch { receiveMessages() }
    }

    private val receivedListenersCount = serverConnection.receiveMessagesWithSubscription<Int>(
        onStart = { serverConnection.send(DeviceRequest.ReceiveListenersCount(true)) },
        onCancellation = { serverConnection.send(DeviceRequest.ReceiveListenersCount(false)) },
    )
    override val listenersCount: Flow<Int> = receivedListenersCount.incoming

    private val receivedIntensityUpdates = serverConnection.receiveMessagesWithSubscription<IntensityUpdate>(
        onStart = { serverConnection.send(DeviceRequest.ReceiveIntensityUpdates) },
        onCancellation = { serverConnection.send(DeviceRequest.CancelIntensityUpdates) },
    )
    override val intensityUpdates: Flow<IntensityUpdate> = receivedIntensityUpdates.incoming

    private suspend fun CoroutineScope.receiveMessages() {
        serverConnection.incoming.collect { event ->
            when (event) {
                is DeviceResponse.Pong -> receivedPongs.send(event)
                is DeviceResponse.ListenersCount -> receivedListenersCount.receive(event.count)
                is DeviceResponse.IntensityUpdate -> receivedIntensityUpdates.receive(
                    IntensityUpdate(
                        event.decrease,
                        event.increase
                    )
                )
            }
        }
    }
}
