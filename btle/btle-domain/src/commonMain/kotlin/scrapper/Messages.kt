package lerpmusic.btle.domain.scrapper

/**
 * Сообщения, получаемые от скрэппера
 */
@Serializable
sealed class ScrapperRequest {
    @Serializable
    data class Announcement(
        val id: String,
        val rssi: Int
    ) : ScrapperRequest()
}
