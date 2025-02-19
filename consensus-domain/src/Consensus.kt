package lerpmusic.consensus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Алгоритм интерактивной композиции.
 */
fun CoroutineScope.launchConsensus(
    composition: Composition,
    audience: Audience,
) {
    launch {
        // без синхронизации, Kotlin/JS однопоточный
        val playingNotes = mutableSetOf<Note>()

        composition.events.collect { event ->
            when (event) {
                is NoteEvent.NoteOn -> {
                    launch {
                        if (audience.shouldPlayNote(event.note)) {
                            playingNotes += event.note
                            composition.play(event)
                        }
                    }
                }

                is NoteEvent.NoteOff -> {
                    audience.cancelNote(event.note)

                    val wasPlaying = playingNotes.remove(event.note)
                    if (wasPlaying) {
                        launch { composition.play(event) }
                    }
                }
            }
        }

        // В идеале должно быть так
        // composition.events.collect { event ->
        //     launch {
        //         if (audience.shouldPlayNote(event.note)) {
        //             composition.play(event)
        //         }
        //     }
        // }
    }

    launch {
        val intensityUpdates = audience.intensityUpdates.conflateDeltas()
        composition.updateIntensity(intensityUpdates)
    }

    launch {
        val listenersCount = audience.listenersCount.conflate()
        composition.updateListenersCount(listenersCount)
    }
}

interface CompositionEvent {
    val note: Note
}

interface Composition {
    suspend fun updateListenersCount(listenersCount: Flow<Int>)

    /**
     * Входящие MIDI-события.
     * Чтобы воспроизвести событие, нужно вызвать на нём метод [play].
     */
    val events: Flow<NoteEvent>
        get() = emptyFlow()

    /**
     * Воспроизвести событие, полученное из [events].
     */
    suspend fun play(ev: NoteEvent) {}

    suspend fun updateIntensity(intensity: Flow<IntensityUpdate>)
}

interface Audience {
    val listenersCount: Flow<Int>

    /**
     * Спросить слушателей, нужно ли проиграть ноту.
     */
    suspend fun shouldPlayNote(note: Note): Boolean = false

    /**
     * Отменить запрос на проигрывание ноты, запущенный через [shouldPlayNote].
     */
    fun cancelNote(note: Note) {}

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