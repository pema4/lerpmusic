package lerpmusic.website.consensus.device

import io.ktor.server.websocket.*
import lerpmusic.consensus.SessionId
import lerpmusic.consensus.SessionPin
import lerpmusic.website.consensus.NoteQueue
import lerpmusic.website.consensus.session.SessionRepository
import java.util.concurrent.ConcurrentHashMap

class DeviceRepository(
    private val sessionRepository: SessionRepository,
    private val noteQueue: NoteQueue,
) {
    private val activeDevices = ConcurrentHashMap<SessionId, Set<Device>>()

    suspend fun getAndUseDevice(
        sessionId: SessionId,
        sessionPin: SessionPin,
        deviceConnection: WebSocketServerSession,
        block: suspend (Device?) -> Unit,
    ) {
        if (!sessionRepository.exists(sessionId, sessionPin)) {
            return block(null)
        }

        val device = Device(sessionId, deviceConnection)
        try {
            activeDevices.compute(sessionId) { _, devices ->
                devices.orEmpty() + device
            }
            block(device)
        } finally {
            activeDevices.compute(sessionId) { _, devices ->
                (devices.orEmpty() - device)
                    .ifEmpty { null }
            }
        }
    }

    fun getAll(sessionId: SessionId): Collection<Device> {
        return activeDevices[sessionId].orEmpty()
    }
}
