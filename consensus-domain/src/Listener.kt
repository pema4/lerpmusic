package lerpmusic.consensus

import kotlinx.serialization.Serializable

/**
 * Сообщения, получаемые от слушателя
 */
@Serializable
sealed class ListenerRequest {
    @Serializable
    data object Action : ListenerRequest()

    @Serializable
    data object IncreaseIntensity : ListenerRequest()

    @Serializable
    data object DecreaseIntensity : ListenerRequest()
}

/**
 * Сообщения, отправляемые слушателю
 */
@Serializable
sealed class ListenerResponse {
    @Serializable
    data object AskForAction : ListenerResponse()

    @Serializable
    data object Cancel : ListenerResponse()

    @Serializable
    data object ReceiveIntensityUpdates : ListenerResponse()

    @Serializable
    data object CancelIntensityUpdates : ListenerResponse()
}
