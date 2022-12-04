package lerpmusic.btle.server

import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import lerpmusic.btle.domain.note.Note

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
//            sendSerialized<ReceiverResponse>(ReceiverResponse.AskNote(note))
//            receiveDeserialized<ReceiverResponse>() shouldBe ReceiverResponse.PlayNote(note)
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
//            sendSerialized<ReceiverResponse>(ReceiverResponse.CancelNote(note))
        }
    }
}
