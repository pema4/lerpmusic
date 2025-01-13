package lerpmusic.website.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC

suspend fun <T> withCallIdInMDC(callId: String?, block: suspend CoroutineScope.() -> T) {
    MDC.put("call-id", callId)
    withContext(MDCContext(), block)
}