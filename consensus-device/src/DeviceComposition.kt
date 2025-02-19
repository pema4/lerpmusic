package lerpmusic.consensus.device

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import lerpmusic.consensus.Composition
import lerpmusic.consensus.IntensityUpdate

class DeviceComposition(
    private val max: Max,
    private val isIntensityRequested: Flow<Boolean>,
    private val isListenersCountRequested: Flow<Boolean>,
) : Composition {
    override suspend fun updateListenersCount(listenersCount: Flow<Int>) {
        isListenersCountRequested.collectLatest { requested ->
            listenersCount.takeIf { requested }?.collect { count ->
                max.outlet("listeners", count)
            }
        }
    }

    override suspend fun updateIntensity(intensity: Flow<IntensityUpdate>) {
        isIntensityRequested.collectLatest { requested ->
            intensity.takeIf { requested }?.collect { update ->
                max.outlet("intensity", update.decrease, update.increase)
            }
        }
    }
}