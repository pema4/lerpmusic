package lerpmusic.consensus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Алгоритм по фильтрации нот, используемый в композиции.
 */
class ConsensusFilter(
    val composition: Composition,
    val audience: Audience,
) {
    suspend fun run() = supervisorScope {
        val mutex = Mutex()
        val playingNotes = mutableSetOf<Note>()

        composition.events.collect { event ->
            when (event) {
                is NoteEvent.NoteOn -> {
                    launch {
                        if (audience.shouldPlayNote(event.note)) {
                            mutex.withLock { playingNotes += event.note }
                            composition.play(event)
                        }
                    }
                }

                is NoteEvent.NoteOff -> {
                    audience.cancelNote(event.note)

                    val wasPlaying = mutex.withLock { playingNotes.remove(event.note) }
                    if (wasPlaying) {
                        launch { composition.play(event) }
                    }
                }
            }
        }
    }
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
}
