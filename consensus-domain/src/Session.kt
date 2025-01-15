package lerpmusic.consensus

import kotlin.jvm.JvmInline

@JvmInline
value class SessionId(val value: String)

@JvmInline
value class SessionPin(val value: String) {
    init {
        check(value matches pinRegex)
    }

    companion object {
        val pinRegex = "[a-zA-Z0-9]+".toRegex()
    }
}


data class Session(
    val id: SessionId,
    val pin: SessionPin,
)
