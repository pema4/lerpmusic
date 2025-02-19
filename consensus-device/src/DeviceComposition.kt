package lerpmusic.consensus.device

import arrow.core.raise.nullable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import lerpmusic.consensus.Composition
import lerpmusic.consensus.IntensityUpdate
import lerpmusic.consensus.NoteEvent

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

    override val events: Flow<NoteEvent> =
        max.inlet3("midiin")
            .mapNotNull {
                nullable {
                    NoteEvent.Companion.fromRaw(
                        channel = (it?.a as? Int).bind(),
                        pitch = (it.b as? Int).bind(),
                        velocity = (it.c as? Int).bind(),
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

    override suspend fun updateIntensity(intensity: Flow<IntensityUpdate>) {
        isIntensityRequested.collectLatest { requested ->
            intensity.takeIf { requested }?.collect { update ->
                max.outlet("intensity", update.decrease, update.increase)
            }
        }
    }
}