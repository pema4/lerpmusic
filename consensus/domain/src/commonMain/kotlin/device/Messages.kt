package lerpmusic.consensus.domain.device

import kotlinx.serialization.Serializable
import lerpmusic.consensus.domain.note.Note

@Serializable
sealed class DeviceRequest {
    @Serializable
    data class AskNote(
        val note: Note,
    ) : DeviceRequest()

    @Serializable
    data class CancelNote(
        val note: Note,
    ) : DeviceRequest()
}

@Serializable
sealed class DeviceResponse {
    @Serializable
    data class PlayNote(
        val note: Note
    ) : DeviceResponse()
}
