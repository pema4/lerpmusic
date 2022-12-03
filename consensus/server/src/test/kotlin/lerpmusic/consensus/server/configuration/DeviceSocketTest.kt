package lerpmusic.consensus.server.configuration;

import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import lerpmusic.consensus.domain.device.DeviceRequest
import lerpmusic.consensus.domain.device.DeviceResponse
import lerpmusic.consensus.domain.note.Note

class DeviceSocketTest {

    //    @Test
    fun `can connect`() = testApplication {
        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/consensus/10/device/123") {}
    }

    //    @Test
    fun `can request note`() = testApplication {
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

    //    @Test
    fun `can cancel note`() = testApplication {
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
