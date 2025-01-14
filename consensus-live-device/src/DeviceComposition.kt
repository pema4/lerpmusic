package lerpmusic.consensus.device

import arrow.core.raise.nullable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import lerpmusic.consensus.Composition
import lerpmusic.consensus.NoteEvent

class DeviceComposition(
    private val max: Max,
) : Composition {
    override val events: Flow<NoteEvent> =
        max.inlet3("midiin")
            .mapNotNull { (a, b, c) ->
                nullable {
                    NoteEvent.Companion.fromRaw(
                        channel = (a as? Int).bind(),
                        pitch = (b as? Int).bind(),
                        velocity = (c as? Int).bind(),
                    )
                }
            }
            .onEach { max.post("Got midi event $it") }

    override suspend fun play(ev: NoteEvent) {
        when (ev) {
            is NoteEvent.NoteOn ->
                max.outlet("midiout", ev.note.channel, ev.note.pitch, ev.velocity)

            is NoteEvent.NoteOff ->
                max.outlet("midiout", ev.note.channel, ev.note.pitch, 0)
        }
    }
}