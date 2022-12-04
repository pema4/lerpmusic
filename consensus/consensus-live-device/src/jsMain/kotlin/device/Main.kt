package lerpmusic.consensus.device

import io.ktor.client.engine.js.Js
import kotlinx.coroutines.MainScope

fun main() {
    val script = ConsensusScript(
        max = Max,
        httpClientEngineFactory = Js,
    )
    script.launchIn(MainScope())
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
