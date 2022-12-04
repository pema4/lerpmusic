package lerpmusic.btle.domain.note

@Serializable
data class Note(
    val channel: Int,
    val pitch: Int,
)
