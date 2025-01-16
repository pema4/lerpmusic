package lerpmusic.consensus

import kotlinx.serialization.Serializable

/**
 * Сообщения, получаемые от девайса
 */
@Serializable
sealed class DeviceRequest {
    @Serializable
    data object Ping : DeviceRequest()

    @Serializable
    data class AskNote(
        val note: Note,
    ) : DeviceRequest()

    @Serializable
    data class CancelNote(
        val note: Note,
    ) : DeviceRequest()

    @Serializable
    data object ReceiveIntensityUpdates : DeviceRequest()

    @Serializable
    data object CancelIntensityUpdates : DeviceRequest()
}

/**
 * Сообщения, отправляемые девайсу
 */
@Serializable
sealed class DeviceResponse {
    @Serializable
    data object Pong : DeviceResponse()

    @Serializable
    data class PlayNote(
        val note: Note
    ) : DeviceResponse()

    @Serializable
    data class IntensityUpdate(val delta: Double) : DeviceResponse()
}
