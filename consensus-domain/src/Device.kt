package lerpmusic.consensus

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Сообщения, получаемые от девайса
 */
@Serializable
sealed class DeviceRequest {
    @Serializable
    @SerialName("Ping")
    data object Ping : DeviceRequest()

    @Serializable
    @SerialName("AskNote")
    data class AskNote(
        val note: Note,
    ) : DeviceRequest()

    @Serializable
    @SerialName("CancelNote")
    data class CancelNote(
        val note: Note,
    ) : DeviceRequest()

    @Serializable
    @SerialName("ReceiveIntensityUpdates")
    data object ReceiveIntensityUpdates : DeviceRequest()

    @Serializable
    @SerialName("CancelIntensityUpdates")
    data object CancelIntensityUpdates : DeviceRequest()
}

/**
 * Сообщения, отправляемые девайсу
 */
@Serializable
sealed class DeviceResponse {
    @Serializable
    @SerialName("Pong")
    data object Pong : DeviceResponse()

    @Serializable
    @SerialName("PlayNote")
    data class PlayNote(
        val note: Note
    ) : DeviceResponse()

    @Serializable
    @SerialName("IntensityUpdate")
    data class IntensityUpdate(val delta: Double) : DeviceResponse()
}
