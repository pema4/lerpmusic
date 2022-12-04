package lerpmusic.btle.domain.scrapper

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Peripheral(
    val id: String,
    val address: String,
    val addressType: String?,
    val connectable: Boolean,
//    val advertisement: Advertisement?,
    val rssi: Int,
    val mtu: String?,
    val state: PeripheralState,
)

@Serializable
enum class PeripheralState {
    @SerialName("error")
    ERROR,

    @SerialName("connecting")
    CONNECTING,

    @SerialName("connected")
    CONNECTED,

    @SerialName("disconnecting")
    DISCONNECTING,

    @SerialName("disconnected")
    DISCONNECTED,
}

@Serializable
data class Advertisement(
    val localName: String?,
    val txPowerLevel: Int?,
    val manufacturerData: ManufacturerData?,
    val serviceData: ServiceData?,
)

@Serializable
data class ManufacturerData(
    val type: ManufacturerDataType,
//    val data: List<Int>
)

@Serializable
enum class ManufacturerDataType {
    @SerialName("Buffer")
    BUFFER
}

@Serializable
data class ServiceData(
    val uuid: String?
)
