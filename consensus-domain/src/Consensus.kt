package lerpmusic.consensus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/**
 * Алгоритм, используемый в композиции.
 *
 * Доменная модель алгоритма:
 * - не упоминает сервер
 *     - или идёт с точки зрения алгоритма
 *     - или с точки зрения сервера
 * - так как взаимодействие двунаправленное — музыкант спрашивает аудиторию, аудитория отвечает музыканту,
 * не понятно, с чьей точки зрения моделировать домен
 */
class Consensus(
    val composition: Composition,
    val audience: Audience,
) {
    suspend fun run() = supervisorScope {
        composition.events.collect { event ->
            launch {
                when (event) {
                    is NoteEvent.NoteOn -> {
                        if (audience.shouldPlayNote(event.note)) {
                            composition.play(event)
                        }
                    }

                    is NoteEvent.NoteOff -> {
                        audience.cancelNote(event.note)
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
    suspend fun cancelNote(note: Note)
}
