package lerpmusic.website

import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import lerpmusic.website.config.configureHttpsRedirect
import lerpmusic.website.config.configureLogging
import lerpmusic.website.config.configureRouting
import lerpmusic.website.config.configureWebSockets

@Suppress("unused")
fun Application.websiteModule() {
    install(AutoHeadResponse)

    install(ContentNegotiation) {
        json()
    }

    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate(10, "abcdef0123456789")
    }

    configureHttpsRedirect()
    configureLogging()
    configureWebSockets()
    configureRouting()
}

fun main(args: Array<String>) =
    io.ktor.server.netty.EngineMain.main(args)
