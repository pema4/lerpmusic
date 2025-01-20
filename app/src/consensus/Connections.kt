package lerpmusic.website.consensus

import io.ktor.server.websocket.WebSocketServerSession
import kotlinx.coroutines.CoroutineScope
import lerpmusic.consensus.DeviceRequest
import lerpmusic.consensus.DeviceResponse
import lerpmusic.consensus.ListenerRequest
import lerpmusic.consensus.ListenerResponse
import lerpmusic.consensus.utils.WebSocketConnection
import lerpmusic.website.util.WebSocketConnection

class ListenerConnection(
    val id: String,
    private val webSocketSession: WebSocketServerSession,
    private val coroutineScope: CoroutineScope,
) : WebSocketConnection<ListenerRequest, ListenerResponse> by WebSocketConnection(webSocketSession, coroutineScope)

class DeviceConnection(
    val id: String,
    private val webSocketSession: WebSocketServerSession,
    private val coroutineScope: CoroutineScope,
) : WebSocketConnection<DeviceRequest, DeviceResponse> by WebSocketConnection(webSocketSession, coroutineScope)
