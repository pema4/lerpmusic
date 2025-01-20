package lerpmusic.website.util

import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lerpmusic.consensus.utils.WebSocketConnection

inline fun <reified T, reified R> WebSocketConnection(
    session: WebSocketServerSession,
    scope: CoroutineScope,
): WebSocketConnection<T, R> {
    return object : WebSocketConnection<T, R>, CoroutineScope by scope {
        override val incoming: Flow<T> = flow {
            while (true) {
                emit(session.receiveDeserialized<T>())
            }
        }

        override suspend fun send(response: R) {
            session.sendSerialized(response)
        }
    }
}