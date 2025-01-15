package lerpmusic.consensus

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Сообщения, получаемые от девайса
 */
@Serializable
sealed class DeviceRequest {
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
    @SerialName("AskIntensityUpdates")
    data object ReceiveIntensityUpdates : DeviceRequest()

    @Serializable
    @SerialName("AskIntensityUpdates")
    data object CancelIntensityUpdates : DeviceRequest()
}

/**
 * Сообщения, отправляемые девайсу
 */
@Serializable
sealed class DeviceResponse {
    @Serializable
    @SerialName("PlayNote")
    data class PlayNote(
        val note: Note
    ) : DeviceResponse()

    data class IntensityUpdate(val delta: Int) : DeviceResponse()
}
