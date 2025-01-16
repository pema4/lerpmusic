package lerpmusic.consensus

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Сообщения, получаемые от слушателя
 */
@Serializable
sealed class ListenerRequest {
    @Serializable
    @SerialName("Action")
    data object Action : ListenerRequest()

    @Serializable
    @SerialName("IncreaseIntensity")
    data object IncreaseIntensity : ListenerRequest()

    @Serializable
    @SerialName("DecreaseIntensity")
    data object DecreaseIntensity : ListenerRequest()
}

/**
 * Сообщения, отправляемые слушателю
 */
@Serializable
sealed class ListenerResponse {
    @Serializable
    @SerialName("AskForAction")
    data object AskForAction : ListenerResponse()

    @Serializable
    @SerialName("Cancel")
    data object Cancel : ListenerResponse()

    @Serializable
    @SerialName("ReceiveIntensityUpdates")
    data object ReceiveIntensityUpdates : ListenerResponse()

    @Serializable
    @SerialName("CancelIntensityUpdates")
    data object CancelIntensityUpdates : ListenerResponse()
}
