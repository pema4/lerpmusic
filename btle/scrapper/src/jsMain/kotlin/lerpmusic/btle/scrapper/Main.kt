package lerpmusic.btle.scrapper

import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

suspend fun main() {
    val noble = Noble()
    withTimeout(20.seconds) {
        noble
            .discover()
            .collect { println(it) }
    }
}

@Serializable
class Peripheral(
    val id: String,
//    val address: String,
//    val addressType: String,
//    val connectable: Boolean,
//    val advertisement: Advertisement,
//    val rssi: Int,
//    val mtu: String,
//    val state: PeripheralState,
)

@Serializable
enum class PeripheralState {
    @JsName("error")
    ERROR,

    @JsName("connecting")
    CONNECTING,

    @JsName("connected")
    CONNECTED,

    @JsName("disconnecting")
    DISCONNECTING,

    @JsName("disconnected")
    DISCONNECTED,
}

@Serializable
class Advertisement(
    val txPowerLevel: Int,
    val manufacturerData: ManufacturerData,
)

@Serializable
class ManufacturerData(
    val type: ManudacturerDataType,
    val data: List<Int>
)

enum class ManudacturerDataType {

}

val x = """
    {
      "id": "54952d32a1cc5334e1464f10061242f5",
      "address": "",
      "addressType": "unknown",
      "connectable": true,
      "advertisement": {
        "txPowerLevel": 8,
        "manufacturerData": {
          "type": "Buffer",
          "data": [
            76,
            0,
            16,
            6,
            51,
            29,
            201,
            92,
            114,
            120
          ]
        }
      },
      "rssi": -66,
      "mtu": null,
      "state": "disconnected"
    }

""".trimIndent()
