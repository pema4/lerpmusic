package lerpmusic.consensus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import lerpmusic.consensus.utils.conflateDelta

/**
 * Алгоритм интерактивной композиции.
 */
class Consensus(
    val composition: Composition,
    val audience: Audience,
) {
    suspend fun filterCompositionEvents() = supervisorScope {
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
//        composition.events.collect { event ->
//            launch {
//                if (audience.shouldPlayNote(event.note)) {
//                    composition.play(event)
//                }
//            }
//        }
    }

    suspend fun receiveIntensityUpdates() {
        composition.isIntensityRequested.collectLatest { requested ->
            if (requested) {
                audience.intensityUpdates
                    .conflateDelta()
                    .collect { composition.updateIntensity(it) }
            }
        }
    }
}

interface CompositionEvent {
    val note: Note
}

interface Composition {
    /**
     * Входящие MIDI-события.
     * Чтобы воспроизвести событие, нужно вызвать на нём метод [play].
     */
    val events: Flow<NoteEvent>

    /**
     * Воспроизвести событие, полученное из [events].
     */
    suspend fun play(ev: NoteEvent)

    /**
     * Нужно ли запрашивать [Audience.intensityUpdates].
     */
    val isIntensityRequested: Flow<Boolean>

    suspend fun updateIntensity(delta: Double)
}

interface Audience {
    /**
     * Спросить слушателей, нужно ли проиграть ноту.
     */
    suspend fun shouldPlayNote(note: Note): Boolean

    /**
     * Отменить запрос на проигрывание ноты, запущенный через [shouldPlayNote].
     */
    fun cancelNote(note: Note)

    /**
     * Изменения интенсивности
     */
    val intensityUpdates: Flow<Double>
}
