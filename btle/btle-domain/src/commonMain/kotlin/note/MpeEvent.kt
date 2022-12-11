package lerpmusic.btle.domain.note

sealed interface MpeEvent {
    val note: Note

    data class NoteOn(
        override val note: Note,
        val velocity: Int,
    ) : MpeEvent

    data class NoteOff(
        override val note: Note,
    ) : MpeEvent

    data class Pressure(
        override val note: Note,
        val pressure: Int,
    ) : MpeEvent

    companion object {
        fun fromRaw(channel: Int, pitch: Int, velocity: Int): MpeEvent? {
            if (pitch !in 0..127) {
                return null
            }

            if (channel !in 0..16) {
                return null
            }

            return when (velocity) {
                0 -> NoteOff(Note(channel, pitch))
                in 1..127 -> NoteOn(Note(channel, pitch), velocity)
                else -> null
            }
        }
    }
}
