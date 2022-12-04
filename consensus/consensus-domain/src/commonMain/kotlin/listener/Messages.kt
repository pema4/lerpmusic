package lerpmusic.consensus.domain.listener

import kotlinx.serialization.SerialName

/**
 * Сообщения, получаемые от слушателя
 */
@Serializable
sealed class ListenerRequest {
    private val name: String by lazy {
        this::class.simpleName!!
    }

    override fun toString() = name

    @Serializable
    @SerialName("Action")
    object Action : ListenerRequest()
}

/**
 * Сообщения, отправляемые слушателю
 */
@Serializable
sealed class ListenerResponse {
    private val name: String by lazy {
        this::class.simpleName!!
    }

    override fun toString() = name

    @Serializable
    @SerialName("AskForAction")
    object AskForAction : ListenerResponse()

    @Serializable
    @SerialName("Cancel")
    object Cancel : ListenerResponse()
}
