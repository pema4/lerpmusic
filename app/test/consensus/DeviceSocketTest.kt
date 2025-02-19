package lerpmusic.website.consensus

import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import lerpmusic.website.testLerpMusicApplication
import org.junit.jupiter.api.Test

class DeviceSocketTest {

    @Test
    fun `can connect`() = testLerpMusicApplication {
        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/consensus/10/device/123") {}
    }
}