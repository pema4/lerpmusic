package lerpmusic.consensus

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val channel: Int,
    val pitch: Int,
)
