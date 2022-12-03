package lerpmusic.consensus.domain.listener

import io.ktor.server.plugins.callid.callId
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import lerpmusic.consensus.domain.session.ConsensusService
import lerpmusic.consensus.domain.session.SessionId
import mu.KotlinLogging

class Listener(
    val sessionId: SessionId,
    private val wsSession: WebSocketServerSession,
) {
    data class Id(
        val sessionId: SessionId,
        val callId: String?,
    )

    val id = Id(sessionId, wsSession.call.callId)

    suspend fun receiveRequest(): ListenerRequest =
        wsSession.receiveDeserialized()

    suspend fun cancelAction() =
        respond(ListenerResponse.Cancel)

    suspend fun askForAction() =
        respond(ListenerResponse.AskForAction)

    private suspend fun respond(response: ListenerResponse) {
        wsSession.sendSerialized(response)
//        log.debug { "Sent ListenerResponse $response to $this" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Listener

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Listener($sessionId, ${id.callId})"
    }
}

suspend fun Listener.processRequests(
    consensusService: ConsensusService,
) {
    coroutineScope {
        if (consensusService.sessionHasEnqueuedNotes(sessionId)) {
            askForAction()
        }

        while (true) {
            val request = receiveRequest()
//            log.debug { "Received ListenerRequest $request from ${this@processRequests}" }

            when (request) {
                is ListenerRequest.Action -> {
                    launch { consensusService.playOldestNote(sessionId) }
                }
            }
        }
    }
}

private val log = KotlinLogging.logger {}
