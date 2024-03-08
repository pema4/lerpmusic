package lerpmusic.website.consensus

import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC

suspend fun <T> withCallIdInMDC(callId: String?, block: suspend () -> T) {
    MDC.put("call-id", callId)
    withContext(MDCContext()) {
        block()
    }
}