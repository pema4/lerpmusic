package lerpmusic.consensus.domain.note

@Serializable
data class Note(
    val channel: Int,
    val pitch: Int,
)
