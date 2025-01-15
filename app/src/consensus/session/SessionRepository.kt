package lerpmusic.website.consensus.session

import lerpmusic.consensus.Session
import lerpmusic.consensus.SessionId
import lerpmusic.consensus.SessionPin

class SessionRepository(
    private val sessionPin: SessionPin,
) {
    private val availableSessions = setOf(
        Session(SessionId("10"), sessionPin),
        Session(SessionId("11"), sessionPin),
        Session(SessionId("12"), sessionPin),
        Session(SessionId("13"), sessionPin),
        Session(SessionId("14"), sessionPin),
    )

    fun exists(id: SessionId): Boolean {
        return availableSessions.any { it.id == id }
    }

    fun exists(id: SessionId, pin: SessionPin): Boolean {
        return Session(id, pin) in availableSessions
    }
}