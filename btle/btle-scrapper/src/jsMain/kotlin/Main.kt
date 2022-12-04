package lerpmusic.btle.scrapper

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlinx.coroutines.MainScope
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    val serverHost = args.getOrElse(0) { "lerpmusic.ru" }

    val scrapper = Scrapper(
        noble = Noble(),
        httpClient = HttpClient(Js) {
            install(WebSockets) {
                maxFrameSize = Long.MAX_VALUE
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
        },
        serverHost = serverHost,
    )

    scrapper.launchIn(MainScope())
}
