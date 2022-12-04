package lerpmusic.btle.domain.receiver

import kotlinx.serialization.Serializable

/**
 * Сообщения, отправляемые ресиверу
 */
@Serializable
sealed class ReceiverResponse {
    @Serializable
    data class FoundPeripheral(
        val bucket: Int,
        val rssi: Int,
    ) : ReceiverResponse()

    @Serializable
    data class LostPeripheral(
        val bucket: Int,
    ) : ReceiverResponse()
}