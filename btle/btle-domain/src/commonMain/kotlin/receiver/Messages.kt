package lerpmusic.btle.domain.receiver

import kotlinx.serialization.Serializable

/**
 * Сообщения, отправляемые ресиверу
 */
@Serializable
sealed class ReceiverResponse {
    abstract val bucket: Int

    @Serializable
    data class FoundPeripheral(
        override val bucket: Int,
        val rssi: Int,
    ) : ReceiverResponse()

    @Serializable
    data class LostPeripheral(
        override val bucket: Int,
    ) : ReceiverResponse()
}
