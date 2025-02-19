package lerpmusic.consensus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch

/**
 * Алгоритм интерактивной композиции.
 */
fun CoroutineScope.launchConsensus(
    composition: Composition,
    audience: Audience,
) {
    launch {
        val intensityUpdates = audience.intensityUpdates.conflateDeltas()
        composition.updateIntensity(intensityUpdates)
    }

    launch {
        val listenersCount = audience.listenersCount.conflate()
        composition.updateListenersCount(listenersCount)
    }
}

interface Composition {
    suspend fun updateListenersCount(listenersCount: Flow<Int>)

    suspend fun updateIntensity(intensity: Flow<IntensityUpdate>)
}

interface Audience {
    val listenersCount: Flow<Int>

    /**
     * Изменения интенсивности
     */
    val intensityUpdates: Flow<IntensityUpdate>
}

data class IntensityUpdate(val decrease: Double, val increase: Double) {
    operator fun plus(other: IntensityUpdate) =
        IntensityUpdate(decrease + other.decrease, increase + other.increase)

    operator fun minus(other: IntensityUpdate) =
        IntensityUpdate(decrease - other.decrease, increase - other.increase)
}

/**
 * Апдейты могут приходить быстрее, чем их обработает коллектор
 */
fun Flow<IntensityUpdate>.conflateDeltas(): Flow<IntensityUpdate> {
    return flow {
        var lastCollectedSum = IntensityUpdate(0.0, 0.0)

        runningFold(lastCollectedSum, IntensityUpdate::plus)
            .conflate()
            .collect { sum ->
                val delta = sum - lastCollectedSum
                lastCollectedSum = sum
                emit(delta)
            }
    }
}