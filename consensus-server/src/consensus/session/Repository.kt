package lerpmusic.consensus.domain.session

class SessionRepository {
    private val availableSessions = setOf(
        Session(SessionId("10"), SessionPin("123")),
        Session(SessionId("11"), SessionPin("123")),
        Session(SessionId("12"), SessionPin("123")),
        Session(SessionId("13"), SessionPin("123")),
        Session(SessionId("14"), SessionPin("123")),
    )

    fun exists(id: SessionId): Boolean {
        return availableSessions.any { it.id == id }
    }

    fun exists(id: SessionId, pin: SessionPin): Boolean {
        return Session(id, pin) in availableSessions
    }
}
