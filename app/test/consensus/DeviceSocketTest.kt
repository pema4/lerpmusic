package lerpmusic.website.consensus

import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import lerpmusic.consensus.DeviceRequest
import lerpmusic.consensus.DeviceResponse
import lerpmusic.consensus.Note
import kotlin.test.Test

class DeviceSocketTest {

    @Test
    fun `can connect`() = testApplication {
        val client = createClient {
            install(WebSockets)
        }

        client.webSocket("/consensus/10/device/123") {}
    }

    @Test
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

    @Test
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