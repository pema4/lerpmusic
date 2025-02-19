package lerpmusic.consensus

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Сообщения, получаемые от слушателя
 */
@Serializable
sealed class ListenerRequest {
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
    @SerialName("ReceiveIntensityUpdates")
    data object ReceiveIntensityUpdates : ListenerResponse()

    @Serializable
    @SerialName("CancelIntensityUpdates")
    data object CancelIntensityUpdates : ListenerResponse()
}
