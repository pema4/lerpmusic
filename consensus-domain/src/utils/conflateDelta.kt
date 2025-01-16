package lerpmusic.consensus.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.runningFold

/**
 * Апдейты могут приходить быстрее, чем их обрабатывает [lerpmusic.consensus.Composition].
 */
fun Flow<Double>.conflateDelta(): Flow<Double> {
    return flow {
        var lastCollectedSum = 0.0

        runningFold(0.0, Double::plus)
            .conflate()
            .collect { sum ->
                val delta = sum - lastCollectedSum
                lastCollectedSum = sum
                emit(delta)
            }
    }
}
