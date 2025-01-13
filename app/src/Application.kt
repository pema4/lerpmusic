package lerpmusic.website

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.partialcontent.*
import lerpmusic.website.config.configureLogging
import lerpmusic.website.config.configureRouting
import lerpmusic.website.config.configureWebSockets

@Suppress("unused")
fun Application.lerpMusicModule() {
    install(AutoHeadResponse)
    install(PartialContent)

    install(ContentNegotiation) {
        json()
    }

    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate(10, "abcdef0123456789")
    }

//    configureHttpsRedirect()
    configureLogging()
    configureWebSockets()
    configureRouting()
}