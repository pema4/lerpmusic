package lerpmusic.consensus

import kotlinx.serialization.Serializable

/**
 * Сообщения, получаемые от девайса
 */
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

/**
 * Сообщения, отправляемые девайсу
 */
@Serializable
sealed class DeviceResponse {
    @Serializable
    data class PlayNote(
        val note: Note
    ) : DeviceResponse()
}