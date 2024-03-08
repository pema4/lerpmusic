package lerpmusic.consensus.device

import io.ktor.client.engine.js.Js
import kotlinx.coroutines.coroutineScope

suspend fun main() {
    coroutineScope {
        val script = ConsensusScript(
            max = Max,
            httpClientEngineFactory = Js,
            coroutineScope = this
        )
        script.start()
    }
//    MainScope().launch {
//        client.get("http://localhost:8081/test")
//    }
}

//private val client = HttpClient(Js) {
//    install(WebSockets) {
//        maxFrameSize = Long.MAX_VALUE
//        contentConverter = KotlinxWebsocketSerializationConverter(Json)
//    }
//}
