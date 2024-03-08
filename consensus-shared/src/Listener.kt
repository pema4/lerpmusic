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
}
