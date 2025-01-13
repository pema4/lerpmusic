package lerpmusic.website.consensus

import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.serialization.json.Json
import lerpmusic.consensus.DeviceRequest
import lerpmusic.consensus.DeviceResponse
import lerpmusic.consensus.Note
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

    @Test
    fun `can request note`() = testLerpMusicApplication {
        val client = createClient {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
        }

        client.webSocket("/consensus/abc/device/124") {
            val note = Note(1, 123)
            sendSerialized<DeviceRequest>(DeviceRequest.AskNote(note))
            receiveDeserialized<DeviceResponse>() shouldBe DeviceResponse.PlayNote(note)
        }
    }

    @Test
    fun `can cancel note`() = testLerpMusicApplication {
        val client = createClient {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
        }

        client.webSocket("/consensus/abc/device/124") {
            val note = Note(1, 123)
            sendSerialized<DeviceRequest>(DeviceRequest.CancelNote(note))
        }
    }
}